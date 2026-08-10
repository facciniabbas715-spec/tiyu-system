<#
.SYNOPSIS
本地 MySQL 数据库备份（sportseq）

.DESCRIPTION
使用 mysqldump 单事务备份，输出到仓库根目录 backups/，
文件名 sportseq-YYYYMMDD-HHmmss.sql。默认账号仅需库级权限。

.EXAMPLE
.\scripts\backup-db.ps1
#>
param(
    [string]$MysqlDump = 'D:\mysql\MySQL Server 8.0\bin\mysqldump.exe',
    [string]$HostName = 'localhost',
    [string]$Port = '3306',
    [string]$User = 'sportseq',
    [string]$Password = 'Sportseq@123',
    [string]$Database = 'sportseq'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $MysqlDump)) {
    Write-Host "[FAIL] 未找到 mysqldump：$MysqlDump（可传 -MysqlDump 指定路径）" -ForegroundColor Red
    exit 1
}

$backupDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\backups'))
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
$file = Join-Path $backupDir "$Database-$(Get-Date -Format 'yyyyMMdd-HHmmss').sql"

& $MysqlDump -h $HostName -P $Port -u $User "-p$Password" `
    --single-transaction --no-tablespaces --routines --triggers "--result-file=$file" $Database

if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $file)) {
    Write-Host "[FAIL] 备份失败（mysqldump 退出码 $LASTEXITCODE）" -ForegroundColor Red
    exit 1
}

$size = (Get-Item -LiteralPath $file).Length
Write-Host "[PASS] 备份完成：$file（$([math]::Round($size / 1KB, 1)) KB）" -ForegroundColor Green
