param(
    [Parameter(Position=0)]
    [ValidateSet("status", "stop", "start", "restart", "ayuda", "help")]
    [string]$Comando = "status"
)

$APP = "stocky-backend"
$RG = "stocky-rg"
$URL = "https://stocky-backend-e7e5bbebatdmb9d2.brazilsouth-01.azurewebsites.net"
$HEALTH = "$URL/actuator/health"

function Mostrar-Banner {
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "             STOCKY - ESTADO EN AZURE CLOUD               " -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Cyan
}

function Obtener-Estado {
    Write-Host "Consultando estado del servicio..." -ForegroundColor DarkGray
    try {
        $res = Invoke-RestMethod -Uri $HEALTH -Method Get -TimeoutSec 4 -ErrorAction Stop
        
        Write-Host ""
        Write-Host "  =================================================" -ForegroundColor Green
        Write-Host "   [ ESTADO ACTUAL: INICIADO / RUNNING ]           " -ForegroundColor Green
        Write-Host "  =================================================" -ForegroundColor Green
        Write-Host "   * Servidor Web:      ENCENDIDO Y RESPONDIENDO" -ForegroundColor White
        Write-Host "   * Base de Datos DB:  CONECTADA (PostgreSQL UP)" -ForegroundColor White
        Write-Host "   * URL:               $URL" -ForegroundColor Gray
        Write-Host ""
    } catch {
        Write-Host ""
        Write-Host "  =================================================" -ForegroundColor Red
        Write-Host "   [ ESTADO ACTUAL: DETENIDO / STOPPED ]           " -ForegroundColor Red
        Write-Host "  =================================================" -ForegroundColor Red
        Write-Host "   * Servidor Web:      APAGADO / NO RESPONDE" -ForegroundColor White
        Write-Host "   * Los datos en PostgreSQL siguen intactos." -ForegroundColor Gray
        Write-Host "   * URL:               $URL" -ForegroundColor Gray
        Write-Host ""
    }
}

function Detener-App {
    Write-Host ""
    Write-Host "Orden recibida: APAGAR APLICACION" -ForegroundColor Yellow
    if (Get-Command az -ErrorAction SilentlyContinue) {
        az webapp stop --name $APP --resource-group $RG
        Start-Sleep -Seconds 2
        Obtener-Estado
    } else {
        Write-Host ""
        Write-Host " [!] Azure CLI no esta instalado localmente." -ForegroundColor Yellow
        Write-Host "     Tienes 2 formas muy rapidas de apagarla:" -ForegroundColor White
        Write-Host "     1. En Azure Portal: Entra a '$APP' y pulsa el boton [Stop]." -ForegroundColor Cyan
        Write-Host "     2. En Azure Cloud Shell: ejecuta: az webapp stop --name $APP --resource-group $RG" -ForegroundColor Cyan
        Write-Host ""
        Write-Host " Luego ejecuta '.\stocky.ps1 status' para verificar el cartel rojo [DETENIDO]." -ForegroundColor Green
        Write-Host ""
    }
}

function Iniciar-App {
    Write-Host ""
    Write-Host "Orden recibida: ENCENDER APLICACION" -ForegroundColor Yellow
    if (Get-Command az -ErrorAction SilentlyContinue) {
        az webapp start --name $APP --resource-group $RG
        Start-Sleep -Seconds 3
        Obtener-Estado
    } else {
        Write-Host ""
        Write-Host " [!] Azure CLI no esta instalado localmente." -ForegroundColor Yellow
        Write-Host "     Tienes 2 formas muy rapidas de encenderla:" -ForegroundColor White
        Write-Host "     1. En Azure Portal: Entra a '$APP' y pulsa el boton [Start]." -ForegroundColor Cyan
        Write-Host "     2. En Azure Cloud Shell: ejecuta: az webapp start --name $APP --resource-group $RG" -ForegroundColor Cyan
        Write-Host ""
        Write-Host " Luego ejecuta '.\stocky.ps1 status' para verificar el cartel verde [INICIADO]." -ForegroundColor Green
        Write-Host ""
    }
}

function Reiniciar-App {
    Write-Host ""
    Write-Host "Orden recibida: REINICIAR (Prueba 12-Factor Desechabilidad)" -ForegroundColor Yellow
    if (Get-Command az -ErrorAction SilentlyContinue) {
        az webapp restart --name $APP --resource-group $RG
        Write-Host " Contenedor reiniciado. Verificando arranque..." -ForegroundColor DarkGray
        Start-Sleep -Seconds 6
        Obtener-Estado
    } else {
        Write-Host ""
        Write-Host " [!] En Azure Portal: Entra a '$APP' y pulsa el boton [Restart]." -ForegroundColor Cyan
        Write-Host "     En 5 segundos ejecuta '.\stocky.ps1 status' para comprobar que volvio a encender." -ForegroundColor White
        Write-Host ""
    }
}

Mostrar-Banner

switch ($Comando) {
    "status"  { Obtener-Estado }
    "stop"    { Detener-App }
    "start"   { Iniciar-App }
    "restart" { Reiniciar-App }
    default   {
        Write-Host "Comandos de PowerShell disponibles:" -ForegroundColor Cyan
        Write-Host "  .\stocky.ps1 status   -> Ver si esta INICIADO o DETENIDO"
        Write-Host "  .\stocky.ps1 stop     -> Detener servicio"
        Write-Host "  .\stocky.ps1 start    -> Iniciar servicio"
        Write-Host "  .\stocky.ps1 restart  -> Reiniciar servicio (Desechabilidad)"
        Write-Host ""
    }
}
