# Sistema de Agendamiento de Citas Médicas - MediSalud

API REST para la gestión de citas médicas de **MediSalud**. Permite registrar médicos y pacientes, agendar citas, consultar disponibilidad de horarios y gestionar cancelaciones con penalizaciones automáticas.

---

## Tecnologías Utilizadas

*   **Java 17:** Versión del lenguaje, asegurando soporte a largo plazo y características modernas.
*   **Spring Boot 3.1.12:** Framework base para el desarrollo rápido y estructurado de la API REST.
*   **Spring Data JPA e Hibernate:** Para el mapeo objeto-relacional y acceso a datos de forma segura.
*   **PostgreSQL:** Motor de base de datos relacional para garantizar la persistencia e integridad de los datos.
*   **MapStruct & Lombok:** Herramientas para reducir código repetitivo y agilizar la transformación entre modelos de dominio, entidades y DTOs.
*   **JUnit 5 & Mockito:** Para asegurar la calidad de la lógica mediante pruebas automatizadas.
*   **SpringDoc OpenAPI (Swagger):** Documentación interactiva y autogenerada de todos los endpoints.
*   **Maven:** Gestor de dependencias y construcción del ciclo de vida del proyecto.

---

## Reglas del Negocio

Estas son las reglas que el sistema valida automáticamente:

1. **Horarios de atención:** Las citas solo pueden agendarse de lunes a viernes de 8:00 a.m. a 6:00 p.m., y los sábados de 8:00 a.m. a 1:00 p.m. No se aceptan citas los domingos ni los días festivos del calendario oficial de Colombia.

2. **Sin citas dobles para el médico:** Un médico no puede tener dos citas en la misma franja horaria.

3. **Sin citas dobles para el paciente:** Un paciente no puede tener dos citas en la misma franja de tiempo.

4. **Fecha de nacimiento válida:** El paciente debe tener una fecha de nacimiento registrada que no sea en el futuro. No hay restricción de edad mínima; se aceptan citas para pacientes de cualquier edad, incluyendo recién nacidos.

5. **Penalización por cancelación tardía:** Si un paciente cancela una cita con menos de 2 horas de antelación, esa cancelación queda registrada como penalización.

6. **Bloqueo por demasiadas penalizaciones:** Si un paciente acumula 3 o más cancelaciones tardías en los últimos 30 días, el sistema le impide agendar nuevas citas hasta que pasen esos 30 días.

**REGLAS DE NEGOCIO PROPORCIONADAS POR EL DESARROLLADOR, CON EL FIN DE PROCEDER CON LAS BUENAS PRACTICAS**

7. **Sanitización de datos de entrada:** Los nombres y textos que lleguen con espacios al inicio o al final se limpian automáticamente. Las tildes también se eliminan para garantizar consistencia en la base de datos.

8. **Teléfonos solo con números y guiones:** El campo de teléfono solo acepta dígitos y guiones (por ejemplo: `555-1234` o `3001234567`).

---

## Arquitectura del Proyecto

El proyecto usa los principios de la **Arquitectura Hexagonal (Ports and Adapters)**, pero aplicada de forma pragmática.

**¿Qué significa eso?**

La lógica de negocio vive en una capa de "dominio" que no sabe cómo funciona la base de datos ni cómo llegan las peticiones HTTP. El dominio solo habla con interfaces (puertos), y la infraestructura (base de datos, controladores) es la que implementa esas interfaces.

Sin embargo, en este proyecto se tomó una decisión consciente: se usan anotaciones de Spring como `@Service` y `@Transactional` directamente dentro de la capa de dominio. En un enfoque hexagonal estricto, el dominio debería ser 100% independiente del framework. Pero llevar eso al extremo implicaría crear clases de configuración adicionales solo para decirle a Spring cómo instanciar cada servicio, lo cual añade archivos y complejidad sin que el proyecto se beneficie de ello de ninguna forma práctica. Por eso se optó por un enfoque pragmático: se aprovechan las facilidades del framework donde no hay un costo real de mantenimiento, y se mantiene la lógica del negocio completamente aislada y testeable mediante pruebas unitarias sin necesidad de levantar la aplicación completa.

