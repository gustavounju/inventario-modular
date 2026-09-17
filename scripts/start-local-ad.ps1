param(
    [string]$ServerPort = "8081",
    [string]$RemoteMysqlHost = "MYSQL_INTERNO_IP",
    [string]$RemoteMysqlPort = "3306",
    [string]$RemoteMysqlDatabase = "inventario_modular",
    [string]$RemoteMysqlDefaultUser = "inventario_modular_app",
    [string]$LocalMysqlHost = "127.0.0.1",
    [string]$LocalMysqlPort = "3306",
    [string]$LocalMysqlDatabase = "inventario_modular",
    [string]$LocalMysqlDefaultUser = "inventario_local",
    [string]$LdapUrl = "ldap://10.15.0.41:389",
    [string]$LdapDomain = "podjudsp.local",
    [string]$LdapBaseDn = "DC=podjudsp,DC=local",
    [string]$LdapUserSearchBase = "OU=USUARIOS,OU=PODJUDSP",
    [string]$LdapDefaultUser = "gmurad",
    [string]$LocalAdminPassword = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

function Read-SecretPlain {
    param([string]$Prompt)

    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        if ($bstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
}

function Find-MysqlExe {
    $mysqlExe = Get-Command mysql.exe -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source
    if ($mysqlExe) {
        return $mysqlExe
    }
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Workbench 8.0 CE\mysql.exe"
    )
    return $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
}

function Test-MysqlLogin {
    param(
        [string]$MysqlExe,
        [string]$HostName,
        [string]$Port,
        [string]$Database,
        [string]$UserName,
        [string]$Password
    )

    $oldMysqlPwd = $env:MYSQL_PWD
    $env:MYSQL_PWD = $Password
    try {
        $output = & $MysqlExe --host=$HostName --port=$Port --protocol=tcp --user=$UserName `
            --database=$Database --batch --skip-column-names --execute="SELECT 1;" 2>&1
        return $LASTEXITCODE -eq 0 -and (($output -join "`n") -match "1")
    } finally {
        if ($null -eq $oldMysqlPwd) {
            Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
        } else {
            $env:MYSQL_PWD = $oldMysqlPwd
        }
    }
}

Write-Host "Inventario Modular local + Active Directory" -ForegroundColor Cyan
Write-Host "Base de datos: primero MySQL remoto (${RemoteMysqlHost}:$RemoteMysqlPort/$RemoteMysqlDatabase); fallback MySQL local (${LocalMysqlHost}:$LocalMysqlPort/$LocalMysqlDatabase)."
Write-Host "LDAP: $LdapUrl / $LdapDomain"
Write-Host "LDAP base login: $LdapBaseDn"
Write-Host "LDAP base busqueda usuarios: $LdapUserSearchBase"
Write-Host ""

$mysqlExe = Find-MysqlExe
if (-not $mysqlExe) {
    throw "No encontre mysql.exe para validar credenciales MySQL. Ejecute primero .\scripts\setup-local-mysql.ps1."
}

$remoteJdbcUrl = "jdbc:mysql://$RemoteMysqlHost`:$RemoteMysqlPort/$RemoteMysqlDatabase`?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Argentina/Buenos_Aires"
$localJdbcUrl = "jdbc:mysql://$LocalMysqlHost`:$LocalMysqlPort/$LocalMysqlDatabase`?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Argentina/Buenos_Aires"

$remoteReachable = Test-NetConnection -ComputerName $RemoteMysqlHost -Port $RemoteMysqlPort -InformationLevel Quiet -WarningAction SilentlyContinue
$remoteOk = $false
$remoteMysqlUser = $RemoteMysqlDefaultUser
$remoteMysqlPassword = ""
if ($remoteReachable) {
    Write-Host "OK   MySQL remoto responde en ${RemoteMysqlHost}:$RemoteMysqlPort" -ForegroundColor Green
    for ($attempt = 1; $attempt -le 3 -and -not $remoteOk; $attempt++) {
        $remoteMysqlUser = Read-Host -Prompt "Usuario MySQL REMOTO [$RemoteMysqlDefaultUser]"
        if ([string]::IsNullOrWhiteSpace($remoteMysqlUser)) {
            $remoteMysqlUser = $RemoteMysqlDefaultUser
        }
        $remoteMysqlPassword = Read-SecretPlain -Prompt "Clave MySQL REMOTO para $remoteMysqlUser"
        $remoteOk = Test-MysqlLogin $mysqlExe $RemoteMysqlHost $RemoteMysqlPort $RemoteMysqlDatabase $remoteMysqlUser $remoteMysqlPassword
        if (-not $remoteOk) {
            Write-Host "MySQL remoto rechazo ese usuario o clave para $RemoteMysqlDatabase. Reintente." -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "AVISO MySQL remoto no responde en ${RemoteMysqlHost}:$RemoteMysqlPort. Se probara MySQL local." -ForegroundColor Yellow
}

$localOk = $false
$localMysqlUser = $LocalMysqlDefaultUser
$localMysqlPassword = ""
if (-not $remoteOk) {
    for ($attempt = 1; $attempt -le 3 -and -not $localOk; $attempt++) {
        $localMysqlUser = Read-Host -Prompt "Usuario MySQL LOCAL [$LocalMysqlDefaultUser]"
        if ([string]::IsNullOrWhiteSpace($localMysqlUser)) {
            $localMysqlUser = $LocalMysqlDefaultUser
        }
        $localMysqlPassword = Read-SecretPlain -Prompt "Clave MySQL LOCAL para $localMysqlUser"
        $localOk = Test-MysqlLogin $mysqlExe $LocalMysqlHost $LocalMysqlPort $LocalMysqlDatabase $localMysqlUser $localMysqlPassword
        if (-not $localOk) {
            Write-Host "MySQL local rechazo ese usuario o clave para $LocalMysqlDatabase. Reintente o ejecute .\scripts\setup-local-mysql.ps1." -ForegroundColor Yellow
        }
    }
}
if (-not $remoteOk -and -not $localOk) {
    throw "No se pudo validar MySQL remoto ni MySQL local. No se arranca la app hasta corregir red, usuario, clave o base."
}
$activeDbLabel = if ($remoteOk) { "remoto" } else { "local" }
Write-Host "OK   MySQL $activeDbLabel valido" -ForegroundColor Green

$ldapUri = [Uri]$LdapUrl
$ldapPort = if ($ldapUri.Port -gt 0) { $ldapUri.Port } else { 389 }
$ldapReachable = Test-NetConnection -ComputerName $ldapUri.Host -Port $ldapPort -InformationLevel Quiet -WarningAction SilentlyContinue
$ldapEnabled = $false
$ldapUser = ""
$ldapPassword = ""
if ($ldapReachable) {
    $ldapEnabled = $true
    Write-Host "OK   Active Directory responde en $($ldapUri.Host):$ldapPort" -ForegroundColor Green
    $ldapUser = Read-Host -Prompt "Usuario lector AD [$LdapDefaultUser]"
    if ([string]::IsNullOrWhiteSpace($ldapUser)) {
        $ldapUser = $LdapDefaultUser
    }
    if ($ldapUser -notmatch "[@\\]") {
        $ldapUser = "$ldapUser@$LdapDomain"
    }
    $ldapPassword = Read-SecretPlain -Prompt "Clave AD para $ldapUser"
} else {
    Write-Host "AVISO Active Directory no responde en $($ldapUri.Host):$ldapPort. Se arrancara con LDAP deshabilitado y usuarios locales." -ForegroundColor Yellow
}

$localIpv4 = (Get-NetIPConfiguration |
    Where-Object { $_.IPv4DefaultGateway -and $_.IPv4Address } |
    ForEach-Object { $_.IPv4Address.IPAddress } |
    Where-Object { $_ -notlike "169.254.*" -and $_ -ne "127.0.0.1" } |
    Select-Object -First 1)

$env:SPRING_PROFILES_ACTIVE = "local"
$env:INVENTARIO_SERVER_PORT = $ServerPort
$env:INVENTARIO_LOCAL_AUTH_ENABLED = "true"
$env:INVENTARIO_LOCAL_AUTH_USERNAME = "admin.local"
if ([string]::IsNullOrWhiteSpace($LocalAdminPassword)) {
    $LocalAdminPassword = Read-SecretPlain -Prompt "Clave bootstrap para admin.local"
}
$env:INVENTARIO_LOCAL_AUTH_PASSWORD = $LocalAdminPassword
$env:INVENTARIO_LOCAL_DB_AUTH_ENABLED = "true"
$env:INVENTARIO_MOVIL_APK_PATH = Join-Path $repoRoot "output\android\inventario-tareas-lan-piloto.apk"

if ($remoteOk) {
    $env:INVENTARIO_DB_PRIMARY_URL = $remoteJdbcUrl
    $env:INVENTARIO_DB_PRIMARY_USER = $remoteMysqlUser
    $env:INVENTARIO_DB_PRIMARY_PASSWORD = $remoteMysqlPassword
} else {
    $env:INVENTARIO_DB_PRIMARY_URL = $localJdbcUrl
    $env:INVENTARIO_DB_PRIMARY_USER = $localMysqlUser
    $env:INVENTARIO_DB_PRIMARY_PASSWORD = $localMysqlPassword
}
$env:INVENTARIO_DB_FALLBACK_URL = $localJdbcUrl
$env:INVENTARIO_DB_FALLBACK_USER = $localMysqlUser
$env:INVENTARIO_DB_FALLBACK_PASSWORD = $localMysqlPassword

$env:INVENTARIO_LDAP_ENABLED = if ($ldapEnabled) { "true" } else { "false" }
$env:INVENTARIO_LDAP_URL = $LdapUrl
$env:INVENTARIO_LDAP_DOMAIN = $LdapDomain
$env:INVENTARIO_LDAP_BASE_DN = $LdapBaseDn
$env:INVENTARIO_LDAP_DISPLAY_NAME_ATTRIBUTE = "displayName"
$env:INVENTARIO_LDAP_FUERO_ATTRIBUTE = "department"
$env:INVENTARIO_LDAP_READ_ONLY_USER_DN = $ldapUser
$env:INVENTARIO_LDAP_READ_ONLY_PASSWORD = $ldapPassword
$env:INVENTARIO_LDAP_USER_SEARCH_BASE = $LdapUserSearchBase
$env:INVENTARIO_LDAP_USER_SEARCH_FILTER = "(&(objectClass=user)(!(objectClass=computer)))"
$env:INVENTARIO_LDAP_USER_SEARCH_LIMIT = "50"

Write-Host ""
Write-Host "Arrancando en http://localhost:$ServerPort" -ForegroundColor Green
if ($localIpv4) {
    Write-Host "Desde el celular, pruebe: http://$localIpv4`:$ServerPort/movil/login" -ForegroundColor Green
}
Write-Host "MySQL primario remoto: ${RemoteMysqlHost}:$RemoteMysqlPort/$RemoteMysqlDatabase con usuario $remoteMysqlUser"
Write-Host "MySQL fallback local: ${LocalMysqlHost}:$LocalMysqlPort/$LocalMysqlDatabase con usuario $localMysqlUser"
Write-Host "MySQL activo validado antes de arrancar: $activeDbLabel"
Write-Host "Active Directory: $(if ($ldapEnabled) { 'habilitado' } else { 'deshabilitado; usuarios locales' })"
Write-Host "Usuario local bootstrap: admin.local habilitado hasta guardar Active Directory; clave oculta"
Write-Host "Para detener: Ctrl+C en esta ventana."
Write-Host ""

try {
    .\mvnw.cmd spring-boot:run
} finally {
    Remove-Item Env:\INVENTARIO_DB_PRIMARY_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:\INVENTARIO_DB_FALLBACK_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:\INVENTARIO_LDAP_READ_ONLY_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:\INVENTARIO_LOCAL_AUTH_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}
