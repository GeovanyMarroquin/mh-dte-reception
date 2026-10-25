# Mock de validación de DTE

> **Este proyecto es exclusivamente un servicio mock para desarrollo y pruebas.** Su propósito es validar documentos
> JSON contra los JSON Schemas oficiales incluidos en el repositorio. No es un servicio de Hacienda, no transmite DTE
> y no debe utilizarse para operar en producción.

Servicio REST open source desarrollado con Java 25 y Spring Boot 4.1.1 que simula la interfaz de autenticación y
recepción de Documentos Tributarios Electrónicos (DTE) de El Salvador. Permite probar integraciones localmente y
detectar errores estructurales contra los schemas, sin conectarse a los servicios reales de Hacienda.

El servicio desempaqueta el payload JWS y lo valida contra los JSON Schema incluidos en `src/main/resources/schemas/`.
La firma criptográfica no se verifica: únicamente se decodifica el payload y se valida el JSON resultante. Los schemas
incluidos son la referencia de validación de este mock.

## Alcance y limitaciones

Este repositorio existe para facilitar pruebas de integración y validación local de payloads. La API imita la forma de
los endpoints esperados, pero sus respuestas son simuladas.

- Valida la estructura del JSON contra los schemas oficiales incluidos en el proyecto.
- Desempaqueta el payload de un JWS para obtener el JSON que se valida.
- Simula autenticación, recepción, anulación y consulta usando datos locales.
- No autentica credenciales ni verifica firmas criptográficas.
- No envía, recibe ni registra documentos ante Hacienda.
- No implementa reglas de negocio adicionales fuera de los schemas incluidos.
- No sustituye los ambientes oficiales ni garantiza la aceptación de un DTE real.

Las contribuciones deben preservar este alcance: cualquier cambio debe mantener explícito que se trata de un mock de
validación de JSON Schemas y no de una implementación del servicio oficial.

## Requisitos

- Java 25 para ejecutarlo localmente.
- Docker y Docker Compose para ejecutarlo en contenedor.

## Ejecución

### Gradle

En macOS o Linux:

```bash
./gradlew bootRun
```

En Windows:

```bat
gradlew.bat bootRun
```

El servicio escucha en `http://localhost:8085` por defecto. El puerto se puede cambiar mediante la variable de entorno
`SERVER_PORT`.

### Docker Compose

```bash
docker compose up --build -d
```

Compose publica el servicio en el puerto `8085`.

## API

### Autenticación simulada

`POST /seguridad/auth` recibe un formulario `application/x-www-form-urlencoded` con los campos `user` y `pwd`. Ambos
deben contener un valor no vacío. El servicio devuelve un token fijo de simulación; no autentica credenciales ni valida
el contenido del token en los endpoints de recepción y consulta.

```bash
curl -X POST http://localhost:8085/seguridad/auth \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'user=usuario' \
  --data-urlencode 'pwd=clave'
```

Respuesta `200 OK`:

```json
{
  "status": "OK",
  "body": {
    "token": "mock-token-eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkiLCJuYW1lIjoiTW9jayBVc2VyIiwiaWF0IjoxNTE2MjM5MDIyfQ",
    "roles": [
      "ROLE_USER"
    ],
    "tokenType": "Bearer"
  }
}
```

Si `user` o `pwd` están vacíos, devuelve `400 Bad Request`.

### Recepción de DTE

`POST /fesv/recepciondte` recibe un objeto JSON con la estructura del sobre de recepción. `documento` debe ser un JWS
compacto de tres segmentos, cuyo payload sea el DTE codificado en Base64URL (o Base64). `tipoDte` se usa como
alternativa si el documento no incluye `identificacion.tipoDte`.

```json
{
  "ambiente": "00",
  "idEnvio": 1,
  "version": 1,
  "tipoDte": "01",
  "documento": "<header-base64url>.<payload-dte-base64url>.<firma>"
}
```

Ejemplo de llamada:

```bash
curl -X POST http://localhost:8085/fesv/recepciondte \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer mock-token' \
  --data @solicitud.json
```

El header `Authorization` se exige únicamente con el formato `Bearer <valor>` para imitar la interfaz; el mock no
autentica el token. Tampoco comprueba la firma JWS: solo decodifica el payload y valida el JSON resultante.

#### Resultado procesado

