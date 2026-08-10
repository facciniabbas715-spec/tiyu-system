<#
.SYNOPSIS
本地后台启动前端 dev server（Vite，端口 5173）

.DESCRIPTION
隐藏窗口启动，日志写入 scripts/web-smoke.log（已被 .gitignore 忽略）。
停止前端：找到 5173 端口占用进程后 Stop-Process。

.EXAMPLE
.\scripts\start-frontend.ps1
#>
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$webDir = Join-Path $root 'sportseq-web'
$logFile = Join-Path $PSScriptRoot 'web-smoke.log'

$psi = [System.Diagnostics.ProcessStartInfo]::new()
$psi.FileName = 'cmd.exe'
$psi.Arguments = "/c npm run dev > `"$logFile`" 2>&1"
$psi.WorkingDirectory = $webDir
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true

$process = [System.Diagnostics.Process]::Start($psi)
Write-Host "前端已后台启动（wrapper pid=$($process.Id)），日志：$logFile"
