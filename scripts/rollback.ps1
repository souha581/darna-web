param(
    [Parameter(Mandatory = $true)]
    [string]$TargetImage,
    [string]$TargetVersion = "rollback",
    [string]$BaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"

Write-Host "[rollback] Reverting test environment to stable image: $TargetImage"
$env:DARNA_IMAGE = $TargetImage
$env:APP_VERSION = $TargetVersion

docker compose -f docker-compose.test.yml pull app-test
docker compose -f docker-compose.test.yml up -d --force-recreate app-test

Write-Host "[rollback] Verifying restored service via smoke tests..."
& "$PSScriptRoot\smoke-test.ps1" -BaseUrl $BaseUrl

New-Item -ItemType Directory -Force -Path "deployments" | Out-Null
$ts = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
Add-Content -Path "deployments\history.log" -Value "$ts | ROLLBACK | image=$TargetImage | version=$TargetVersion | status=SUCCESS"
Write-Host "[rollback] Rollback completed and recorded in deployments\history.log"