Si el DTE cumple el schema, responde `200 OK` con `estado: PROCESADO`, `codigoMsg: 001` y `descripcionMsg: RECIBIDO`. Se
conserva `identificacion.codigoGeneracion` cuando está disponible; de lo contrario, se genera un UUID.

#### Resultado rechazado

Si no hay schema configurado, el documento no cumple el schema o el payload no se puede decodificar, responde
`400 Bad Request`. La respuesta incluye `estado: RECHAZADO`, `codigoMsg: ERROR_CODIGO` y las observaciones encontradas.

### Anulación de DTE

`POST /fesv/anulardte` recibe un objeto JSON con la estructura del sobre de anulación. `documento` debe ser un JWS
compacto de tres segmentos, cuyo payload sea el evento de invalidación codificado en Base64URL (o Base64) y cumpla
`v3/invalidacion-schema-v3.json` (incluido `identificacion.version: 3`). A diferencia de la recepción, el sobre no
incluye `tipoDte`.

```json
{
  "ambiente": "00",
  "idEnvio": 1,
  "version": 3,
  "documento": "<header-base64url>.<payload-invalidacion-base64url>.<firma>"
}
```

Ejemplo de llamada:

```bash
curl -X POST http://localhost:8085/fesv/anulardte \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer mock-token' \
  --data @anulacion.json
```

El header `Authorization` se exige únicamente con el formato `Bearer <valor>` para imitar la interfaz; el mock no
autentica el token. Tampoco comprueba la firma JWS: solo decodifica el payload y valida el JSON resultante.

#### Resultado procesado

Si el evento cumple el schema, responde `200 OK` con `estado: PROCESADO`, `codigoMsg: 001` y
`descripcionMsg: Invalidación Recibida y Procesada`. La respuesta sigue el contrato de anulación (`version: 2`,
`versionApp: 1`, sin `clasificaMsg`) y `observaciones` es `null`.

#### Resultado rechazado

Si el evento no cumple el schema o el payload no se puede decodificar, responde `400 Bad Request` con
`estado: RECHAZADO`, `codigoMsg: ERROR_CODIGO`, `selloRecibido: null`, `fhProcesamiento: null` y las observaciones
encontradas. El endpoint es stateless: valida y simula la respuesta sin persistir ni modificar el estado del DTE
original.

## Schemas soportados

Los 15 schemas se cargan al iniciar el servicio y se mantienen en memoria. Los tipos de documento se resuelven así:

| `tipoDte` | Schema              |
|-----------|---------------------|
| `01`      | `v2/fe-f-v2.json`   |
| `03`      | `v4/fe-ccf-v4.json` |
| `04`      | `v4/fe-nr-v4.json`  |
| `05`      | `v4/fe-nc-v4.json`  |
| `06`      | `v4/fe-nd-v4.json`  |
| `07`      | `v2/fe-cr-v2.json`  |
| `08`      | `v2/fe-dcl-v2.json` |
| `09`      | `v2/fe-cl-v2.json`  |
| `11`      | `v3/fe-fex-v3.json` |
| `14`      | `v2/fe-fse-v2.json` |
| `15`      | `v2/fe-cd-v2.json`  |

Los eventos se identifican por el contenido de `tipoEvento` o `evento` en el payload, y tienen prioridad sobre `tipoDte`:

| Contexto de evento | Schema                           |
|--------------------|----------------------------------|
| `operaciones`      | `v1/fe-eop-v1.json`              |
| `retorno`          | `v1/fe-eret-v1.json`             |
| `invalidacion`     | `v3/invalidacion-schema-v3.json` |
| `contingencia`     | `v4/contingencia-schema-v4.json` |

## Estructura del proyecto

```text
src/main/java/com/digital/reception/
├── controller/DteController.java
├── dto/                       # Objetos de solicitud y respuesta
├── exception/GlobalExceptionHandler.java
├── service/DteSchemaValidatorService.java
└── util/JwsDecoderUtil.java
src/main/resources/schemas/    # JSON Schema por versión
```

## Pruebas

```bash
./gradlew test
```

## Licencia

Este proyecto se distribuye como open source bajo la [licencia MIT](LICENSE). La licencia permite usar, modificar y
redistribuir el mock, siempre respetando las limitaciones funcionales descritas en este documento.
