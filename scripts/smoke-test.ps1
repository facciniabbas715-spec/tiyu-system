<#
.SYNOPSIS
体育器材管理系统 本地联调冒烟测试

.DESCRIPTION
依次验证：后端健康检查、验证码登录、仪表盘汇总、统计报表接口、业务列表接口、未认证 401 拦截。
要求：MySQL（sportseq 库）、Redis（6379）已运行，后端已启动在 8080。

.EXAMPLE
.\scripts\smoke-test.ps1
#>
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$RedisCli = 'D:\Redis\redis-cli.exe',
    [string]$Username = 'admin',
    [string]$Password = 'admin123'
)

$ErrorActionPreference = 'Stop'
$script:failures = @()
$script:token = $null

function Test-Port {
    param([string]$HostName = 'localhost', [int]$Port)
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $task = $client.ConnectAsync($HostName, $Port)
        if (-not $task.Wait(1000)) { return $false }
        return $client.Connected
    } catch {
        return $false
    } finally {
        if ($client) { $client.Dispose() }
    }
}

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

function Get-Api {
    param([string]$Path)
    Invoke-RestMethod -Uri "$BaseUrl$Path" `
        -Headers @{ Authorization = "Bearer $($script:token)" } `
        -TimeoutSec 15
}

Write-Host "== 体育器材管理系统 本地冒烟测试 ==" -ForegroundColor Cyan

if (-not (Test-Port -Port 6379)) {
    Write-Host "[FAIL] Redis 未运行（需先启动：D:\Redis\redis-server.exe D:\Redis\redis.windows.conf）" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path -LiteralPath $RedisCli)) {
    Write-Host "[FAIL] 未找到 redis-cli：$RedisCli（冒烟脚本依赖它读取验证码）" -ForegroundColor Red
    exit 1
}

Check 'GET /api/health' {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/health" -TimeoutSec 10
    if ($r.code -ne 0 -or $r.data -ne 'OK') {
        throw "健康检查异常: $($r | ConvertTo-Json -Compress)"
    }
}

Check '验证码 + POST /api/auth/login' {
    $captcha = Invoke-RestMethod -Uri "$BaseUrl/api/auth/captcha" -TimeoutSec 10
    if ($captcha.code -ne 0 -or [string]::IsNullOrEmpty($captcha.data.uuid)) {
        throw '获取验证码失败'
    }
    $code = (& $RedisCli GET "sportseq:captcha:$($captcha.data.uuid)").Trim()
    if ([string]::IsNullOrEmpty($code)) {
        throw '未从 Redis 读取到验证码'
    }
    $body = @{
        username = $Username
        password = $Password
        code     = $code
        uuid     = $captcha.data.uuid
    } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" `
        -ContentType 'application/json; charset=utf-8' `
        -Body $body -TimeoutSec 10
    if ($login.code -ne 0 -or [string]::IsNullOrEmpty($login.data.token)) {
        throw '登录失败，请确认 admin/admin123 账号与验证码链路正常'
    }
    $script:token = $login.data.token
}

Check 'GET /api/dashboard/summary' {
    $r = Get-Api '/api/dashboard/summary'
    if ($r.code -ne 0) { throw 'summary 返回错误码' }
    foreach ($field in 'todayBorrowCount', 'todayReturnCount', 'todayStockInCount',
        'warningStockCount', 'pendingScrapCount', 'equipmentTotal', 'stockTotalValue') {
        if ($null -eq $r.data.$field) { throw "缺少字段 $field" }
    }
}

Check 'GET /api/statistics/borrow-trend（近12个月）' {
    $r = Get-Api '/api/statistics/borrow-trend'
    if ($r.code -ne 0 -or @($r.data).Count -ne 12) {
        throw "借用趋势异常，月份数=$(@($r.data).Count)"
    }
}

Check 'GET /api/statistics/equipment-usage' {
    $r = Get-Api '/api/statistics/equipment-usage'
    if ($r.code -ne 0 -or $null -eq $r.data) { throw '使用率排行返回异常' }
}

Check 'GET /api/statistics/dept-borrow' {
    $r = Get-Api '/api/statistics/dept-borrow'
    if ($r.code -ne 0 -or $null -eq $r.data) { throw '部门统计返回异常' }
}

Check 'GET /api/statistics/overdue' {
    $r = Get-Api '/api/statistics/overdue'
    if ($r.code -ne 0 -or $null -eq $r.data.penaltyTotal) { throw '逾期统计返回异常' }
}

Check 'GET /api/statistics/export（Excel 导出）' {
    $tmp = Join-Path $env:TEMP "sportseq-smoke-export-$([guid]::NewGuid().ToString('N')).xlsx"
    Invoke-RestMethod -Uri "$BaseUrl/api/statistics/export?type=usage" `
        -Headers @{ Authorization = "Bearer $($script:token)" } -OutFile $tmp -TimeoutSec 20
    $file = Get-Item -LiteralPath $tmp
    if ($file.Length -lt 1000) { throw '导出文件过小' }
    $head = [System.IO.File]::ReadAllBytes($tmp)[0..1]
    if (-not ($head[0] -eq 80 -and $head[1] -eq 75)) { throw '导出文件不是有效 xlsx（缺少 PK 头）' }
    try {
        [System.IO.File]::Delete($tmp)
    } catch {
        Write-Host "    提示：临时文件清理失败：$tmp" -ForegroundColor Yellow
    }
}

Check 'GET /api/equipment/page' {
    $r = Get-Api '/api/equipment/page?current=1&size=5'
    if ($r.code -ne 0 -or $null -eq $r.data.records) { throw '器材分页返回异常' }
}

Check 'GET /api/stock/page' {
    $r = Get-Api '/api/stock/page?current=1&size=5'
    if ($r.code -ne 0 -or $null -eq $r.data.records) { throw '库存分页返回异常' }
}

Check 'GET /api/knowledge/documents/page（RAG 知识库）' {
    $r = Get-Api '/api/knowledge/documents/page?current=1&size=5'
    if ($r.code -ne 0 -or $null -eq $r.data.records) { throw '知识库分页返回异常' }
}

Check '未认证访问受保护接口应返回 401' {
    try {
        Invoke-WebRequest -Uri "$BaseUrl/api/dashboard/summary" -TimeoutSec 10 | Out-Null
        throw '接口未拦截未认证请求'
    } catch {
        $status = [int]$_.Exception.Response.StatusCode
        if ($status -ne 401) { throw "期望 401，实际 $status" }
    }
}

Write-Host ''
if ($script:failures.Count -eq 0) {
    Write-Host '冒烟测试全部通过 ✔' -ForegroundColor Green
    exit 0
} else {
    Write-Host "冒烟测试失败 $($script:failures.Count) 项：" -ForegroundColor Red
    $script:failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}