**Capas del proyecto:**

- **Controladores** (`controller/`): Reciben la petición HTTP, la validan y la pasan al dominio. No tienen lógica de negocio.
- **Servicios de dominio** (`domain/service/`): Aquí viven todas las reglas del negocio. Son completamente independientes de la base de datos.
- **Adaptadores de persistencia** (`adapter/out/persistence/`): Se encargan de hablar con la base de datos usando JPA.

**Seguridad básica:**

- Cada petición usa su propio objeto de entrada (DTO), lo que evita que un usuario pueda modificar campos que no debería tocar simplemente enviando datos extra en el JSON.
- El acceso a la base de datos se hace a través de consultas preparadas (JPA), lo que protege contra inyección SQL.
- Los errores del servidor nunca exponen información interna al usuario. El manejador global de errores devuelve mensajes claros y seguros.

---

## Instrucciones para Ejecutar Localmente

### Prerrequisitos

- **Java 17** o superior instalado (con la variable de entorno `JAVA_HOME` configurada).
- **Maven** instalado.
- Una instancia de **PostgreSQL** corriendo con una base de datos creada.

### Configuración

Antes de arrancar, revisa el archivo `src/main/resources/application.yml` y ajusta los datos de conexión a tu base de datos PostgreSQL:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nombre_de_tu_bd
    username: tu_usuario
    password: tu_contraseña
```

### Ejecución

1. Abre una terminal en la raíz del proyecto.
2. Compila el proyecto:
   ```bash
   mvn clean install
   ```
3. Levanta la aplicación:
   ```bash
   mvn spring-boot:run
   ```
   La aplicación estará disponible en `http://localhost:8080/api/v1`.

### Perfiles disponibles

El proyecto tiene tres perfiles configurados:

- **`dev` (por defecto)** — `mvn spring-boot:run`
  Desarrollo local. Crea y actualiza tablas automáticamente.

- **`qa`** — `mvn spring-boot:run -Dspring.profiles.active=qa`
  Pruebas y validación.

- **`prod`** — `mvn spring-boot:run -Dspring.profiles.active=prod`
  Producción. No modifica el esquema de base de datos automáticamente.

### Ejecutar las pruebas automatizadas

```bash
mvn test
```

---

## Documentación Interactiva (Swagger)

Con la aplicación corriendo, puedes probar todos los endpoints directamente desde el navegador:

