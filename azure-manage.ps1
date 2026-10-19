<#
.SYNOPSIS
    Script de Gestión y Ciclo de Vida para Stocky Backend en Azure (12-Factor App: Disposability)
.DESCRIPTION
    Permite ver el estado en tiempo real (Health Check), detener, iniciar y reiniciar el App Service en Azure.
#>

param(
    [ValidateSet("status", "stop", "start", "restart", "menu")]
    [string]$Action = "menu"
)

$APP_NAME = "stocky-backend"
$RESOURCE_GROUP = "stocky-rg"
$HEALTH_URL = "https://stocky-backend-e7e5bbebatdmb9d2.brazilsouth-01.azurewebsites.net/actuator/health"

function Show-Header {
    Clear-Host
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "     STOCKY - GESTION DE CICLO DE VIDA EN AZURE CLOUD     " -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "App Service:    $APP_NAME"
    Write-Host "Resource Group: $RESOURCE_GROUP"
    Write-Host "Health URL:     $HEALTH_URL"
    Write-Host "----------------------------------------------------------"
}

function Check-Health {
    Write-Host "`n[1/2] Consultando endpoint de salud (Spring Boot Actuator)..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri $HEALTH_URL -Method Get -TimeoutSec 5 -ErrorAction Stop
        Write-Host "  >> ESTADO HTTP: EN LINEA (200 OK)" -ForegroundColor Green
        Write-Host "  >> JSON: $($response | ConvertTo-Json -Compress)" -ForegroundColor Gray
    } catch {
        Write-Host "  >> ESTADO HTTP: DETENIDO / NO DISPONIBLE" -ForegroundColor Red
        Write-Host "  >> Detalle: $($_.Exception.Message)" -ForegroundColor DarkGray
    }

    Write-Host "`n[2/2] Consultando estado en Azure CLI (si esta disponible)..." -ForegroundColor Yellow
    if (Get-Command az -ErrorAction SilentlyContinue) {
        try {
            $state = az webapp show --name $APP_NAME --resource-group $RESOURCE_GROUP --query "state" -o tsv 2>$null
            if ($state -eq "Running") {
                Write-Host "  >> ESTADO AZURE: RUNNING (Ejecutandose)" -ForegroundColor Green
            } elseif ($state -eq "Stopped") {
                Write-Host "  >> ESTADO AZURE: STOPPED (Detenido)" -ForegroundColor Red
            } else {
                Write-Host "  >> ESTADO AZURE: $state" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "  >> (No se pudo consultar Azure CLI: verifica tu sesion con 'az login')" -ForegroundColor DarkGray
        }
    } else {
        Write-Host "  >> (Azure CLI no instalado localmente; el estado HTTP de arriba es la fuente directa)" -ForegroundColor DarkGray
    }
}

function Stop-App {
    Write-Host "`n[!] Deteniendo App Service '$APP_NAME' en Azure..." -ForegroundColor Yellow
    if (Get-Command az -ErrorAction SilentlyContinue) {
        az webapp stop --name $APP_NAME --resource-group $RESOURCE_GROUP
        Write-Host "  >> Comando 'stop' enviado a Azure." -ForegroundColor Green
    } else {
        Write-Host "  >> Azure CLI no detectado en esta terminal." -ForegroundColor Red
        Write-Host "  >> Para detenerlo por consola sin instalar nada, ejecuta en Azure Cloud Shell:" -ForegroundColor Yellow
        Write-Host "     az webapp stop --name $APP_NAME --resource-group $RESOURCE_GROUP" -ForegroundColor White
        Write-Host "  >> O desde Azure Portal: Entra a '$APP_NAME' y presiona el boton [Stop]." -ForegroundColor Cyan
    }
    Start-Sleep -Seconds 2
    Check-Health
}

function Start-App {
    Write-Host "`n[!] Iniciando App Service '$APP_NAME' en Azure..." -ForegroundColor Yellow
    if (Get-Command az -ErrorAction SilentlyContinue) {
        az webapp start --name $APP_NAME --resource-group $RESOURCE_GROUP
        Write-Host "  >> Comando 'start' enviado a Azure." -ForegroundColor Green
    } else {
        Write-Host "  >> Azure CLI no detectado en esta terminal." -ForegroundColor Red
        Write-Host "  >> Para iniciarlo por consola sin instalar nada, ejecuta en Azure Cloud Shell:" -ForegroundColor Yellow
        Write-Host "     az webapp start --name $APP_NAME --resource-group $RESOURCE_GROUP" -ForegroundColor White
        Write-Host "  >> O desde Azure Portal: Entra a '$APP_NAME' y presiona el boton [Start]." -ForegroundColor Cyan
    }
    Start-Sleep -Seconds 3
    Check-Health
}

function Restart-App {
    Write-Host "`n[!] Reiniciando App Service '$APP_NAME' (Prueba de 12-Factor Disposability)..." -ForegroundColor Yellow
    if (Get-Command az -ErrorAction SilentlyContinue) {
        az webapp restart --name $APP_NAME --resource-group $RESOURCE_GROUP
        Write-Host "  >> Contenedor destruido y recreado. Esperando arranque del nuevo proceso..." -ForegroundColor Green
        Start-Sleep -Seconds 8
    } else {
        Write-Host "  >> Azure CLI no detectado en esta terminal." -ForegroundColor Red
        Write-Host "  >> Para reiniciar por consola ejecuta en Azure Cloud Shell:" -ForegroundColor Yellow
        Write-Host "     az webapp restart --name $APP_NAME --resource-group $RESOURCE_GROUP" -ForegroundColor White
        Write-Host "  >> O desde Azure Portal: Entra a '$APP_NAME' y presiona el boton [Restart]." -ForegroundColor Cyan
    }
    Check-Health
}

# Flujo principal
Show-Header

switch ($Action) {
    "status"  { Check-Health }
    "stop"    { Stop-App }
    "start"   { Start-App }
    "restart" { Restart-App }
    "menu" {
        do {
            Write-Host "`nSelecciona una opcion:" -ForegroundColor Cyan
            Write-Host "  1. Ver Estado en Tiempo Real (Health Check)"
            Write-Host "  2. Detener Aplicacion (Stop)"
            Write-Host "  3. Iniciar Aplicacion (Start)"
            Write-Host "  4. Reiniciar Aplicacion (Restart - 12-Factor Demo)"
            Write-Host "  5. Salir"
            $choice = Read-Host "`nIngresa opcion (1-5)"

            switch ($choice) {
                "1" { Check-Health }
                "2" { Stop-App }
                "3" { Start-App }
                "4" { Restart-App }
                "5" { Write-Host "Saliendo..."; break }
                default { Write-Host "Opcion invalida." -ForegroundColor Red }
            }
        } while ($choice -ne "5")
    }
}
