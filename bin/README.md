# MediSalud — Sistema de Agendamiento de Citas Médicas

API REST para la gestión de citas médicas de la clínica MediSalud.

## Arquitectura

Este proyecto usa **Arquitectura Hexagonal** (Ports & Adapters):

```
com.medisalud.citas
├── domain/                         ← Núcleo de negocio (sin dependencias externas)
│   ├── model/                      ← Entidades de dominio puras
│   │   ├── Medico.java
│   │   ├── Paciente.java
│   │   ├── Cita.java
│   │   └── EstadoCita.java (enum)
│   ├── port/
│   │   ├── in/                     ← Puertos de entrada (use cases / interfaces)
│   │   │   ├── MedicoUseCase.java
│   │   │   ├── PacienteUseCase.java
│   │   │   └── CitaUseCase.java
│   │   └── out/                    ← Puertos de salida (contratos de persistencia)
│   │       ├── MedicoRepositoryPort.java
│   │       ├── PacienteRepositoryPort.java
│   │       └── CitaRepositoryPort.java
│   └── service/                    ← Implementaciones de los use cases
│       ├── MedicoService.java
│       ├── PacienteService.java
│       └── CitaService.java
└── infrastructure/                 ← Adaptadores externos (Spring, JPA, HTTP)
    └── adapter/
        ├── in/web/                 ← Adaptadores de ENTRADA (HTTP)
        │   ├── controller/         ← Controladores REST
        │   ├── dto/request/        ← DTOs de entrada
        │   ├── dto/response/       ← DTOs de salida
        │   ├── mapper/             ← MapStruct (DTO ↔ Dominio)
        │   └── exception/          ← Manejador global de excepciones
        └── out/persistence/        ← Adaptadores de SALIDA (JPA / PostgreSQL)
            ├── entity/             ← Entidades JPA
            ├── repository/         ← Spring Data JPA Repositories
            ├── mapper/             ← MapStruct (Entity ↔ Dominio)
            └── *RepositoryAdapter  ← Implementan los puertos de salida
```

## Tecnologías

- **Java 11** / **Spring Boot 3.1.x**
- **PostgreSQL** — base de datos relacional
- **Spring Data JPA / Hibernate** — persistencia
- **MapStruct** — mapeo entre capas (sin código manual)
- **Lombok** — reducción de boilerplate
- **Bean Validation (@Valid)** + validación manual en DTOs
- **Spring Boot Actuator** — health checks

## Configurar y Ejecutar

### 1. Pre-requisitos
- Java 11+
- Maven 3.8+
- PostgreSQL 13+

### 2. Crear la base de datos
```sql
CREATE DATABASE medisalud_citas;
```

### 3. Configurar credenciales
Edita `src/main/resources/application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/medisalud_citas
    username: postgres
    password: postgres
```

### 4. Ejecutar
```bash
mvn spring-boot:run
```

La API quedará disponible en: `http://localhost:8080/api/v1`

## Endpoints de la API

### Médicos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/medicos` | Registrar médico |
| `GET` | `/api/v1/medicos` | Listar todos los médicos |
| `GET` | `/api/v1/medicos/{id}` | Obtener médico por ID |
| `PUT` | `/api/v1/medicos/{id}` | Actualizar médico |
| `DELETE` | `/api/v1/medicos/{id}` | Eliminar médico |

### Pacientes

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/pacientes` | Registrar paciente |
| `GET` | `/api/v1/pacientes` | Listar todos los pacientes |
| `GET` | `/api/v1/pacientes/{id}` | Obtener paciente por ID |
| `PUT` | `/api/v1/pacientes/{id}` | Actualizar paciente |
| `DELETE` | `/api/v1/pacientes/{id}` | Eliminar paciente |

### Citas

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/citas` | Reservar cita |
| `GET` | `/api/v1/citas` | Listar citas (con filtros) |
| `GET` | `/api/v1/citas/{id}` | Obtener cita por ID |
| `PATCH` | `/api/v1/citas/{id}/cancelar` | Cancelar cita |
| `PUT` | `/api/v1/citas/{id}/reprogramar` | Reprogramar cita |
| `DELETE` | `/api/v1/citas/{id}` | Eliminar cita |

#### Filtros disponibles para GET /api/v1/citas:
```
?medicoId=1&pacienteId=2&estado=PROGRAMADA&fechaInicio=2026-07-01T00:00:00&fechaFin=2026-07-31T23:59:59
```

## Ejemplos de Request/Response

### Registrar médico
```json
POST /api/v1/medicos
{
  "nombreCompleto": "Dra. María González",
  "especialidad": "Cardiología",
  "telefono": "555-1001",
  "email": "maria.gonzalez@medisalud.com"
}

Response 201:
{
  "success": true,
  "message": "Médico registrado exitosamente",
  "data": {
    "id": 1,
    "nombreCompleto": "Dra. María González",
    "especialidad": "Cardiología",
    "telefono": "555-1001",
    "email": "maria.gonzalez@medisalud.com"
  },
  "timestamp": "2026-07-09T11:30:00"
}
```

### Reservar cita
```json
POST /api/v1/citas
{
  "pacienteId": 1,
  "medicoId": 1,
  "fechaHora": "2026-07-15T09:00:00"
}

Response 201:
{
  "success": true,
  "message": "Cita reservada exitosamente",
  "data": {
    "id": 1,
    "pacienteId": 1,
    "nombrePaciente": "Juan Pérez",
    "medicoId": 1,
    "nombreMedico": "Dra. María González",
    "especialidadMedico": "Cardiología",
    "fechaHora": "2026-07-15T09:00:00",
    "estado": "PROGRAMADA",
    "penalizado": false
  }
}
```

### Error de validación
```json
Response 400:
{
  "success": false,
  "message": "Error de validación en los datos enviados",
  "errors": [
    "Campo 'nombreCompleto': El nombre completo es obligatorio",
    "Campo 'especialidad': La especialidad es obligatoria"
  ]
}
```

## Reglas de Negocio (TODO para el desarrollador)

Las siguientes reglas de negocio están marcadas con `// TODO` en `CitaService.java`:

- **RN-01**: Franjas horarias Lun-Vie 08:00-18:00, Sáb 08:00-13:00, sin domingos/festivos
- **RN-02**: Un médico no puede tener dos citas en la misma franja de 30 min
- **RN-03**: No se aceptan fechas de nacimiento futuras en pacientes
- **RN-04**: Un paciente no puede tener dos citas con el mismo médico en la misma franja
- **RN-05**: Penalización si cancela con menos de 2 horas; 3+ penalizaciones en 30 días bloquea al paciente
- **RN-06**: Reprogramación = cancelar original + crear nueva validando disponibilidad

## Manejo de Errores HTTP

| Código | Excepción | Cuándo |
|--------|-----------|--------|
| `400` | MethodArgumentNotValidException | Validación de campos fallida |
| `400` | HttpMessageNotReadableException | JSON malformado |
| `404` | RecursoNoEncontradoException | Entidad no encontrada |
| `409` | ConflictoException | Documento/email duplicado |
| `422` | ReglaNegocioException | Violación de regla de negocio |
| `500` | Exception | Error interno no controlado |
