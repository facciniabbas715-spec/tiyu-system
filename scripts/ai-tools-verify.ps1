<#
.SYNOPSIS
AI 客服工具调用端到端真实验证（知识 + 业务数据）

.DESCRIPTION
前置：后端已启动（8080，需为包含工具调用的版本）、Redis（6379）、MySQL 已运行，
且已配置 AI_API_KEY 等模型环境变量（聊天模型需支持 function calling）。
验证三类问题：
  1) 知识题“篮球应该怎么保养？”→ 意图 KNOWLEDGE，走 searchKnowledge（RAG 命中）；
  2) 库存题“现在篮球还有多少个？”→ 意图 BUSINESS，走 getEquipmentStock 返回真实库存结构；
  3) 借阅题“我借的篮球什么时候应该归还？”→ 走 getUserBorrowRecords，查询当前用户真实记录。
脚本只校验工具确实被调用并返回真实业务数据（debug.toolCalls），不校验具体数字。

.EXAMPLE
.\scripts\ai-tools-verify.ps1
#>
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$RedisCli = 'D:\Redis\redis-cli.exe',
    [string]$Username = 'admin',
    [string]$Password = 'admin123'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Net.Http

function Convert-JsonUtf8 {
    param([byte[]]$Bytes)
    return ([System.Text.Encoding]::UTF8.GetString($Bytes) | ConvertFrom-Json)
}

function Invoke-Api {
    param(
        [string]$Path,
        [string]$Method = 'Get',
        [string]$Body,
        [string]$Token,
        [int]$TimeoutSec = 120
    )
    $http = [System.Net.Http.HttpClient]::new()
    $http.Timeout = [TimeSpan]::FromSeconds($TimeoutSec)
    if ($Token) {
        $http.DefaultRequestHeaders.Authorization =
            [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)
    }
    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method), "$BaseUrl$Path")
    if ($Body) {
        $request.Content = [System.Net.Http.StringContent]::new(
            $Body, [System.Text.Encoding]::UTF8, 'application/json')
    }
    $response = $http.SendAsync($request).Result
    $bytes = $response.Content.ReadAsByteArrayAsync().Result
    $http.Dispose()
    return (Convert-JsonUtf8 $bytes)
}

function Assert-ToolCall {
    param($Debug, [string]$ToolName, [string]$Intent, [switch]$AllowNoData)
    if ($Debug.intent -notin @($Intent, 'MIXED')) {
        throw "意图不符：期望 $Intent，实际 $($Debug.intent)"
    }
    $calls = @($Debug.toolCalls)
    $hit = $calls | Where-Object { $_.name -eq $ToolName } | Select-Object -First 1
    if (-not $hit) {
        $called = ($calls | ForEach-Object { $_.name }) -join ', '
        throw "未调用工具 $ToolName，实际调用：$called"
    }
    if ($hit.result -match '"ok":true') {
        return
    }
    if ($AllowNoData -and $hit.result -match '未查询到') {
        Write-Host '    [说明] 当前库中无该器材，工具如实返回「未查询到」，未编造数据' -ForegroundColor Yellow
        return
    }
    throw "工具 $ToolName 未返回真实数据：$($hit.result)"
}

Write-Host '== AI 客服工具调用端到端验证（RAG + 业务数据） ==' -ForegroundColor Cyan

$captcha = Invoke-Api -Path '/api/auth/captcha'
if ($captcha.code -ne 0) { throw '获取验证码失败' }
$code = (& $RedisCli GET "sportseq:captcha:$($captcha.data.uuid)").Trim()
if ([string]::IsNullOrEmpty($code)) { throw '未从 Redis 读取到验证码' }
$login = Invoke-Api -Path '/api/auth/login' -Method 'Post' -Body (@{
    username = $Username
    password = $Password
    code     = $code
    uuid     = $captcha.data.uuid
} | ConvertTo-Json)
if ($login.code -ne 0) { throw '登录失败' }
$token = $login.data.token

Write-Host '0/3 导入内置知识库（幂等，已导入则跳过）...' -ForegroundColor Cyan
$seed = Invoke-Api -Path '/api/knowledge/documents/seed' -Method 'Post' -Token $token -TimeoutSec 300
if ($seed.code -ne 0) { throw "导入知识库失败：$($seed.message)" }

Write-Host '1/3 知识题“篮球应该怎么保养？”（应走 RAG 知识工具）...' -ForegroundColor Cyan
$knowledge = Invoke-Api -Path '/api/ai/chat' -Method 'Post' -Token $token -Body (@{
    message = '篮球应该怎么保养？'
} | ConvertTo-Json)
if ($knowledge.code -ne 0) { throw "客服调用失败：$($knowledge.message)" }
if (-not $knowledge.data.debug.knowledgeUsed -or @($knowledge.data.debug.hits).Count -eq 0) {
    throw '知识题未命中知识库'
}
Assert-ToolCall -Debug $knowledge.data.debug -ToolName 'searchKnowledge' -Intent 'KNOWLEDGE'
Write-Host "    [知识] 命中《$($knowledge.data.debug.hits[0].title)》片段" -ForegroundColor Green
Write-Host "    [回答] $($knowledge.data.content)" -ForegroundColor Green

Write-Host '2/3 库存题“现在篮球还有多少个？”（应调用业务工具查真实库存）...' -ForegroundColor Cyan
$stock = Invoke-Api -Path '/api/ai/chat' -Method 'Post' -Token $token -Body (@{
    message = '现在篮球还有多少个？'
} | ConvertTo-Json)
if ($stock.code -ne 0) { throw "客服调用失败：$($stock.message)" }
Assert-ToolCall -Debug $stock.data.debug -ToolName 'getEquipmentStock' -Intent 'BUSINESS' -AllowNoData
if ([string]::IsNullOrWhiteSpace($stock.data.content)) { throw '库存题回答为空' }
Write-Host '    [业务] 已调用 getEquipmentStock，返回真实库存数据' -ForegroundColor Green
Write-Host "    [回答] $($stock.data.content)" -ForegroundColor Green

Write-Host '3/3 借阅题“我借的篮球什么时候应该归还？”（应查询当前用户真实借阅）...' -ForegroundColor Cyan
$borrow = Invoke-Api -Path '/api/ai/chat' -Method 'Post' -Token $token -Body (@{
    message = '我借的篮球什么时候应该归还？'
} | ConvertTo-Json)
if ($borrow.code -ne 0) { throw "客服调用失败：$($borrow.message)" }
Assert-ToolCall -Debug $borrow.data.debug -ToolName 'getUserBorrowRecords' -Intent 'BUSINESS'
if ([string]::IsNullOrWhiteSpace($borrow.data.content)) { throw '借阅题回答为空' }
Write-Host '    [业务] 已调用 getUserBorrowRecords，用户身份由服务端绑定' -ForegroundColor Green
Write-Host "    [回答] $($borrow.data.content)" -ForegroundColor Green

Write-Host ''
Write-Host 'AI 客服工具调用验证全部通过 ✔（知识走 RAG、库存/借阅查真实业务数据）' -ForegroundColor Green
