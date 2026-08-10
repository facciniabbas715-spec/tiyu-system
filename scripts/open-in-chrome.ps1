<#
.SYNOPSIS
用本机 Chrome 打开指定 URL

.EXAMPLE
.\scripts\open-in-chrome.ps1 -Url http://localhost:5173
#>
param(
    [string]$Url = 'http://localhost:5173',
    [string]$Chrome = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $Chrome)) {
    Write-Host "[FAIL] 未找到 Chrome：$Chrome" -ForegroundColor Red
    exit 1
}

$psi = [System.Diagnostics.ProcessStartInfo]::new()
$psi.FileName = $Chrome
$psi.Arguments = $Url
$psi.UseShellExecute = $false
$process = [System.Diagnostics.Process]::Start($psi)
Write-Host "Chrome 已打开：$Url"
