<#
.SYNOPSIS
    Script de Gestión y Ciclo de Vida para Stocky Backend en Azure (12-Factor App: Disposability)
.DESCRIPTION
    Permite ver el estado en tiempo real, detener, iniciar y reiniciar el App Service directamente desde PowerShell.
    Utiliza autenticación directa por Azure REST API con Service Principal (no requiere Azure CLI instalado).
#>

param(
    [ValidateSet("status", "stop", "start", "restart", "menu")]
    [string]$Action = "menu"
)

$APP_NAME        = "stocky-backend"
$RESOURCE_GROUP  = "stocky-rg"
$SUBSCRIPTION_ID = "b8ef6f00-cbeb-471c-abef-34f5971a3450"
$TENANT_ID       = "b1eea3ae-be33-4355-a382-19c8ce58ee71"
$CLIENT_ID       = "0fff0d7f-22e0-42e5-ad6b-302d494f5fe5"
$HEALTH_URL      = "https://stocky-backend-e7e5bbebatdmb9d2.brazilsouth-01.azurewebsites.net/actuator/health"

# Carga segura de Client Secret (desde variable de entorno o archivo local .azure-credentials.json)
$CLIENT_SECRET = $env:AZURE_CLIENT_SECRET
$CREDS_FILE    = Join-Path $PSScriptRoot ".azure-credentials.json"
if (-not $CLIENT_SECRET -and (Test-Path $CREDS_FILE)) {
    try {
        $creds = Get-Content $CREDS_FILE -Raw | ConvertFrom-Json
        $CLIENT_SECRET = $creds.CLIENT_SECRET
    } catch {}
}

$script:cachedToken = $null

function Get-AzureToken {
    if ($script:cachedToken) {
        return $script:cachedToken
    }
    Write-Host "  >> Autenticando con Azure Service Principal..." -ForegroundColor Gray
    $body = @{
        grant_type    = "client_credentials"
        client_id     = $CLIENT_ID
        client_secret = $CLIENT_SECRET
        scope         = "https://management.azure.com/.default"
    }
    try {
        $res = Invoke-RestMethod -Uri "https://login.microsoftonline.com/$TENANT_ID/oauth2/v2.0/token" -Method Post -Body $body -TimeoutSec 10
        $script:cachedToken = $res.access_token
        return $script:cachedToken
    } catch {
        Write-Host "  >> Error de autenticacion con Azure: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

function Invoke-AzureArmApi {
    param(
        [string]$Method,
        [string]$SubPath = ""
    )
    $token = Get-AzureToken
    if (-not $token) { return $null }

    $headers = @{
        Authorization = "Bearer $token"
    }
    $url = "https://management.azure.com/subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.Web/sites/$APP_NAME$SubPath`?api-version=2022-03-01"
    try {
        return Invoke-RestMethod -Uri $url -Method $Method -Headers $headers -TimeoutSec 20
    } catch {
        Write-Host "  >> Error en llamada Azure API: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

function Show-Header {
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "     STOCKY - CONTROL DE CICLO DE VIDA EN AZURE CLOUD     " -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "App Service:    $APP_NAME"
    Write-Host "Resource Group: $RESOURCE_GROUP"
    Write-Host "Health URL:     $HEALTH_URL"
    Write-Host "----------------------------------------------------------"
}

function Check-Health {
    Write-Host "`n[1/2] Consultando estado en Azure Cloud..." -ForegroundColor Yellow
    $appInfo = Invoke-AzureArmApi -Method Get
    if ($appInfo -and $appInfo.properties) {
        $state = $appInfo.properties.state
        if ($state -eq "Running") {
            Write-Host "  >> ESTADO AZURE: RUNNING (En ejecucion)" -ForegroundColor Green
        } elseif ($state -eq "Stopped") {
            Write-Host "  >> ESTADO AZURE: STOPPED (Detenido)" -ForegroundColor Red
        } else {
            Write-Host "  >> ESTADO AZURE: $state" -ForegroundColor Yellow
        }
    }

    Write-Host "`n[2/2] Consultando endpoint de salud (Spring Boot Actuator)..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri $HEALTH_URL -Method Get -TimeoutSec 4 -ErrorAction Stop
        Write-Host "  >> HTTP ACTUATOR: EN LINEA (200 OK)" -ForegroundColor Green
        Write-Host "  >> JSON: $($response | ConvertTo-Json -Compress)" -ForegroundColor Gray
    } catch {
        Write-Host "  >> HTTP ACTUATOR: FUERA DE SERVICIO / DETENIDO" -ForegroundColor Red
    }
}

function Stop-App {
    Write-Host "`n[!] Enviando orden de DETENER (Stop) a Azure..." -ForegroundColor Yellow
    $res = Invoke-AzureArmApi -Method Post -SubPath "/stop"
    Write-Host "  >> Orden 'stop' ejecutada con exito." -ForegroundColor Green
    Start-Sleep -Seconds 3
    Check-Health
}

function Wait-ForActuator {
    Write-Host "  >> Esperando arranque de Spring Boot y conexion a PostgreSQL (Cold Start)..." -ForegroundColor Gray
    $maxAttempts = 12
    $attempt = 1
    while ($attempt -le $maxAttempts) {
        Start-Sleep -Seconds 3
        try {
            $resp = Invoke-RestMethod -Uri $HEALTH_URL -Method Get -TimeoutSec 3 -ErrorAction Stop
            if ($resp.status -eq "UP") {
                Write-Host "  >> Spring Boot inicio exitosamente despues de $($attempt * 3) segundos!" -ForegroundColor Green
                return
            }
        } catch {
            Write-Host "     [Intento $attempt/$maxAttempts] Esperando que Tomcat y la DB respondan..." -ForegroundColor DarkGray
        }
        $attempt++
    }
}

function Start-App {
    Write-Host "`n[!] Enviando orden de INICIAR (Start) a Azure..." -ForegroundColor Yellow
    $res = Invoke-AzureArmApi -Method Post -SubPath "/start"
    Write-Host "  >> Orden 'start' ejecutada con exito." -ForegroundColor Green
    Wait-ForActuator
    Check-Health
}

function Restart-App {
    Write-Host "`n[!] Enviando orden de REINICIAR (Restart) a Azure..." -ForegroundColor Yellow
    Write-Host "  (Demostracion 12-Factor App: Disposability / Apagado y Recreacion rapida)" -ForegroundColor DarkCyan
    $res = Invoke-AzureArmApi -Method Post -SubPath "/restart"
    Write-Host "  >> Orden 'restart' ejecutada con exito." -ForegroundColor Green
    Wait-ForActuator
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
            Write-Host "  2. Detener Backend (Stop)"
            Write-Host "  3. Iniciar Backend (Start)"
            Write-Host "  4. Reiniciar Backend (Restart - 12 Factor Disposability)"
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
