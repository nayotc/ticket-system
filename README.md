# Ticket System

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen)
![Vaadin](https://img.shields.io/badge/Vaadin-24.10.4-00b4f0)
![Persistence](https://img.shields.io/badge/Persistence-Spring%20Data%20JPA%20%2F%20Hibernate-orange)

Ticket System is a Spring Boot and Vaadin application for managing event production companies, event catalogs, hall maps, reservations, lotteries, orders, memberships, discounts, and system administration.

This document focuses on how to initialize and configure the system according to the current persistence and bootstrap implementation.

## System Requirements & Initialization

### Requirements

- Java 17
- Maven 3.9+ or the Maven wrapper if one is added later
- A database for the selected runtime mode:
  - Local development: the default in-memory H2 database from `src/main/resources/application.properties`
  - Cloud/production: an external PostgreSQL database configured through Spring properties
- Required runtime secrets supplied through environment variables or an external properties file

### Required Environment Variables

The default application configuration always enables the system-admin bootstrap. Before starting the server, provide a system admin password:

```powershell
$env:SYSTEM_ADMIN_PASSWORD = "change-me-to-a-strong-admin-password"
```

Recommended production variables:

```powershell
$env:SYSTEM_ADMIN_USERNAME = "admin@example.com"
$env:SYSTEM_ADMIN_PASSWORD = "change-me-to-a-strong-admin-password"
$env:SYSTEM_ADMIN_FULL_NAME = "System Admin"
$env:SYSTEM_ADMIN_PHONE = "0500000000"
$env:SYSTEM_ADMIN_BIRTH_DATE = "2001-01-01"
$env:JWT_SECRET = "change-me-to-at-least-32-characters-long"
$env:EXTERNAL_SYSTEM_URL = "https://your-external-service.example/"
```

For cloud persistence, also provide:

```powershell
$env:DB_HOST = "your-postgres-host"
$env:DB_PORT = "5432"
$env:DB_NAME = "ticketsystem"
$env:DB_USER = "your-db-user"
$env:DB_PASSWORD = "your-db-password"
```

### Build and Run Locally

From the project root:

```powershell
mvn clean spring-boot:run
```

By default, the application uses:

- `src/main/resources/application.properties`
- H2 in-memory database: `jdbc:h2:mem:ticketsystem`
- H2 console: `/h2-console`
- System admin bootstrap: enabled
- Initial-state file bootstrap: disabled

After startup, open the Vaadin application in the browser at:

```text
http://localhost:8080
```

### Run with Cloud Persistence

Activate the `cloud` Spring profile:

```powershell
mvn clean spring-boot:run "-Dspring-boot.run.profiles=cloud"
```

The `cloud` profile loads `src/main/resources/application-cloud.properties`, which expects database and secret values from environment variables.

### Run with Initial-State Bootstrap

The Version 3 initial-state bootstrap is disabled by default. Enable it with the `initial-state` profile:

```powershell
$env:INITIAL_STATE_FILE = "file:./config/initial-state-v3.json"
mvn clean spring-boot:run "-Dspring-boot.run.profiles=initial-state"
```

To initialize a remote database and load the initial state in the same startup:

```powershell
$env:INITIAL_STATE_FILE = "file:./config/initial-state-v3.json"
mvn clean spring-boot:run "-Dspring-boot.run.profiles=cloud,initial-state"
```

The `initial-state` profile loads `src/main/resources/application-initial-state.properties`, which sets:

```properties
ticketsystem.initial-state.enabled=true
ticketsystem.initial-state.file=${INITIAL_STATE_FILE:file:./config/initial-state-v3.json}
```

## External Configuration File Format

Spring Boot can load configuration from `application.properties` or `application.yml`. This project already includes:

- `src/main/resources/application.properties` for local development
- `src/main/resources/application-cloud.properties` for remote PostgreSQL persistence
- `src/main/resources/application-initial-state.properties` for file-based bootstrap activation

For production, keep database credentials outside the source code. Provide them through environment variables, a secure secret manager, or an external configuration file that is not committed to Git.

### Required Persistence Properties

The persistence layer uses Spring Data JPA and Hibernate. At minimum, an external database configuration must define:

```properties
spring.datasource.url=...
spring.datasource.username=...
spring.datasource.password=...
spring.datasource.driver-class-name=...
spring.jpa.hibernate.ddl-auto=...
spring.jpa.show-sql=...
```

The project also configures Hibernate cache behavior and HikariCP connection pooling for the cloud profile.

### Production PostgreSQL Example

This matches the current `application-cloud.properties` profile and the PostgreSQL driver included in `pom.xml`.

```properties
# Remote PostgreSQL database
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}?ssl=true&sslmode=require&connectTimeout=10&socketTimeout=10&loginTimeout=10
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.use_query_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
spring.jpa.properties.hibernate.javax.cache.provider=org.ehcache.jsr107.EhcacheCachingProvider
spring.jpa.properties.hibernate.generate_statistics=false

# HikariCP
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.validation-timeout=5000
spring.datasource.hikari.initialization-fail-timeout=0
spring.datasource.hikari.max-lifetime=1800000

# Security and external services
jwt.secret=${JWT_SECRET}
external.system.url=${EXTERNAL_SYSTEM_URL}

# Disable local H2 console in cloud mode
spring.h2.console.enabled=false
```

### Google Cloud SQL for PostgreSQL Example

Use the Cloud SQL instance private IP, public IP, or DNS name as `DB_HOST`, depending on the deployment topology.

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}?ssl=true&sslmode=require&connectTimeout=10&socketTimeout=10&loginTimeout=10
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

jwt.secret=${JWT_SECRET}
external.system.url=${EXTERNAL_SYSTEM_URL}
```

### MySQL Format Example

The current `pom.xml` includes PostgreSQL and H2 drivers. If the system is deployed on MySQL, add a MySQL JDBC driver dependency first, then configure:

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME}?useSSL=true&requireSSL=true&serverTimezone=UTC
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

jwt.secret=${JWT_SECRET}
external.system.url=${EXTERNAL_SYSTEM_URL}
```

### Architectural Note

Database credentials, JWT secrets, external-service URLs, and system-admin credentials must be managed through external configuration. They must never be hardcoded in Java classes, committed in `application.properties`, or stored in source-controlled files such as `.env`.

## Initial State File Format

### Current Implementation

The current Version 3 bootstrap is implemented by `InitialStateFileInitializer`. It does not parse a semicolon command DSL such as:

```text
guest-registration(username, password, email, ...);
login(username, password);
open-production-company(token, company_name, ...);
appoint-manager(token, company_name, employee_username, ...);
```

Instead, it reads a JSON file using Jackson. The default external file path is:

```text
config/initial-state-v3.json
```

The active path is controlled by:

```properties
ticketsystem.initial-state.file=${INITIAL_STATE_FILE:file:./config/initial-state-v3.json}
```

The initializer is activated only when:

```properties
ticketsystem.initial-state.enabled=true
```

### JSON Schema

The initial-state file must contain exactly the top-level data groups consumed by `InitialStateFileInitializer`:

```json
{
  "users": [
    {
      "id": "u1",
      "username": "u1@example.com",
      "password": "123456",
      "fullName": "User One",
      "phone": "0500000001",
      "birthDate": "2001-01-01"
    }
  ],
  "company": {
    "name": "p1"
  },
  "event": {
    "name": "e1",
    "artist": "Demo Artist",
    "date": "2026-12-31T20:00:00",
    "location": "TEL_AVIV",
    "category": "CONCERT",
    "trafficThreshold": 1000,
    "eventBasePrice": 50,
    "mapHeight": 20,
    "mapWidth": 20,
    "standingZone": {
      "name": "Standing Zone",
      "capacity": 30,
      "price": 50
    },
    "seatingZone": {
      "name": "Seating Zone",
      "rows": 10,
      "columns": 10,
      "price": 100
    }
  },
  "couponDiscount": {
    "name": "Company Sale Coupon",
    "code": "sale123",
    "percentage": 20,
    "compositionType": "MAX",
    "expiresAt": "2027-12-31T23:59:59"
  }
}
```

### Field Rules

- `users` must include entries with IDs `u1`, `u2`, and `u3`; the initializer looks them up by these IDs.
- `username`, `password`, `fullName`, `phone`, and `birthDate` are passed to user signup.
- `birthDate` must use ISO local-date format: `YYYY-MM-DD`.
- `event.date` and `couponDiscount.expiresAt` must use ISO local-date-time format: `YYYY-MM-DDTHH:mm:ss`.
- `event.location` must be one of: `NEW_YORK`, `LOS_ANGELES`, `CHICAGO`, `HOUSTON`, `MIAMI`, `TEL_AVIV`, `JERUSALEM`, `BEER_SHEVA`, `HAIFA`, `OTHER`.
- `event.category` must be one of: `CONCERT`, `SPORTS`, `THEATER`, `EXHIBITION`, `OTHER`.
- `couponDiscount.compositionType` must be one of: `SUM`, `MAX`.
- Numeric fields such as `trafficThreshold`, `eventBasePrice`, `mapHeight`, `mapWidth`, `capacity`, `rows`, `columns`, `price`, and `percentage` must be valid JSON numbers.

### Bootstrap Command Sequence

Although the file format is JSON, the initializer executes a fixed sequence of application-service actions equivalent to the following setup commands:

```text
guest-registration(u1.username, u1.password, u1.fullName, u1.phone, u1.birthDate);
guest-registration(u2.username, u2.password, u2.fullName, u2.phone, u2.birthDate);
guest-registration(u3.username, u3.password, u3.fullName, u3.phone, u3.birthDate);
guest-registration(u4.username, u4.password, u4.fullName, u4.phone, u4.birthDate);

login(u1.username, u1.password);
open-production-company(u1Token, company.name);

login(u2.username, u2.password);
login(u3.username, u3.password);

appoint-owner(u1Token, company.id, u2.username);
approve-assignment(u2Token, company.id);

appoint-manager(u2Token, company.id, u3.username, [CONFIGURE_HALL_AND_MAP]);
approve-assignment(u3Token, company.id);

add-event(u2Token, company.id, event.name, event.date, event.location, event.trafficThreshold, event.category, event.artist, event.eventBasePrice, event.mapHeight, event.mapWidth);
define-event-map(u2Token, event.id, event.standingZone, event.seatingZone);

set-company-coupon-discount(u2Token, company.id, couponDiscount.name, couponDiscount.code, couponDiscount.percentage, couponDiscount.compositionType, couponDiscount.expiresAt);

logout-and-exit(u1Token);
logout-and-exit(u2Token);
logout-and-exit(u3Token);
```

The only manager permission assigned by the current initializer is:

```text
CONFIGURE_HALL_AND_MAP
```

### Example File

The repository includes a ready-to-use example:

```text
config/initial-state-v3.json
```

It creates:

- Users `u1@example.com`, `u2@example.com`, `u3@example.com`, and `u4@example.com`
- Production company `p1`
- Owner assignment for `u2`
- Manager assignment for `u3` with hall/map configuration permission
- Event `e1`
- A standing zone with capacity `30`
- A `10 x 10` seating zone
- Company coupon discount `sale123`

### Execution and Atomicity Rules

Initialization commands are processed sequentially by Spring Boot startup runners:

1. `SystemAdminBootstrapInitializer` runs first and guarantees that the configured system admin exists.
2. `InitialStateFileInitializer` runs later only if `ticketsystem.initial-state.enabled=true`.

The intended initialization contract is atomic: the setup should complete successfully only if all configured actions are valid and execute without errors. If any step fails, startup aborts and reports the specific exception raised by the failing service call or file parser.

When running against a persistent database, execute the initial-state bootstrap on an empty or disposable database unless the intended state is already known. The current initializer is partially idempotent for existing users, company, and event, but it does not implement a full transaction boundary around the entire bootstrap sequence. For strict rollback guarantees, run initialization inside a managed deployment/database transaction strategy or initialize a fresh database snapshot and discard it on failure.

## Useful Commands

Run tests:

```powershell
mvn test
```

Build the application:

```powershell
mvn clean package
```

Run the packaged application:

```powershell
java -jar target/ticketsystem-1.0-SNAPSHOT.jar
```
