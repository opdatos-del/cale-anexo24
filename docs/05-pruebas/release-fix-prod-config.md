# RELEASE_FIX_PROD_CONFIG_FINAL_GATE

## Alcance

Validación determinista de la configuración del perfil `prod`, sin ejecutar
`bootRun` para los smokes de runtime. Los smokes usaron directamente el boot
JAR generado por Gradle y un launcher efímero con environment aislado.

## Estado antes y después

### BEFORE

El perfil `prod` estaba incompleto: los dos datasources, la configuración JWT
y el origen permitido de CORS no tenían un contrato productivo completo y el
fallo por configuración incompleta no estaba validado de extremo a extremo.

### AFTER

- Primary datasource config: **PASS**.
- App datasource config: **PASS**.
- JWT config: **PASS**; secreto obligatorio y longitud mínima validada.
- CORS config: **PASS**; variable obligatoria documentada y enlazada.
- Hikari: **PASS**; cada datasource recibe su propio bloque de propiedades.
- Positive prod JAR startup: **PASS**.
- Positive `/actuator/health`: **HTTP 200**.
- Negative sin `APP_DB_URL`: **FAIL EXPECTED**; el proceso termina con
  causa `APP_DATASOURCE_URL_MISSING`.

## Evidencia del gate final

| Escenario | Profile | APP_DB_URL | Startup | Resultado |
|---|---|---|---|---|
| Positive JAR | `prod` por CLI | presente | PASS | health HTTP 200 |
| Negative JAR | `prod` por CLI | ausente | FAIL | exit 1, app datasource URL requerida |

En el escenario negativo se eliminó explícitamente `APP_DB_URL` del environment
del proceso hijo. No se usó `SPRING_PROFILES_ACTIVE` para activar el perfil y no
se usó `bootRun`.

## Regresión backend

- `./gradlew clean test`: **408/408 PASS**, 0 fallos, 0 omitidos.
- `./gradlew build`: **PASS**.
- El contrato focal de configuración cubre ambos datasources, los pools Hikari,
  JWT y el fallo por URL del app datasource ausente.

## Variables obligatorias de producción

Sin valores por defecto inseguros, deben documentarse y proporcionarse por el
mecanismo de secretos del ambiente:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- `APP_DB_URL`, `APP_DB_USERNAME`, `APP_DB_PASSWORD`.
- `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`.
- `APP_CORS_ALLOWED_ORIGINS`.

No se incluyen secretos, credenciales, tokens ni URLs de conexión en esta
bitácora.

## Estado de release

- `RELEASE-PROD-001`: **FIXED**.
- `INTERNAL_BLOCKERS`: **0**.
- `SCOPE_DECISION_REQUIRED`: **YES** — saldos, confirmación de facturación y
  Dashboard V1.
- `RELEASE_PROD_CONFIG_READY_TO_COMMIT`: **YES**, sujeto a revisión humana.

Esta validación no realiza commit ni push.
