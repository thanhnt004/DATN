# Quick Start Script for Sellico Backend
# Starts all services without rebuilding

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Starting Sellico Microservices" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "Checking Docker status..." -ForegroundColor Yellow
try {
    docker info | Out-Null
    Write-Host "✓ Docker is running" -ForegroundColor Green
}
catch {
    Write-Host "✗ Docker is not running!" -ForegroundColor Red
    Write-Host "Please start Docker Desktop and try again." -ForegroundColor Red
    exit 1
}

# Start all services
Write-Host ""
Write-Host "Starting all services..." -ForegroundColor Yellow
docker-compose up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "✓ All services started!" -ForegroundColor Green
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Checking service status..." -ForegroundColor Yellow
    docker-compose ps
}
else {
    Write-Host "✗ Failed to start services" -ForegroundColor Red
    Write-Host "Try running build-all.ps1 first" -ForegroundColor Yellow
    exit 1
}


