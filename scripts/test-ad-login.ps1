param(
    [string]$LdapHost = "10.15.0.41",
    [string]$LdapPort = "389",
    [string]$DomainDns = "podjudsp.local",
    [string]$DomainNetbios = "PODJUDSP"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.DirectoryServices.Protocols

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

function Test-LdapBind {
    param(
        [string]$Label,
        [System.DirectoryServices.Protocols.AuthType]$AuthType,
        [System.Net.NetworkCredential]$Credential
    )

    $identifier = [System.DirectoryServices.Protocols.LdapDirectoryIdentifier]::new($LdapHost, [int]$LdapPort, $false, $false)
    $connection = [System.DirectoryServices.Protocols.LdapConnection]::new($identifier)
    $connection.AuthType = $AuthType
    $connection.SessionOptions.ProtocolVersion = 3
    $connection.Timeout = [TimeSpan]::FromSeconds(8)
    $connection.Credential = $Credential
    try {
        $connection.Bind()
        Write-Host "OK   $Label" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "FALLA $Label" -ForegroundColor Red
        Write-Host "      $($_.Exception.Message)"
        if ($_.Exception.InnerException) {
            Write-Host "      $($_.Exception.InnerException.Message)"
        }
        return $false
    } finally {
        $connection.Dispose()
    }
}

Write-Host "Prueba de login Active Directory" -ForegroundColor Cyan
Write-Host "Servidor: ${LdapHost}:$LdapPort"
Write-Host "Dominio: $DomainDns / $DomainNetbios"
Write-Host ""

$reachable = Test-NetConnection -ComputerName $LdapHost -Port $LdapPort -InformationLevel Quiet
if (-not $reachable) {
    throw "No se puede conectar a ${LdapHost}:$LdapPort."
}
Write-Host "OK   Puerto LDAP accesible" -ForegroundColor Green

$username = Read-Host -Prompt "Usuario AD corto, sin dominio"
$password = Read-SecretPlain -Prompt "Clave AD para $username"

$ok = $false
$ok = (Test-LdapBind "Simple bind UPN: $username@$DomainDns" `
    ([System.DirectoryServices.Protocols.AuthType]::Basic) `
    ([System.Net.NetworkCredential]::new("$username@$DomainDns", $password))) -or $ok

$ok = (Test-LdapBind "Negotiate: $DomainNetbios\$username" `
    ([System.DirectoryServices.Protocols.AuthType]::Negotiate) `
    ([System.Net.NetworkCredential]::new($username, $password, $DomainNetbios))) -or $ok

$ok = (Test-LdapBind "Simple bind NetBIOS: $DomainNetbios\$username" `
    ([System.DirectoryServices.Protocols.AuthType]::Basic) `
    ([System.Net.NetworkCredential]::new("$DomainNetbios\$username", $password))) -or $ok

Write-Host ""
if ($ok) {
    Write-Host "Resultado: AD acepto al menos un formato de login." -ForegroundColor Green
    Write-Host "Si la app sigue rechazando, el problema esta en la configuracion Spring o autorizacion local."
} else {
    Write-Host "Resultado: AD rechazo todos los formatos probados." -ForegroundColor Red
    Write-Host "Revisar usuario, clave, cuenta bloqueada, dominio o politica de autenticacion LDAP."
}
