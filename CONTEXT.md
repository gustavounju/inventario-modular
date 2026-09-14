# Contexto Vivo - ServidorInventario

Ultima actualizacion: 2026-09-14.

## Proyecto

Inventario Modular es la nueva aplicacion Java/Spring para el inventario interno del
Departamento de Informatica del Centro Judicial San Pedro. Convive con el Flask legado,
pero no comparte codigo ni despliegue.

## Topologia

- Produccion modular: aplicacion en Ubuntu `APP_INTERNA_IP`, servicio
  `inventario-modular.service`, puerto `8081`.
- Base de produccion modular: MySQL `MYSQL_INTERNO_IP`, base `inventario_modular`, usuario de
  aplicacion `inventario_modular_app`.
- Desarrollo Windows: MySQL local `127.0.0.1:3306`, base `inventario_modular`, usuario
  `inventario_local`.
- Perfil `local`: MySQL local por defecto. En produccion se sobreescribe con variables de
  entorno del servidor.
- Perfil `casa`: H2 solo para laboratorio aislado; no usar para validar celulares, AD ni
  flujo operativo real.

## Estado Funcional

- Login local y Active Directory integrados con autorizacion propia del sistema.
- Modulos principales: usuarios/permisos, equipos, componentes, stock, auditoria, actas,
  ubicaciones, muebles, patrimonio, reportes y tareas tecnicas.
- Modulo Tareas LAN:
  - Visor independiente `/admin/tareas/visor`.
  - Web movil `/movil/login` y `/movil/tareas`.
  - API movil protegida para sesion, avisos, APK y busqueda de usuarios AD.
  - APK Android piloto servida desde `INVENTARIO_MOVIL_APK_PATH`.
  - Comentarios visibles en el listado movil mediante preview asincronico.
  - Los solicitantes de tareas se validan contra AD cuando LDAP esta habilitado.
    Si LDAP esta deshabilitado, se permite carga manual solo para laboratorio/local.

## Arranque Local Windows En El Trabajo

Preparar MySQL local:

```powershell
.\scripts\setup-local-mysql.ps1
```

Arrancar Spring Boot con MySQL local y AD:

```powershell
.\scripts\start-local-ad.ps1
```

El script pide credenciales en consola. No escribir claves en commits, documentos ni chat.

## Seguridad

- No versionar secretos reales.
- No ejecutar comandos contra MySQL `MYSQL_INTERNO_IP` ni deploys de produccion sin autorizacion
  explicita.
- Active Directory autentica identidad; Inventario Modular decide autorizacion y roles.
- Cuando LDAP esta activo, Inventario Modular tambien valida que el usuario solicitante
  exista en AD antes de crear o editar una tarea.
- Las cuentas locales de emergencia/desarrollo se controlan con `inventario.local-auth.*`
  e `inventario.local-db-auth.*`.

## Documentacion Relacionada

- `docs/inventario-modular/README.md`
- `docs/inventario-modular/bitacora-del-proyecto.md`
- `docs/inventario-modular/base-datos-local-windows.md`
- `docs/inventario-modular/tareas-moviles-lan.md`
- `docs/inventario-modular/instalacion-tareas-lan-2026-09-10.md`
