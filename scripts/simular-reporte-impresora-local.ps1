param(
    [string]$ServerUrl = "http://localhost:8081/api/v1/equipos/inventario",
    [string]$Token = "token-reporte-inventario-modular-sanpedro-2026-seguro",
    [string]$NombreEquipo = "PC-CIVIL-04",
    [string]$ImpresoraLocal = "HP LaserJet Pro M404dn",
    [string]$Usuario = "mrodriguez",
    [string]$Fuero = "Juzgado Civil y Comercial",
    [string]$Ubicacion = "Piso 1 - Secretaría 1"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " SIMULACION DE REPORTE DE TELEMETRIA: IMPRESORA LOCAL" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Equipo:           $NombreEquipo" -ForegroundColor Yellow
Write-Host "Usuario:          $Usuario" -ForegroundColor Yellow
Write-Host "Fuero:            $Fuero" -ForegroundColor Yellow
Write-Host "Ubicación:        $Ubicacion" -ForegroundColor Yellow
Write-Host "Impresora Local:  $ImpresoraLocal (Puerto local/USB - No de red)" -ForegroundColor Green
Write-Host "Destino:          $ServerUrl" -ForegroundColor Gray
Write-Host ""

$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type"  = "application/json; charset=utf-8"
    "User-Agent"    = "InventarioModular-WindowsInventory/1.0"
}

$payload = [ordered]@{
    nombre            = $NombreEquipo
    ultimoUsuario     = $Usuario
    fuero             = $Fuero
    ubicacion         = $Ubicacion
    ip                = "10.20.15.42"
    mac               = "00:1B:44:11:3A:B7"
    sistemaOperativo  = "Microsoft Windows 10 Pro 64-bit"
    procesador        = "Intel(R) Core(TM) i5-10400 CPU @ 2.90GHz"
    ramMb             = 16384
    ramDetalles       = "8GB DDR4 2666MHz | 8GB DDR4 2666MHz"
    ramSeriales       = "SN-RAM-8812 | SN-RAM-8813"
    discosModelos     = "KINGSTON SA400S37480G (480 GB) | WDC WD10EZEX (1000 GB)"
    discosSeriales    = "50026B72825C9911 | WD-WCC6Y7AL5922"
    motherboardModelo = "ASUS PRIME H410M-E"
    motherboardSerial = "200874129500123"
    monitores         = "Samsung S24R350 H4ZN801234 | LG 22MP410 204NDDV5S678"
    teclado           = "Teclado estándar PS/2 o USB"
    mouse             = "Mouse óptico USB compatible con HID"
    impresora         = $ImpresoraLocal
    activo            = $true
}

$jsonBody = $payload | ConvertTo-Json -Depth 5

Write-Host "Enviando telemetría vía POST..." -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri $ServerUrl -Method Post -Headers $headers -Body $jsonBody
    Write-Host "[OK] Reporte recibido y procesado exitosamente por el servidor!" -ForegroundColor Green
    Write-Host ""
    Write-Host "--- DETALLE REGISTRADO EN BACKEND ---" -ForegroundColor Cyan
    Write-Host "ID Equipo:          $($response.id)"
    Write-Host "Nombre:             $($response.nombre)"
    Write-Host "Estado Monitoreo:   $($response.monitoreo)"
    Write-Host "Estado Ciclo:       $($response.estadoCiclo)"
    Write-Host "Impresora Reportada:$($response.impresora)"
    Write-Host "Impresora en Red:   $($response.impresoraEnRed)" -ForegroundColor $(if ($response.impresoraEnRed) { "Red" } else { "Green" })
    Write-Host "Último Reporte:     $($response.ultimoReporteEn)"
    Write-Host ""
    Write-Host "Puedes consultar la ficha completa en el panel web:" -ForegroundColor Yellow
    Write-Host "http://localhost:8081/admin/equipos/$($response.id)" -ForegroundColor Cyan
} catch {
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host $reader.ReadToEnd() -ForegroundColor DarkRed
    }
}
