# Stop All Services Script

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Stopping Sellico Microservices" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Stopping all services..." -ForegroundColor Yellow
docker-compose down

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ All services stopped successfully!" -ForegroundColor Green
} else {
    Write-Host "✗ Failed to stop services" -ForegroundColor Red
    exit 1
}

# Ask if user wants to remove volumes
Write-Host ""
$response = Read-Host "Do you want to remove volumes (database data)? (y/N)"
if ($response -eq "y" -or $response -eq "Y") {
    Write-Host "Removing volumes..." -ForegroundColor Yellow
    docker-compose down -v
    Write-Host "✓ Volumes removed!" -ForegroundColor Green
}

