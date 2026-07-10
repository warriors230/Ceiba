# Sistema de Agendamiento de Citas Médicas - MediSalud

Este proyecto es una API REST (Backend) desarrollada para **MediSalud**, la cual permite la gestión y agendamiento de citas médicas. El sistema controla disponibilidades horarias, evita duplicidad de citas y gestiona políticas de penalización por cancelaciones tardías.

## Tecnologías Utilizadas

*   **Java 17:** Versión del lenguaje, asegurando soporte a largo plazo y características modernas.
*   **Spring Boot 3.1.x:** Framework base para el desarrollo rápido y estructurado de la API REST.
*   **Spring Data JPA e Hibernate:** Para el mapeo objeto-relacional y acceso a datos de forma segura.
*   **PostgreSQL:** Motor de base de datos relacional elegido para garantizar la persistencia e integridad transaccional (preparado para escalar).
*   **MapStruct & Lombok:** Herramientas para reducir código repetitivo (boilerplate) y agilizar la transformación entre modelos de dominio, entidades y DTOs.
*   **JUnit 5 & Mockito:** Herramientas para asegurar la calidad de la lógica mediante pruebas automatizadas.
*   **Maven:** Gestor de dependencias y construcción del ciclo de vida del proyecto.

## Arquitectura del Proyecto y Justificación

El proyecto está diseñado bajo los principios de la **Arquitectura Hexagonal (Ports and Adapters)**. Se eligió este enfoque arquitectónico como indicador de calidad y visión de escalabilidad por las siguientes razones:

1.  **Arquitectura Hexagonal Pragmática:** La lógica central y las reglas de negocio (RN-01 a RN-06) habitan en la capa de `dominio`. Si bien el sistema utiliza puertos e interfaces para aislarse de la base de datos y la interfaz web, hemos adoptado un enfoque pragmático al permitir ciertas anotaciones de Spring Boot (como `@Service` y `@Transactional`) en los casos de uso. Esta decisión evita caer en la sobreingeniería, ya que crear clases de configuración exclusivas en la infraestructura solo para instanciar manualmente cada caso de uso agregaría complejidad y archivos innecesarios sin aportar un valor real. Así, aprovechamos la inyección automática del framework manteniendo la lógica 100% testeable.
2.  **Separación Estricta de Responsabilidades:** 
    *   **Capa Web (Adaptadores de Entrada):** Los controladores (`*Controller.java`) solo se encargan de validar sintaxis de los JSON, delegar al caso de uso correspondiente y devolver la respuesta HTTP correcta.
    *   **Capa de Dominio:** Los servicios (`CitaService.java`) contienen las reglas del negocio fuertes. Solo interactúan con los Puertos (interfaces) definidos por ellos mismos.
    *   **Capa de Persistencia (Adaptadores de Salida):** Implementan los puertos para hablar con la base de datos usando JPA.
3.  **Facilidad de Pruebas (Testing):** Al estar la lógica del negocio completamente aislada en el dominio, es muy fácil probar las reglas con pruebas unitarias muy rápidas sin tener que levantar contextos pesados de base de datos o de Spring (Mocking puro).

### Manejo de Seguridad
*   El proyecto se encuentra fuertemente tipado mediante el uso exclusivo de `DTOs` por petición. Esto evita brechas de seguridad como **Mass Assignment** (Inyección de objetos).
*   Gracias al uso de Spring Data JPA y `Prepared Statements`, la base de datos se encuentra protegida nativamente contra ataques de **Inyección SQL**.
*   El manejo centralizado de excepciones (vía `@ControllerAdvice`) encripta cualquier error de servidor (`500`) impidiendo fugas de información sobre la traza del código fuente al usuario final, y devolviendo descripciones legibles para errores de validación y negocio.

---

## Instrucciones para Ejecutar Localmente

### Prerrequisitos
*   Tener **Java 17** (o superior) instalado en el sistema (`JAVA_HOME` configurado correctamente).
*   Tener **Maven** instalado (o usar el wrapper de maven incluido).
*   *Nota: Por defecto, el proyecto utilizará una base de datos embebida (H2) en tiempo de ejecución local para facilitar la evaluación, o puedes cambiar las propiedades en `application.properties` para apuntar a un clúster local de PostgreSQL si así lo deseas.*

### Ejecución
1.  Abre una terminal en la raíz del proyecto.
2.  Ejecuta el siguiente comando para compilar y descargar dependencias:
    ```bash
    mvn clean install
    ```
3.  Levanta la aplicación Spring Boot:
    ```bash
    mvn spring-boot:run
    ```
    *La aplicación se levantará por defecto en el puerto `8080`.*

### Ejecución de Pruebas Automatizadas
Para correr la suite de validación de reglas de negocio en la capa de servicios, ejecuta:
```bash
mvn test
```

---

## Documentación Interactiva (Swagger UI)

El proyecto incluye documentación interactiva y autogenerada gracias a **OpenAPI 3 / Springdoc**. 

Una vez que la aplicación esté ejecutándose localmente, puedes acceder a la interfaz gráfica de Swagger para explorar, visualizar y probar todos los endpoints de la API directamente desde tu navegador sin necesidad de utilizar Postman u otras herramientas externas.

**URL de acceso a Swagger:**
[http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)

**URL de la especificación OpenAPI (JSON):**
[http://localhost:8080/api/v1/v3/api-docs](http://localhost:8080/api/v1/v3/api-docs)

---

## Endpoints de la API (Ejemplos)

A continuación, se describen los flujos principales del API REST. Todas las respuestas cuentan con un formato estándar `ApiResponse` que indica el estado, mensaje e información resultante.

### 1. Registrar Médico
**POST** `/medicos`
```json
// REQUEST
{
    "nombreCompleto": "Dra. Ana López",
    "especialidad": "Dermatología",
    "telefono": "555-1003",
    "email": "ana.lopez@medisalud.com"
}

// RESPONSE (201 Created)
{
    "mensaje": "Médico registrado exitosamente",
    "datos": {
        "id": 1,
        "nombreCompleto": "Dra. Ana López",
        "especialidad": "Dermatología",
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

### 3. Reservar Cita (Agendamiento)
**POST** `/citas`
*(Valida reglas RN-01 a RN-05: Rango horario, disponibilidad del médico, edad y penalizaciones).*
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

### 4. Consultar Citas con Filtros
**GET** `/citas?medicoId=1&estado=PROGRAMADA`
```json
// RESPONSE (200 OK)
{
    "mensaje": "Listado obtenido",
    "datos": [
        {
            "id": 1,
            "pacienteId": 1,
            "medicoId": 1,
            "fechaHora": "2030-05-20T10:00:00",
            "estado": "PROGRAMADA",
            "penalizado": false
        }
    ]
}
```

### 5. Reprogramar Cita
**PUT** `/citas/{id}/reprogramar`
*(Valida disponibilidad del nuevo horario, aplica regla de cancelación RN-05 al horario antiguo).*
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
        "estado": "PROGRAMADA",
        "penalizado": false
    }
}
```

### 6. Cancelar Cita
**DELETE** `/citas/{id}`
*(Aplica penalización automática si la cancelación es menor a 2 horas previo a la cita).*
```json
// RESPONSE (200 OK)
{
    "mensaje": "Cita cancelada exitosamente",
    "datos": {
        "id": 1,
        "pacienteId": 1,
        "medicoId": 1,
        "fechaHora": "2030-05-20T10:00:00",
        "estado": "CANCELADA",
        "fechaCancelacion": "2030-05-19T20:15:30",
        "penalizado": false
    }
}
```