- **Swagger UI:** [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)
- **Especificación JSON (OpenAPI):** [http://localhost:8080/api/v1/v3/api-docs](http://localhost:8080/api/v1/v3/api-docs)

---

## Servidor en Producción

El proyecto está desplegado y disponible en un servidor. No necesitas instalar nada para probarlo.

- **API base:** [http://144.217.241.132:8085/api/v1/](http://144.217.241.132:8085/api/v1/)
- **Swagger UI (servidor):** [http://144.217.241.132:8085/api/v1/swagger-ui/index.html](http://144.217.241.132:8085/api/v1/swagger-ui/index.html)

### Monitoreo con Spring Boot Actuator

La aplicación expone endpoints de Actuator para monitorear el estado del servidor en tiempo real:

- **Estado general de la aplicación:**
  `GET` http://144.217.241.132:8085/api/v1/actuator/health

- **Información de la aplicación:**
  `GET` http://144.217.241.132:8085/api/v1/actuator/info

- **Listado de todas las métricas disponibles:**
  `GET` http://144.217.241.132:8085/api/v1/actuator/metrics

- **Métrica específica — uso de memoria JVM (heap):**
  `GET` http://144.217.241.132:8085/api/v1/actuator/metrics/jvm.memory.used

- **Métrica específica — número de peticiones HTTP activas:**
  `GET` http://144.217.241.132:8085/api/v1/actuator/metrics/http.server.requests

- **Niveles de log en tiempo real:**
  `GET` http://144.217.241.132:8085/api/v1/actuator/loggers

---


## Endpoints Principales

Todas las respuestas tienen el mismo formato:
```json
{
  "mensaje": "Descripción del resultado",
  "datos": { ... }
}
```

### 1. Registrar Médico
**POST** `/medicos`
```json
// REQUEST
{
  "nombreCompleto": "Dra. Ana Lopez",
  "especialidad": "Dermatologia",
  "telefono": "555-1003",
  "email": "ana.lopez@medisalud.com"
}

// RESPONSE (201 Created)
{
  "mensaje": "Médico registrado exitosamente",
  "datos": {
    "id": 1,
    "nombreCompleto": "Dra. Ana Lopez",
    "especialidad": "Dermatologia",
    "telefono": "555-1003",
    "email": "ana.lopez@medisalud.com"
  }
}
```

### 2. Registrar Paciente
**POST** `/pacientes`
```json
// REQUEST
{
  "nombreCompleto": "Carlos Ruiz",
  "documento": "100200300",
  "telefono": "555-9000",
  "email": "carlos.ruiz@ejemplo.com",
  "fechaNacimiento": "1990-05-15"
}

// RESPONSE (201 Created)
{
  "mensaje": "Paciente registrado exitosamente",
  "datos": {
    "id": 1,
    "nombreCompleto": "Carlos Ruiz",
    "documento": "100200300"
  }
}
```

### 3. Reservar Cita
**POST** `/citas`

Valida automáticamente horarios, disponibilidad del médico, edad del paciente y penalizaciones previas.
```json
// REQUEST
{
  "pacienteId": 1,
  "medicoId": 1,
  "fechaHora": "2030-05-20T10:00:00"
}

// RESPONSE (201 Created)
{
  "mensaje": "Cita reservada exitosamente",
  "datos": {
    "id": 1,
    "pacienteId": 1,
    "medicoId": 1,
    "fechaHora": "2030-05-20T10:00:00",
    "estado": "PROGRAMADA",
    "penalizado": false
  }
}
```

### 4. Consultar Citas con parametros opcionales
**GET** `/citas?medicoId=1&estado=PROGRAMADA`

Todos los parámetros son opcionales. Puedes filtrar por médico, paciente, estado y rango de fechas.
```json
// RESPONSE (200 OK)
{
  "mensaje": "Citas obtenidas exitosamente",
  "datos": [
    {
      "id": 1,
      "pacienteId": 1,
      "medicoId": 1,
      "fechaHora": "2030-05-20T10:00:00",
      "estado": "PROGRAMADA"
    }
  ]
}
```

### 5. Consultar Horarios Disponibles de un Médico
**GET** `/citas/disponibles?medicoId=1&fechaInicio=2030-05-20&fechaFin=2030-05-22`

Devuelve todas las franjas horarias libres del médico en el rango de fechas indicado. Excluye automáticamente domingos, festivos y horarios ya ocupados.
```json
// RESPONSE (200 OK)
{
  "mensaje": "Franjas horarias disponibles obtenidas",
  "datos": [
    "2030-05-20T08:00:00",
    "2030-05-20T08:30:00",
    "2030-05-20T09:00:00"
  ]
}
```

### 6. Reprogramar Cita
**PUT** `/citas/{id}/reprogramar`

Cancela la cita original y crea una nueva en el horario indicado.
```json
// REQUEST
{
  "nuevaFechaHora": "2030-05-20T11:30:00"
}

// RESPONSE (200 OK)
{
  "mensaje": "Cita reprogramada exitosamente",
  "datos": {
    "id": 2,
    "pacienteId": 1,
    "medicoId": 1,
    "fechaHora": "2030-05-20T11:30:00",
    "estado": "PROGRAMADA"
  }
}
```

### 7. Cancelar Cita
**DELETE** `/citas/{id}`

Si se cancela con menos de 2 horas de anticipación, se registra como penalización en el historial del paciente.
```json
// RESPONSE (200 OK)
{
  "mensaje": "Cita cancelada exitosamente",
  "datos": {
    "id": 1,
    "estado": "CANCELADA",
    "penalizado": false
  }
}
```
