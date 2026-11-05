# 🛡️ Guía Rápida para la Oficina (Despliegue y Firma GPG)

Este documento resume los pasos exactos para continuar el trabajo en la oficina.

---

## 1. Servidor de Producción (Ubuntu)

En el servidor **NO** hay que configurar nada nuevo de GPG ni contraseñas. Son los comandos de siempre:

```bash
cd /ruta/de/tu/proyecto
git pull gitlab Develop    # o git pull github Develop
./mvnw clean package -DskipTests
sudo systemctl restart inventario-modular   # o reiniciar el servicio
```

El servidor solo lee el código ya firmado y aprobado.

---

## 2. Nueva PC de la Oficina (Windows)

Para que tus commits desde la oficina también salgan con la insignia verde **Verified**:

### Paso A: Abrir Git Bash en la oficina y generar la llave (2 minutos)
```bash
gpg --full-generate-key
```
1. Tipo: Presiona `Enter` (RSA 1).
2. Tamaño: `4096`.
3. Validez: `0` (no caduca) y confirma con `y`.
4. Nombre: `Gustavo Elias Murad`
5. Email: `gustavoeliasm@gmail.com`
6. Comentario: `llave oficina`
7. Escribe `O` (de Okay) y asigna una contraseña que recuerdes.

### Paso B: Copiar el ID y configurar Git
Ejecuta:
```bash
gpg --list-secret-keys --keyid-format=long
```
Verás una línea como:
`sec   rsa4096/A1B2C3D4E5F6G7H8 ...`

Copia ese ID (`A1B2C3D4E5F6G7H8`) y corre:
```bash
git config --global user.signingkey <TU_ID_DE_OFICINA>
git config --global commit.gpgsign true
git config --global gpg.program "C:\Program Files\Git\usr\bin\gpg.exe"
git config --global user.email "gustavoeliasm@gmail.com"
```

### Paso C: Exportar y pegar la llave en GitHub y GitLab
Exporta la llave pública:
```bash
gpg --armor --export <TU_ID_DE_OFICINA>
```
Copia el bloque completo `-----BEGIN PGP PUBLIC KEY BLOCK----- ... -----END PGP PUBLIC KEY BLOCK-----` y pégalo en:
* **GitHub:** https://github.com/settings/keys (Botón verde **New GPG key**, título: `PC Oficina`).
* **GitLab:** https://gitlab.com/-/user_settings/gpg_keys (Botón **Add key**).

---

## 3. Nuevas Funcionalidades Implementadas

* **🚨 Modo Pánico (Lockdown):**
  * Botón en el header del panel `/admin` y en el visor de tareas `/admin/tareas/visor`.
  * Si detectas alguna anomalía o ataque en la LAN, lo activas con un clic y una justificación.
  * Bloquea inmediatamente a cualquier usuario no administrador (`423 Locked`).
* **🛡️ Auditoría de Fuerza Bruta:**
  * Ante 5 intentos fallidos en menos de 5 minutos desde una IP, el sistema bloquea y registra una alerta inmutable en la tabla de auditoría (`BLOQUEO_BRUTE_FORCE`).
* **⚡ Optimización Active Directory:**
  * Cache automático de 4 horas para fueros y 1 hora para usuarios.
  * Botón manual de sincronización en el visor y panel general para cuando quieras refrescar al instante.

