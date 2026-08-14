<#
.SYNOPSIS
RAG 知识库端到端真实验证

.DESCRIPTION
前置：后端已启动（8080）、Redis（6379）、MySQL 已运行，且已配置：
  AI_API_KEY（聊天，如 DeepSeek）、AI_EMBEDDING_API_KEY / AI_EMBEDDING_BASE_URL / AI_EMBEDDING_MODEL（如百炼 text-embedding-v3）。
验证：导入内置知识库 → 提问“篮球应该怎么保养？”应命中知识库并给出接地回答；
再提问知识库外的问题应返回固定文案“知识库中暂无相关信息。”，证明未让模型胡编。

.EXAMPLE
.\scripts\rag-verify.ps1
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
        [int]$TimeoutSec = 60
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

Write-Host '== RAG 知识库端到端验证 ==' -ForegroundColor Cyan

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

Write-Host '1/3 导入内置知识库（解析→切分→Embedding→写向量）...' -ForegroundColor Cyan
$seed = Invoke-Api -Path '/api/knowledge/documents/seed' -Method 'Post' -Token $token -TimeoutSec 300
if ($seed.code -ne 0) { throw "导入知识库失败：$($seed.message)" }
Write-Host "    内置知识库：新增 $($seed.data.imported) 篇，跳过 $($seed.data.skipped) 篇" -ForegroundColor Green

Write-Host '2/3 提问“篮球应该怎么保养？”（应命中知识库）...' -ForegroundColor Cyan
$hit = Invoke-Api -Path '/api/ai/chat' -Method 'Post' -Token $token -TimeoutSec 120 -Body (@{
    message = '篮球应该怎么保养？'
} | ConvertTo-Json)
if ($hit.code -ne 0) { throw "客服调用失败：$($hit.message)" }
if (-not $hit.data.debug.knowledgeUsed -or @($hit.data.debug.hits).Count -eq 0) {
    throw '知识库未命中：请确认内置知识库已导入且 Embedding 配置正确'
}
Write-Host '    [命中] 检索到的知识片段：' -ForegroundColor Green
foreach ($item in $hit.data.debug.hits) {
    Write-Host ("      - 《{0}》片段#{1}  相似度={2:N4}" -f $item.title, $item.chunkIndex, $item.similarity)
    Write-Host ("        {0}" -f ($item.content -replace '\s+', ' '))
}
Write-Host "    [回答] $($hit.data.debug.answer)" -ForegroundColor Green
if ([string]::IsNullOrWhiteSpace($hit.data.content)) { throw '回答为空' }

Write-Host '3/3 提问知识库外问题“月球车怎么保养？”（应固定回复，禁止胡编）...' -ForegroundColor Cyan
$miss = Invoke-Api -Path '/api/ai/chat' -Method 'Post' -Token $token -TimeoutSec 120 -Body (@{
    message = '月球车应该怎么保养？'
} | ConvertTo-Json)
if ($miss.code -ne 0) { throw "客服调用失败：$($miss.message)" }
if ($miss.data.debug.knowledgeUsed) { throw '知识库外的问题不应命中知识库' }
if ($miss.data.content -ne '知识库中暂无相关信息。') {
    throw "未命中时应返回固定文案，实际：$($miss.data.content)"
}
Write-Host '    [防幻觉] 知识库无相关资料时正确返回固定文案，且未调用大模型' -ForegroundColor Green

Write-Host ''
Write-Host 'RAG 验证全部通过 ✔（检索命中、接地回答、无资料不胡编）' -ForegroundColor Green
