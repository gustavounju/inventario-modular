param(
    [string]$ServerUrl = "http://localhost:8081/api/v1/equipos/inventario",
    [string]$Token = $env:INVENTARIO_REPORT_TOKEN,
    [string]$Fuero = $env:INVENTARIO_FUERO,
    [string]$BackupDirectory = "$env:ProgramData\InventarioModular",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Token)) {
    $Token = "dev-token-123456"
}

function ConvertTo-JsonString {
    param($Value)
    if ($null -eq $Value) {
        return ""
    }
    $Text = [string]$Value
    $Text = $Text.Replace("\", "\\")
    $Text = $Text.Replace('"', '\"')
    $Text = $Text -replace "`r", " "
    $Text = $Text -replace "`n", " "
    return $Text.Trim()
}

function Get-InventoryClass {
    param(
        [string]$ClassName,
        [string]$Filter = ""
    )
    if (Get-Command Get-CimInstance -ErrorAction SilentlyContinue) {
        if ([string]::IsNullOrWhiteSpace($Filter)) {
            return Get-CimInstance -ClassName $ClassName
        }
        return Get-CimInstance -ClassName $ClassName -Filter $Filter
    }
    if ([string]::IsNullOrWhiteSpace($Filter)) {
        return Get-WmiObject -Class $ClassName
    }
    return Get-WmiObject -Class $ClassName -Filter $Filter
}

function Get-FirstIPv4 {
    try {
        $Adapters = Get-InventoryClass -ClassName "Win32_NetworkAdapterConfiguration" -Filter "IPEnabled = True"
        foreach ($Adapter in $Adapters) {
            foreach ($Address in $Adapter.IPAddress) {
                if ($Address -match "^\d{1,3}(\.\d{1,3}){3}$" -and
                    -not $Address.StartsWith("127.") -and
                    -not $Address.StartsWith("169.254.")) {
                    return $Address
                }
            }
        }
    }
    catch {
        return ""
    }
    return ""
}

function Get-PrinterName {
    try {
        $DefaultPrinter = Get-InventoryClass -ClassName "Win32_Printer" | Where-Object { $_.Default -eq $true } | Select-Object -First 1
        if ($DefaultPrinter -and $DefaultPrinter.Name) {
            return $DefaultPrinter.Name
        }
        $Printer = Get-InventoryClass -ClassName "Win32_Printer" | Where-Object { $_.Name -notmatch "PDF|XPS|OneNote|Fax" } | Select-Object -First 1
        if ($Printer -and $Printer.Name) {
            return $Printer.Name
        }
    }
    catch {
        return ""
    }
    return ""
}

function Save-InventoryBackup {
    param(
        [string]$Json,
        [string]$ComputerName
    )
    if (-not (Test-Path $BackupDirectory)) {
        New-Item -ItemType Directory -Path $BackupDirectory | Out-Null
    }
    $Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $Path = Join-Path $BackupDirectory "inventario-$ComputerName-$Timestamp.json"
    [System.IO.File]::WriteAllText($Path, $Json, [System.Text.Encoding]::UTF8)
    return $Path
}

$ComputerSystem = Get-InventoryClass -ClassName "Win32_ComputerSystem"
$OperatingSystem = Get-InventoryClass -ClassName "Win32_OperatingSystem"
$Processor = Get-InventoryClass -ClassName "Win32_Processor" | Select-Object -First 1

$ComputerName = $env:COMPUTERNAME
$CurrentUser = $env:USERNAME
$IpAddress = Get-FirstIPv4
$RamMb = 0
if ($ComputerSystem.TotalPhysicalMemory) {
    $RamMb = [int][Math]::Round($ComputerSystem.TotalPhysicalMemory / 1MB)
}

$Json = "{"
$Json += '"nombre":"' + (ConvertTo-JsonString $ComputerName) + '",'
$Json += '"ultimoUsuario":"' + (ConvertTo-JsonString $CurrentUser) + '",'
$Json += '"fuero":"' + (ConvertTo-JsonString $Fuero) + '",'
$Json += '"ip":"' + (ConvertTo-JsonString $IpAddress) + '",'
$Json += '"sistemaOperativo":"' + (ConvertTo-JsonString $OperatingSystem.Caption) + '",'
$Json += '"procesador":"' + (ConvertTo-JsonString $Processor.Name) + '",'
$Json += '"ramMb":' + $RamMb + ','
$Json += '"impresora":"' + (ConvertTo-JsonString (Get-PrinterName)) + '",'
$Json += '"activo":true'
$Json += "}"

if ($DryRun) {
    Write-Host $Json
    exit 0
}

Write-Host "Enviando inventario de $ComputerName a $ServerUrl ..."

try {
    $Client = New-Object System.Net.WebClient
    $Client.Headers.Add("Content-Type", "application/json; charset=utf-8")
    $Client.Headers.Add("Authorization", "Bearer $Token")
    $Client.Encoding = [System.Text.Encoding]::UTF8
    $Response = $Client.UploadString($ServerUrl, "POST", $Json)
    Write-Host "Inventario enviado correctamente." -ForegroundColor Green
    Write-Host $Response
}
catch {
    Write-Host "No se pudo enviar el inventario: $($_.Exception.Message)" -ForegroundColor Yellow
    $BackupPath = Save-InventoryBackup -Json $Json -ComputerName $ComputerName
    Write-Host "Se guardo una copia local en: $BackupPath" -ForegroundColor Yellow
    exit 1
}
