# ADR-009: Metodologia ITAM/CMDB de taller

## Estado

Aceptada.

## Contexto

Inventario Modular ya no es solo un listado de equipos. El sistema registra equipos,
componentes, stock, gemelos digitales, diferencias, tareas tecnicas, APK movil, actas y
auditoria. Ese conjunto se parece mas a una plataforma de operacion del taller de
informatica que a un inventario patrimonial simple.

En el mercado existen enfoques consolidados:

- ITAM: gestion del ciclo de vida de activos de tecnologia.
- CMDB: registro de configuraciones y relaciones entre elementos tecnicos.
- ITSM: gestion de tareas, incidentes, cambios y servicios.
- Discovery/inventory agents: deteccion automatica de hardware y software.

Inventario Modular debe tomar esas ideas, pero adaptarlas al contexto real del Centro
Judicial San Pedro: red LAN, taller fisico, tecnicos con APK, MySQL local/productivo,
comparacion con scripts de inventario y decisiones diarias del administrador.

## Decision

Inventario Modular adopta una metodologia **ITAM/CMDB de taller**.

Esto significa que:

1. `Equipo` es el activo ITAM principal.
2. Los componentes esperados/administrativos son la configuracion autorizada.
3. Lo reportado por script es la configuracion detectada.
4. El gemelo digital es la vista CMDB viva del equipo.
5. Las diferencias son desviaciones que requieren decision o trabajo tecnico.
6. `StockComponente` representa repuestos e insumos disponibles para intervenir activos.
7. `TareaTecnica` representa trabajo operativo sobre activos, stock o usuarios.
8. `Acta`, movimientos y auditoria son evidencia institucional.
9. El panel general debe priorizar decision operativa, no decoracion ni navegacion redundante.

La terminologia visible para el usuario puede seguir siendo simple: equipos, componentes,
stock, tareas, fichas y diferencias. Internamente, la arquitectura debe respetar el mapa
ITAM/CMDB para crecer sin perder trazabilidad.

## Mapa conceptual

| Concepto ITAM/CMDB | Nombre operativo en Inventario Modular | Implementacion actual |
|---|---|---|
| Activo | Equipo | `Equipo`, `EquipoService`, `/admin/equipos` |
| CI principal | Equipo inventariado | `equipos` |
| Atributos de activo | Usuario, fuero, ubicacion, sistema operativo | Campos de `Equipo` |
| Configuracion esperada | Componentes administrativos/cargados | `Componente` con origen manual/stock |
| Configuracion detectada | Reporte del script | `EquipoService.registrarInventario`, componentes detectados |
| Gemelo digital | Estado tecnico consolidado del equipo | `GemeloDigitalService` |
| Desviacion | Diferencia entre esperado y detectado | Dashboard de diferencias |
| Repuesto/insumo | Stock de componentes | `StockComponente`, `StockService` |
| Trabajo tecnico | Tarea tecnica | `TareaTecnica`, APK, visor publico |
| Evidencia | Acta, auditoria, comentario, stock usado | `Acta`, `AuditoriaService`, `tareas_stock_uso` |
| Ciclo de vida | Alta, uso, mantenimiento, baja | Flujos de equipos, tareas, actas y auditoria |

## Reglas de diseno

- No crear una CMDB generica abstracta antes de necesitarla.
- No cambiar nombres de pantalla a terminos academicos si confunden al taller.
- Cada nuevo dato importante debe responder una pregunta operativa:
  - que activo es;
  - donde esta;
  - que tiene;
  - que deberia tener;
  - que cambio;
  - quien lo toco;
  - que tarea o evidencia lo respalda.
- Las diferencias del gemelo no son solo informacion: deben poder convertirse en trabajo,
  decision o cierre.
- El stock no es un modulo aislado: es capacidad de reparacion del taller.
- Las tareas no son un modulo aislado: son el puente entre decision administrativa y accion
  tecnica.
- El historial debe crecer por eventos y evidencia, no por campos sueltos imposibles de
  auditar.

## Consecuencias

Positivas:

- El sistema queda preparado para crecer hacia mesa de ayuda, cambios, mantenimiento y
  reportes sin rehacer el nucleo.
- El administrador puede tomar decisiones con datos vivos: diferencias, stock, tareas y
  estado de activos.
- La APK sigue teniendo sentido como herramienta de campo dentro del modelo.
- El gemelo digital pasa a ser una pieza central de CMDB, no una pantalla secundaria.

Costos:

- Hay que cuidar mas la trazabilidad de cada cambio.
- Las futuras migraciones deben distinguir datos de activo, configuracion, trabajo y
  evidencia.
- Algunas pantallas deberan mostrar menos campos crudos y mas estados utiles.

## Proximos pasos recomendados

1. Documentar estados de ciclo de vida del equipo.
2. Convertir diferencias relevantes del gemelo en tareas tecnicas sugeridas.
3. Agregar una vista de ficha ITAM/CMDB por equipo: activo, configuracion, diferencias,
   tareas, stock usado y evidencia.
4. Agregar reportes de decision: equipos con diferencias abiertas, equipos sin reporte
   reciente, stock critico, tareas vencidas, piezas usadas por periodo.
5. Definir baja/retiro de activo con acta y evidencia.

