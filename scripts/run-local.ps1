$ErrorActionPreference = "Stop"

$passwordFile = Join-Path $PSScriptRoot "..\.local-secrets\mysql-password.txt"

if (-not (Test-Path $passwordFile)) {
    Write-Host ""
    Write-Host "MySQL password file not found." -ForegroundColor Red
    Write-Host "Create it first with:" -ForegroundColor Yellow
    Write-Host "  .\scripts\setup-local-secret.ps1" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

$securePassword = Get-Content $passwordFile | ConvertTo-SecureString

$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

try {
    $env:SPRING_DATASOURCE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}

Write-Host ""
Write-Host "Starting FarmConnect backend..." -ForegroundColor Green
Write-Host "Database: localhost:3306/farmconnect_db" -ForegroundColor Cyan
Write-Host ""

mvn spring-boot:run
