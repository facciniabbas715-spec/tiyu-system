<#
.SYNOPSIS
高级代码审查配套端到端测试（评审用，非正式回归脚本）

.DESCRIPTION
覆盖：基础资料 -> 盘点调整 -> 入库全流程 -> 借用全流程 -> 归还全流程 -> 报废全流程，
并尝试复现两个疑似缺陷（重复归还重复入账、报废处置扣减锁定库存）。
测试数据使用 REVW 前缀，结束时自动清理。

要求：后端已启动（8080）、Redis 运行。
#>
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$RedisCli = 'D:\Redis\redis-cli.exe'
)

$ErrorActionPreference = 'Stop'
$script:failures = @()
$script:observations = @()
$script:authHeaders = @{}
$script:today = (Get-Date).ToString('yyyy-MM-dd')
$script:returnDate = (Get-Date).AddDays(7).ToString('yyyy-MM-dd')

function Check {
    param([string]$Name, [scriptblock]$Body)
    try {
        & $Body
        Write-Host "[PASS] $Name" -ForegroundColor Green
    } catch {
        $script:failures += "$Name -> $($_.Exception.Message)"
        Write-Host "[FAIL] $Name -> $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Note {
    param([string]$Text)
    $script:observations += $Text
    Write-Host "[观察] $Text" -ForegroundColor Yellow
}

function Get-Api {
    param([string]$Path)
    Invoke-RestMethod -Uri "$BaseUrl$Path" -Headers $script:authHeaders -TimeoutSec 15
}

function Send-Json {
    param([string]$Method, [string]$Path, [object]$Body)
    $json = if ($null -eq $Body) { '' } else { $Body | ConvertTo-Json -Depth 10 }
    Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -Headers $script:authHeaders `
        -ContentType 'application/json; charset=utf-8' -Body $json -TimeoutSec 15
}

function Assert-Code {
    param($Resp, [string]$Context)
    if ($Resp.code -ne 0) {
        throw "$Context 返回错误: code=$($Resp.code) msg=$($Resp.message)"
    }
}

function Login {
    $captcha = Invoke-RestMethod -Uri "$BaseUrl/api/auth/captcha" -TimeoutSec 10
    $code = (& $RedisCli GET "sportseq:captcha:$($captcha.data.uuid)").Trim()
    if ([string]::IsNullOrEmpty($code)) { throw '未读取到验证码' }
    $body = @{ username = 'admin'; password = 'admin123'; code = $code; uuid = $captcha.data.uuid } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" `
        -ContentType 'application/json; charset=utf-8' -Body $body -TimeoutSec 10
    if ($login.code -ne 0) { throw '登录失败' }
    $script:authHeaders = @{ Authorization = "Bearer $($login.data.token)" }
}

function Find-BorrowByPurpose {
    param([string]$Purpose)
    $page = Get-Api '/api/borrow/page?current=1&size=200'
    $row = @($page.data.records) | Where-Object { $_.purpose -eq $Purpose } | Select-Object -First 1
    if ($null -eq $row) { throw "未找到借用单 purpose=$Purpose" }
    return $row.id
}

function Find-StockInBySupplier {
    param([string]$Supplier)
    $page = Get-Api '/api/stock/in/page?current=1&size=200'
    $row = @($page.data.records) | Where-Object { $_.supplier -eq $Supplier } | Select-Object -First 1
    if ($null -eq $row) { throw "未找到入库单 supplier=$Supplier" }
    return $row.id
}

function Find-ScrapByRemark {
    param([string]$Remark)
    $page = Get-Api '/api/scrap/page?current=1&size=200'
    $row = @($page.data.records) | Where-Object { $_.remark -eq $Remark } | Select-Object -First 1
    if ($null -eq $row) { throw "未找到报废单 remark=$Remark" }
    return $row.id
}

function Get-Stock {
    param([int]$EquipmentId, [int]$WarehouseId)
    $page = Get-Api '/api/stock/page?current=1&size=200&equipmentName=REVW'
    $row = @($page.data.records) | Where-Object { $_.equipmentId -eq $EquipmentId -and $_.warehouseId -eq $WarehouseId } | Select-Object -First 1
    if ($null -eq $row) { throw '未找到库存行' }
    return @{ quantity = [int]$row.quantity; locked = [int]$row.lockedQuantity }
}

function Cleanup {
    param([int]$EquipmentId, [int]$WarehouseId)
    $jdbc = 'mysql.exe'
    # 清理使用唯一编码/名称的测试数据（无外键，按依赖顺序删）
    $env:MYSQL_PWD = 'Sportseq@123'
    try {
        & 'D:\mysql\MySQL Server 8.0\bin\mysql.exe' -h localhost -P 3306 -u sportseq sportseq -e "
            DELETE ri FROM return_item ri JOIN return_order ro ON ro.id=ri.return_id
              JOIN borrow_order bo ON bo.id=ro.borrow_order_id WHERE bo.purpose LIKE 'REVW-%';
            DELETE ro FROM return_order ro JOIN borrow_order bo ON bo.id=ro.borrow_order_id WHERE bo.purpose LIKE 'REVW-%';
            DELETE bi FROM borrow_item bi JOIN borrow_order bo ON bo.id=bi.borrow_id WHERE bo.purpose LIKE 'REVW-%';
            DELETE FROM borrow_order WHERE purpose LIKE 'REVW-%';
            DELETE si FROM scrap_item si JOIN scrap_order so ON so.id=si.scrap_id WHERE so.remark LIKE 'REVW-%';
            DELETE FROM scrap_order WHERE remark LIKE 'REVW-%';
            DELETE si FROM stock_in_item si JOIN stock_in_order so ON so.id=si.order_id WHERE so.supplier LIKE 'REVW-%';
            DELETE FROM stock_in_order WHERE supplier LIKE 'REVW-%';
            DELETE FROM stock_record WHERE ref_order_no LIKE 'REVW-%';
            DELETE FROM equipment_stock WHERE equipment_id=$EquipmentId AND warehouse_id=$WarehouseId;
            DELETE FROM equipment WHERE equipment_name LIKE 'REVW-%';
            DELETE FROM equipment_category WHERE category_code='REVW';
            DELETE FROM warehouse WHERE warehouse_code='REVW-WH';
        "
        if ($LASTEXITCODE -ne 0) { Write-Host '清理 SQL 执行异常' -ForegroundColor Red }
    } finally {
        $env:MYSQL_PWD = $null
    }
}

Write-Host '== 评审端到端测试开始 ==' -ForegroundColor Cyan
Login

$equipmentId = 0
$warehouseId = 0
try {
    # 1. 基础资料
    Check '创建分类/器材/仓库' {
        $cat = Send-Json POST '/api/equipment/category' @{
            parentId = 0; categoryName = 'REVW-评审分类'; categoryCode = 'REVW'; status = 1
        }
        Assert-Code $cat '创建分类'
        $eq = Send-Json POST '/api/equipment' @{
            equipmentName = 'REVW-评审测试-篮球'; categoryId = (Get-Api '/api/equipment/category/tree').data[0].id
            unit = '个'; purchasePrice = 100; safeStock = 2; status = 1
        }
        Assert-Code $eq '创建器材'
        $wh = Send-Json POST '/api/equipment/warehouse' @{
            warehouseCode = 'REVW-WH'; warehouseName = 'REVW-评审测试仓库'; status = 1
        }
        Assert-Code $wh '创建仓库'
    }

    $tree = Get-Api '/api/equipment/category/tree'
    $catId = [int]($tree.data | Where-Object { $_.categoryCode -eq 'REVW' } | Select-Object -First 1).id
    $eqPage = Get-Api '/api/equipment/page?current=1&size=200&equipmentName=REVW'
    $equipmentId = [int]($eqPage.data.records | Select-Object -First 1).id
    $whPage = Get-Api '/api/equipment/warehouse/list'
    $warehouseId = [int]($whPage.data | Where-Object { $_.warehouseCode -eq 'REVW-WH' } | Select-Object -First 1).id

    # 2. 盘点调整 +10
    Check '库存调整 +10' {
        $r = Send-Json PUT '/api/stock/adjust' @{ equipmentId = $equipmentId; warehouseId = $warehouseId; changeQuantity = 10 }
        Assert-Code $r '库存调整'
        $stock = Get-Stock $equipmentId $warehouseId
        if ($stock.quantity -ne 10) { throw "调整后数量应为10，实际 $($stock.quantity)" }
    }

    # 3. 入库全流程（草稿->提交->审核->验收）
    Check '入库单全流程' {
        $order = Send-Json POST '/api/stock/in' @{
            warehouseId = $warehouseId; inType = 1; supplier = 'REVW-VENDOR'
            items = @(@{ equipmentId = $equipmentId; quantity = 5; unitPrice = 100 })
        }
        Assert-Code $order '创建入库单'
        $orderId = Find-StockInBySupplier 'REVW-VENDOR'
        $r = Send-Json PUT "/api/stock/in/$orderId/submit" $null
        Assert-Code $r '提交入库单'
        $r = Send-Json PUT '/api/stock/in/audit' @{ orderId = $orderId; pass = $true }
        Assert-Code $r '审核入库单'
        $r = Send-Json PUT "/api/stock/in/$orderId/receive" $null
        Assert-Code $r '验收入库单'
        $stock = Get-Stock $equipmentId $warehouseId
        if ($stock.quantity -ne 15) { throw "验收后数量应为15，实际 $($stock.quantity)" }
    }

    # 4. 借用全流程（申请->审核->领用）
    Check '借用单全流程（锁定/审核/发放）' {
        $borrow = Send-Json POST '/api/borrow' @{
            borrowType = 1; purpose = 'REVW-MAIN'; expectedReturnDate = $script:returnDate
            items = @(@{ equipmentId = $equipmentId; warehouseId = $warehouseId; quantity = 4 })
        }
        Assert-Code $borrow '创建借用单'
        $borrowId = Find-BorrowByPurpose 'REVW-MAIN'
        $stock = Get-Stock $equipmentId $warehouseId
        if ($stock.locked -ne 4) { throw "申请后锁定应为4，实际 $($stock.locked)" }
        $r = Send-Json PUT '/api/borrow/audit' @{ orderId = $borrowId; pass = $true }
        Assert-Code $r '审核借用单'
        $r = Send-Json PUT "/api/borrow/$borrowId/issue" $null
        Assert-Code $r '领用发放'
        $stock = Get-Stock $equipmentId $warehouseId
        if ($stock.quantity -ne 11 -or $stock.locked -ne 0) {
            throw "发放后应为数量11/锁定0，实际 $($stock.quantity)/$($stock.locked)"
        }
    }

    # 5. 归还全流程（部分归还->确认->全部归还->确认）
    Check '归还单全流程（部分/全部）' {
        $borrowId = Find-BorrowByPurpose 'REVW-MAIN'
        $detail = Get-Api "/api/borrow/$borrowId"
        $borrowItemId = [int]$detail.data.items[0].id
        $ret1 = Send-Json POST '/api/return' @{
            borrowOrderId = $borrowId
            items = @(@{ borrowItemId = $borrowItemId; quantity = 2; conditionStatus = 1 })
        }
        Assert-Code $ret1 '创建归还单1'
        $retPage = Get-Api '/api/return/page?current=1&size=200'
        $ret1Id = [int]($retPage.data.records | Where-Object { $_.borrowOrderId -eq $borrowId } | Select-Object -First 1).id
        $r = Send-Json PUT "/api/return/$ret1Id/confirm" $null
        Assert-Code $r '确认归还1'
        $stock = Get-Stock $equipmentId $warehouseId
        if ($stock.quantity -ne 13) { throw "部分归还后数量应为13，实际 $($stock.quantity)" }

        $ret2 = Send-Json POST '/api/return' @{
            borrowOrderId = $borrowId
            items = @(@{ borrowItemId = $borrowItemId; quantity = 2; conditionStatus = 1 })
        }
        Assert-Code $ret2 '创建归还单2'
        $retPage = Get-Api '/api/return/page?current=1&size=200'
        $ret2Id = [int]($retPage.data.records | Where-Object { $_.borrowOrderId -eq $borrowId -and $_.id -ne $ret1Id } | Select-Object -First 1).id
        $r = Send-Json PUT "/api/return/$ret2Id/confirm" $null
        Assert-Code $r '确认归还2'
        $stock = Get-Stock $equipmentId $warehouseId
        if ($stock.quantity -ne 15) { throw "全部归还后数量应为15，实际 $($stock.quantity)" }
        $detail = Get-Api "/api/borrow/$borrowId"
        if ($detail.data.status -ne 4) { throw "借用单应为已归还(4)，实际 $($detail.data.status)" }
    }

    # 6. 报废全流程（申请->审核->处置）
    Check '报废单全流程' {
        $scrap = Send-Json POST '/api/scrap' @{
            warehouseId = $warehouseId; scrapType = 2; remark = 'REVW-SCRAP-MAIN'
            items = @(@{ equipmentId = $equipmentId; quantity = 1; scrapReason = 'REVW-评审测试报废' })
        }
        Assert-Code $scrap '创建报废单'
        $scrapId = Find-ScrapByRemark 'REVW-SCRAP-MAIN'
        $r = Send-Json PUT '/api/scrap/audit' @{ orderId = $scrapId; pass = $true }
        Assert-Code $r '审核报废单'
        $r = Send-Json PUT '/api/scrap/dispose' @{ orderId = $scrapId; disposeMethod = 1 }
        Assert-Code $r '报废处置'
        $stock = Get-Stock $equipmentId $warehouseId
        if ($stock.quantity -ne 14) { throw "报废后数量应为14，实际 $($stock.quantity)" }
    }

    # 7. 仪表盘与统计接口连通性
    Check '仪表盘/统计接口' {
        $summary = Get-Api '/api/dashboard/summary'
        Assert-Code $summary '仪表盘'
        $trend = Get-Api '/api/statistics/borrow-trend'
        if (@($trend.data).Count -ne 12) { throw '借用趋势月份数不为12' }
    }

    # ---- 回归验证 A：同一借用明细重复归还必须被拦截 ----
    Write-Host ''
    Write-Host '== 回归验证 A：重复归还同一借用明细 ==' -ForegroundColor Cyan
    Check '重复归还应被拦截且库存只入账一次' {
        Send-Json POST '/api/borrow' @{
            borrowType = 1; purpose = 'REVW-REPRO-A'; expectedReturnDate = $script:returnDate
            items = @(@{ equipmentId = $equipmentId; warehouseId = $warehouseId; quantity = 2 })
        } | Out-Null
        $borrowAId = Find-BorrowByPurpose 'REVW-REPRO-A'
        Send-Json PUT '/api/borrow/audit' @{ orderId = $borrowAId; pass = $true } | Out-Null
        Send-Json PUT "/api/borrow/$borrowAId/issue" $null | Out-Null
        $detailA = Get-Api "/api/borrow/$borrowAId"
        $itemA = [int]$detailA.data.items[0].id
        $stockBefore = Get-Stock $equipmentId $warehouseId
        Send-Json POST '/api/return' @{ borrowOrderId = $borrowAId; items = @(@{ borrowItemId = $itemA; quantity = 2; conditionStatus = 1 }) } | Out-Null
        Send-Json POST '/api/return' @{ borrowOrderId = $borrowAId; items = @(@{ borrowItemId = $itemA; quantity = 2; conditionStatus = 1 }) } | Out-Null
        $retPageA = Get-Api '/api/return/page?current=1&size=200'
        $retIdsA = @($retPageA.data.records | Where-Object { $_.borrowOrderId -eq $borrowAId } | Sort-Object id | Select-Object -First 2)
        if ($retIdsA.Count -lt 2) { throw '应存在两张待确认归还单' }
        $r1 = Send-Json PUT "/api/return/$($retIdsA[0].id)/confirm" $null
        if ($r1.code -ne 0) { throw "第一张确认失败: $($r1.message)" }
        $r2 = Send-Json PUT "/api/return/$($retIdsA[1].id)/confirm" $null
        if ($r2.code -ne 1001) { throw "第二张确认应被拦截(1001)，实际 code=$($r2.code) msg=$($r2.message)" }
        $stockAfter = Get-Stock $equipmentId $warehouseId
        if ($stockAfter.quantity -ne ($stockBefore.quantity + 2)) {
            throw "库存应只 +2，实际 $($stockBefore.quantity) -> $($stockAfter.quantity)"
        }
        $detailA2 = Get-Api "/api/borrow/$borrowAId"
        $returned = [int]$detailA2.data.items[0].returnedQuantity
        if ($returned -ne 2) { throw "returnedQuantity 应为2，实际 $returned" }
        Note "重复归还已拦截：库存 $($stockBefore.quantity) -> $($stockAfter.quantity)（+2），returnedQuantity=$returned"
    }

    # ---- 回归验证 B：报废处置不得扣减锁定库存 ----
    Write-Host '== 回归验证 B：报废处置与锁定库存 ==' -ForegroundColor Cyan
    Check '报废处置应拒绝超过可用库存，且不破坏借用发放' {
        Send-Json POST '/api/borrow' @{
            borrowType = 1; purpose = 'REVW-REPRO-B'; expectedReturnDate = $script:returnDate
            items = @(@{ equipmentId = $equipmentId; warehouseId = $warehouseId; quantity = 3 })
        } | Out-Null
        $borrowBId = Find-BorrowByPurpose 'REVW-REPRO-B'
        $stockLocked = Get-Stock $equipmentId $warehouseId
        if ($stockLocked.locked -ne 3) { throw "锁定应为3，实际 $($stockLocked.locked)" }
        Send-Json POST '/api/scrap' @{
            warehouseId = $warehouseId; scrapType = 2; remark = 'REVW-SCRAP-B'
            items = @(@{ equipmentId = $equipmentId; quantity = 14; scrapReason = 'REVW-回归B' })
        } | Out-Null
        $scrapBId = Find-ScrapByRemark 'REVW-SCRAP-B'
        Send-Json PUT '/api/scrap/audit' @{ orderId = $scrapBId; pass = $true } | Out-Null
        $dispose = Send-Json PUT '/api/scrap/dispose' @{ orderId = $scrapBId; disposeMethod = 1 }
        if ($dispose.code -ne 3001) { throw "报废处置应被拦截(3001)，实际 code=$($dispose.code) msg=$($dispose.message)" }
        $stockAfter = Get-Stock $equipmentId $warehouseId
        if ($stockAfter.quantity -ne $stockLocked.quantity -or $stockAfter.locked -ne 3) {
            throw "处置失败后库存应不变，实际 quantity=$($stockAfter.quantity) locked=$($stockAfter.locked)"
        }
        Send-Json PUT '/api/borrow/audit' @{ orderId = $borrowBId; pass = $true } | Out-Null
        $issue = Send-Json PUT "/api/borrow/$borrowBId/issue" $null
        if ($issue.code -ne 0) { throw "借用发放失败: $($issue.message)" }
        $stockIssued = Get-Stock $equipmentId $warehouseId
        if ($stockIssued.quantity -ne ($stockLocked.quantity - 3) -or $stockIssued.locked -ne 0) {
            throw "发放后库存异常，实际 quantity=$($stockIssued.quantity) locked=$($stockIssued.locked)"
        }
        Note '报废处置已按可用库存拦截，锁定库存未被扣减，借用正常发放'
    }
} finally {
    Write-Host ''
    Cleanup $equipmentId $warehouseId
}

Write-Host ''
if ($script:failures.Count -eq 0) {
    Write-Host '正常流程与回归验证全部通过 ✔' -ForegroundColor Green
} else {
    Write-Host "失败 $($script:failures.Count) 项：" -ForegroundColor Red
    $script:failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
}
