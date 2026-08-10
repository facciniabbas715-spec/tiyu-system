<#
.SYNOPSIS
本地后台启动后端（Spring Boot，端口 8080）

.DESCRIPTION
隐藏窗口启动，日志写入 scripts/boot-smoke.log（已被 .gitignore 忽略）。
停止后端：找到 8080 端口占用进程后 Stop-Process。

.EXAMPLE
.\scripts\start-backend.ps1
#>
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$adminDir = Join-Path $root 'sportseq-admin'
$logFile = Join-Path $PSScriptRoot 'boot-smoke.log'

$psi = [System.Diagnostics.ProcessStartInfo]::new()
$psi.FileName = 'cmd.exe'
$psi.Arguments = "/c mvnw.cmd spring-boot:run > `"$logFile`" 2>&1"
$psi.WorkingDirectory = $adminDir
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$psi.Environment['JAVA_HOME'] = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot'

$process = [System.Diagnostics.Process]::Start($psi)
Write-Host "后端已后台启动（wrapper pid=$($process.Id)），日志：$logFile"
