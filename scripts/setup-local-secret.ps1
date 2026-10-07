$ErrorActionPreference = "Stop"

$secretDirectory = Join-Path $PSScriptRoot "..\.local-secrets"
$passwordFile = Join-Path $secretDirectory "mysql-password.txt"

New-Item -ItemType Directory -Force $secretDirectory | Out-Null

Write-Host ""
Write-Host "Enter your local MySQL root password." -ForegroundColor Cyan
Write-Host "The password will be stored encrypted using Windows DPAPI." -ForegroundColor DarkGray
Write-Host ""

$securePassword = Read-Host "MySQL password" -AsSecureString

$securePassword |
    ConvertFrom-SecureString |
    Set-Content $passwordFile

Write-Host ""
Write-Host "Encrypted MySQL password saved successfully." -ForegroundColor Green
Write-Host "File: .local-secrets\mysql-password.txt" -ForegroundColor DarkGray
Write-Host ""
