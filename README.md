# API de Gestión de Pólizas

API REST para la gestión de pólizas de arrendamiento **individuales** y **colectivas**, sus riesgos, renovaciones y cancelaciones, con integración (mock) hacia el CORE transaccional.

Desarrollado por **Cristian Santiago Angulo Pedreros** como parte de la Prueba Técnica para Desarrollador TI.

## Tecnologías

- Java 17
- Spring Boot 3.3 (Web, Data JPA, Validation)
- H2 (base de datos en memoria)
- JUnit 5, Mockito y MockMvc para pruebas

## Arquitectura del proyecto

Arquitectura por capas, con la lógica de negocio dentro del dominio y la integración con el CORE detrás de una interfaz (puerto), para poder cambiar el mecanismo de integración sin tocar la lógica:

```
src/main/java/com/cristianangulo/polizas
├── controller/     Endpoints REST (PolizaController, RiesgoController, CoreMockController)
├── service/        Casos de uso y reglas de negocio (PolizaService)
├── repository/     Acceso a datos con Spring Data JPA
├── domain/         Entidades (Poliza, Riesgo) y enums de tipo y estado
├── dto/            Objetos de entrada y salida de la API
├── integration/    Puerto CoreClient y adaptador REST hacia el CORE
├── exception/      Excepciones de negocio y manejo global de errores
└── config/         Filtro de API key y datos de ejemplo
```

## Reglas de negocio implementadas

- Una póliza individual solo puede tener 1 riesgo: solo las pólizas colectivas admiten agregar riesgos.
- No se puede renovar una póliza cancelada.
- La cancelación de una póliza cancela todos sus riesgos.
- Agregar un riesgo valida el tipo de póliza y que no esté cancelada.
- La renovación incrementa el canon según el IPC configurado, recalcula la prima (canon x meses de vigencia), extiende la vigencia por el mismo periodo y cambia el estado a `RENOVADA`.
- Toda operación que modifica el estado de una póliza o un riesgo envía el evento `ACTUALIZACION` al CORE (mock).

## Cómo ejecutar

Requisitos: JDK 17 o superior y Maven 3.9 o superior (o abrir el proyecto en IntelliJ IDEA / VS Code).

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`. Al iniciar se cargan 3 pólizas de ejemplo:

| id | Número   | Tipo       | Riesgos |
|----|----------|------------|---------|
| 1  | IND-0001 | INDIVIDUAL | 1       |
| 2  | COL-0001 | COLECTIVA  | 2       |
| 3  | IND-0002 | INDIVIDUAL | 0       |

Configuración en `application.yml` (se puede sobrescribir con variables de entorno):

| Propiedad | Variable | Valor por defecto |
|-----------|----------|-------------------|
| `app.security.api-key` | `API_KEY` | `123456` |
| `app.negocio.ipc` | `IPC` | `0.0525` (5,25 %) |
| `app.core.url` | `CORE_URL` | `http://localhost:8080/core-mock/evento` |

## Ejecutar las pruebas

```bash
mvn test
```

Incluye pruebas unitarias de las reglas de negocio (`PolizaServiceTest`) y pruebas de integración de los endpoints con MockMvc (`PolizaControllerIT`).

## Seguridad

Todas las peticiones requieren el header:

```
x-api-key: 123456
```

Sin el header, o con un valor incorrecto, la API responde `401 Unauthorized`.

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/polizas?tipo=COLECTIVA&estado=ACTIVA` | Lista pólizas; `tipo` y `estado` son opcionales |
| GET | `/polizas/{id}/riesgos` | Lista los riesgos de una póliza |
| POST | `/polizas/{id}/renovar` | Renueva la póliza ajustando canon y prima por IPC |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo (solo pólizas colectivas) |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo |
| POST | `/core-mock/evento` | Mock del CORE: registra en logs el evento recibido |

Valores válidos: `tipo` = `INDIVIDUAL` o `COLECTIVA`; `estado` = `ACTIVA`, `RENOVADA` o `CANCELADA`.

### Ejemplos con curl

```bash
# Listar pólizas colectivas activas
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA"

# Riesgos de la póliza 2
curl -H "x-api-key: 123456" http://localhost:8080/polizas/2/riesgos

# Renovar la póliza 1
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/1/renovar

# Agregar un riesgo a la póliza colectiva 2
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
     -d '{"direccionInmueble":"Carrera 7 # 80-10, Apto 303","arrendatario":"Pedro Gil"}' \
     http://localhost:8080/polizas/2/riesgos

# Intentar agregar un riesgo a la póliza individual 1 (responde 422)
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
     -d '{"direccionInmueble":"Calle 1","arrendatario":"Ana"}' \
     http://localhost:8080/polizas/1/riesgos

# Cancelar un riesgo
curl -X POST -H "x-api-key: 123456" http://localhost:8080/riesgos/2/cancelar

# Cancelar la póliza 2 (cancela todos sus riesgos)
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/2/cancelar

# Mock del CORE
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
     -d '{"evento":"ACTUALIZACION","polizaId":555}' \
     http://localhost:8080/core-mock/evento
```

## Manejo de errores

| Código | Caso |
|--------|------|
| 400 | Datos inválidos, JSON mal formado o parámetro con valor no permitido |
| 401 | Header `x-api-key` ausente o incorrecto |
| 404 | La póliza o el riesgo no existe |
| 422 | Violación de una regla de negocio |

Todas las respuestas de error tienen el mismo formato: `status`, `error`, `mensaje`, `path` y `timestamp`.

## Decisiones técnicas

- **Reglas de negocio en el dominio:** la renovación, la cancelación y el cálculo de la prima viven en la entidad `Poliza`, de modo que la regla está en un solo lugar.
- **Puerto hacia el CORE:** el servicio depende de la interfaz `CoreClient` y no de una implementación concreta. Hoy apunta al mock; en producción se reemplaza por el cliente del servicio de edición en WebLogic, o por una cola de mensajes, sin cambiar la lógica.
- **Fallo del CORE sin bloquear la operación:** si el CORE no responde, el error se registra en logs. En un entorno real se agregarían reintentos o un patrón Outbox para garantizar la entrega.
- **Valores monetarios con `BigDecimal`:** evita errores de redondeo en canon y prima.
- **IPC configurable:** el porcentaje de incremento se ajusta por configuración, sin cambiar el código.

## Nota sobre el uso de IA

Este proyecto se desarrolló con apoyo de herramientas de IA (Claude Code) como asistente de codificación. El diseño, las decisiones técnicas, la revisión del código y la validación de las reglas de negocio estuvieron a cargo del autor.
