# redeban-api-trns-bonos

Servicio Quarkus + Camel que expone los procesos de creación, activación y consulta de bonos siguiendo una arquitectura hexagonal. Las capas se organizan en `application`, `domain` e `infrastructure` y desacoplan los puertos REST de las integraciones con AS/400.

## Estructura principal

- `domain`: modelos, excepciones y puertos (`port/in`, `port/out`).
- `application`: casos de uso (`GestionBonosUseCase`).
- `infrastructure`
  - `in/rest`: controlador REST, DTOs y mapeos.
  - `camel/outbound`: rutas `direct:*` que delegan en el cliente AS/400.
  - `out/jdbc`: conector JDBC que arma y ejecuta el stored procedure.
  - `logging`: utilidades para trazabilidad de request/response en JSON.
- `deploy/base`: manifiestos parametrizados para OpenShift (Deployment, Service, Route, ConfigMap, Secret, HPA).

## Configuración

Los parámetros funcionales y técnicos no se codifican en fuentes. Se leen desde `application.properties` mediante variables de entorno:

| Variable | Descripción |
| --- | --- |
| `BONOS_AS400_SP` | Nombre del stored procedure (`AUBONB1(?,?)` por defecto). |
| `BONOS_DATASOURCE_URL` | URL JDBC AS/400 (por ejemplo `jdbc:as400://host;libraries=AUPGMSV5R;prompt=false;`). |
| `BONOS_DATASOURCE_USER` / `BONOS_DATASOURCE_PASSWORD` | Credenciales AS/400 (inyectadas como Secret). |
| `BONOS_DOCUMENTO_*`, `BONOS_CODIGO_*`, `BONOS_DESCRIPCION_*` | Catálogos de negocio. Todos tienen valores por defecto y pueden ajustarse vía ConfigMap. |
| `APP_CONTEXT` | Contexto raíz HTTP. Se toma de `scripts/parametros.conf` al desplegar. |

El conector verifica que `BONOS_DATASOURCE_URL`, `BONOS_DATASOURCE_USER` y `BONOS_DATASOURCE_PASSWORD` estén presentes antes de invocar el AS/400. Si faltan, la aplicación inicia y responde con código de error técnico (`E09`) indicando que debe completarse la configuración.

### Logging

El controlador registra por operación el request, los headers opcionales (`idTransaccion`, `nombreAplicacion`, `ipAplicacion`, `timestamp`), la respuesta y el tiempo de ejecución en milisegundos en formato JSON.

## Despliegue en OpenShift

### Flujos de desa/certi

1. Ajusta `scripts/parametros.conf` con `APP_NAME`, `APP_CONTEXT`, `NAMESPACE`, `REGISTRY`, `VERSION` y `ROUTE_HOST`.
2. Ejecuta los scripts (PowerShell):
   - `./scripts/compile.ps1` - limpia y construye el *fast-jar* sin tests.
   - `./scripts/build-push.ps1` - construye la imagen con el Dockerfile y la publica en `__REGISTRY__`.
   - `./scripts/deploy.ps1` - renderiza los manifiestos de `deploy/{ENVIRONMENT}` (vía `Render-Manifests`), aplica ConfigMap/Secret y realiza `oc apply`.
   - `./scripts/undeploy.ps1` - elimina los recursos asociados al `APP_NAME`.

Los manifiestos renderizados quedan en `target/ocp`. Recuerda completar el Secret `__APP_NAME__-secret` con las credenciales codificadas en Base64 antes de desplegar.

### Flujo de producción (sin compilación ni build)

Producción reutiliza la imagen aprobada en certificación; por lo tanto el equipo operativo solo ejecuta promoción y despliegue:

1. Ajusta `scripts/parametros-promote.conf` (accesos a los clusters) y `scripts/parametros-prod.conf` (namespace `redeban-transversal-prod`, `ENVIRONMENT=prod`, host, etc.).
2. Promueve la imagen de certificación hacia el registro de producción:
   ```powershell
   ./scripts/promote-prod.ps1 -Action promote
   ```
3. Despliega los manifiestos de producción (sin recompilar ni reconstruir la imagen):
   ```powershell
   ./scripts/promote-prod.ps1 -Action deploy
   ```
4. Para retirar los recursos productivos:
   ```powershell
   ./scripts/promote-prod.ps1 -Action undeploy
   ```
5. Si se desea hacer promoción y despliegue consecutivo en un solo paso: `./scripts/promote-prod.ps1 -Action promoteAndDeploy`.

Los scripts `deploy.ps1` y `undeploy.ps1` aceptan ahora el parámetro `-ParamsFile`, por lo que también puedes invocarlos directamente con `scripts/parametros-prod.conf` si necesitas ejecutar sólo esas tareas (`./scripts/deploy.ps1 -ParamsFile scripts/parametros-prod.conf`).

## Endpoints

- `POST /servicios/bonos/gestionBonos/crearBono`
- `PUT /servicios/bonos/gestionBonos/activarBono`
- `POST /servicios/bonos/gestionBonos/recuperarBono`
- Salud: `/q/health`, `/q/health/live`, `/q/health/ready`
- OpenAPI: `/openapi`

## Consideraciones adicionales

- No se incluyen pruebas automatizadas por solicitud explícita.
- El Dockerfile, scripts y `scripts/parametros.conf` se mantienen sin cambios de formato.
- Todos los valores de negocio se parametrizan; no hay hardcodeos en las clases.



- BASE_IMAGE: Imagen base para la etapa runtime.

- Requiere docker buildx instalado para montar el repositorio Maven local.

- Requiere docker buildx instalado para montar el repositorio Maven local.

