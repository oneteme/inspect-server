# INSPECT Server 

<p align="center">
  <a href="https://spring.io/">
    <img src="https://img.shields.io/badge/SpringBoot-3.2-$.svg?logo=spring&logoColor=white" alt="Angular 16" style="border-radius: 4px;">
  </a>
  <a href="https://github.com/oneteme/jquery/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/jquery-4.0.5-blue.svg" alt="License" style="border-radius: 4px;">
  </a>
</p>

## 📋 Table of Contents

- ## [Integration](#%EF%B8%8F-integration)
  - ### [Setup](#setup-1)
  - ### [Dispatch](#dispatch-1)
  - ### [Partition](#partition-1)
  - ### [Purge](#purge-1)


---

# 🛠️ Integration

Starting the server is very straightforward, similar to any Spring Boot project. You just need to configure the data source.

## Setup
```YAML
spring:
  datasource:
    url: **
    username: **
    password: **
    driver-class-name: **
```

## Dispatch

The dispatch configuration section is responsible for managing how sessions collected by various collectors are buffered in memory before being periodically saved to the database according to the configured delay.

```YAML
inspect:
  ..
  dispatch:
    delay: 30 #sever trace frequency
    unit: SECONDS
```

You can even configure the initial size of the buffer as well as the maximum size at which INSPECT will discard additional sessions in case they cannot be saved to the database.

```YAML
inspect:
  ..
  dispatch:
    delay: 30 #sever trace frequency
    unit: SECONDS
    buffer-size: 50 # Initial number of sessions in the buffer
    buffer-max-size: -1 # Maximum number of sessions in the buffer
```


#### API Reference

| VARIABLE                               | TYPE       | REQUIRED    | 
|----------------------------------------|------------|-------------|
| INSPECT_ENABLED                       | **string** | false       |
| INSPECT_DISPATCH_DELAY                | **int**    | 30          | 
| INSPECT_DISPATCH_UNIT                 | **string** | SECONDS     |
| INSPECT_DISPATCH_BUFFER_SIZE           | **int** | 50     |
| INSPECT_DISPATCH_BUFFER_MAX_SIZE       | **int**    | -1          |

---

## Partition

The partition configuration section is designed to adapt data partitioning based on traffic and the volume of data to be stored. This ensures efficient handling and storage of data by segmenting it according to specific criteria.

```YAML
inspect:
  #...
  partition:
    enabled: true #A flag to enable or disable the partition functionality
    #schedule: "0 0 0 L * ?" The cron expression that defines when the partition operation should be executed
    session:
      http: DAY
      #main: MONTH
    request:
      http: DAY
      jdbc: DAY
      #ftp: MONTH
      #smtp: MONTH
      #ldap: MONTH
      #local: MONTH

```

#### API Reference

| VARIABLE                        | TYPE                        | REQUIRED    | 
|---------------------------------|-----------------------------|-------------|
| INSPECT_PARTITION_ENABLED       | **string**                  | false       | 
| INSPECT_PARTITION_SCHEDULE      | **string**                  | 0 0 0 L * ? | 
| INSPECT_PARTITION_SESSION_HTTP  | **string**  ( DAY / MONTH ) | MONTH     | 
| INSPECT_PARTITION_SESSION_MAIN  | **string**  ( DAY / MONTH ) | MONTH         | 
| INSPECT_PARTITION_REQUEST_HTTP  | **string**  ( DAY / MONTH ) | MONTH         | 
| INSPECT_PARTITION_REQUEST_JDBC  | **string**  ( DAY / MONTH ) | MONTH       | 
| INSPECT_PARTITION_REQUEST_FTP   | **string**  ( DAY / MONTH ) |  MONTH      | 
| INSPECT_PARTITION_REQUEST_SMTP  | **string**  ( DAY / MONTH ) |  MONTH       | 
| INSPECT_PARTITION_REQUEST_LDAP  | **string**  ( DAY / MONTH ) |  MONTH       | 
| INSPECT_PARTITION_REQUEST_LOCAL | **string**  ( DAY / MONTH ) |  MONTH     | 

---

## Purge

The purge configuration section is responsible for automatically deleting traces that exceed a certain configured delay. This delay can be overridden for different environments, such as DEV and PROD, to better manage data volume while retaining important traces for a longer period.

```YAML
inspect:
  #...
  purge:
    enabled : true # A flag to enable or disable the purge functionality.
    schedule:  "0 0 1 * * ?" # The cron expression that defines when the purge operation should be executed.
    depth: 90 #The number of days for which traces should be retained before being purged
    #env
      #ppd: 120
```

#### Purge API — `POST /purge`

Allows manual triggering of the data purge operation (also runs on a schedule).

| VARIABLE                               | TYPE       | REQUIRED    | 
|----------------------------------------|------------|-------------|
| INSPECT_PURGE_ENABLED                  | **string** | false       | 
| INSPECT_PURGE_SCHEDULE                 | **string** |  0 0 1 * * ?| 
| INSPECT_PURGE_DEPTH                    | **string** | 90          | 


---

# Technical Documentation

## Overview

**inspect-server** is the central backend component of the **INSPECT** platform — an *INtegrated System Performance Evaluation and Communication Tracking* solution. It is a Spring Boot 3.2 application (Java 21) that acts as a telemetry aggregation server: it receives distributed traces from instrumented client applications (via `inspect-core` collectors), persists them to a relational database, and exposes REST APIs to query and analyze those traces.

---

## Architecture

```
Instrumented apps  ──►  inspect-server  ──►  PostgreSQL / H2
   (collectors)          (this module)         (persistence)
                              │
                              └──►  inspect-app (UI frontend)
```

The server sits at the center of the observability stack:

- **Collectors** (e.g., `inspect-ng-collector`, `inspect-nginx-collector`) push traces to the server via REST.
- The server buffers them in-memory in a **dispatcher queue**, then persists them in batches.
- A **query API** allows the frontend (or any consumer) to retrieve and aggregate traces.

---

## Technology Stack

| Component          | Technology                                |
|--------------------|-------------------------------------------|
| Framework          | Spring Boot 3.2                           |
| Language           | Java 21                                   |
| Build              | Maven                                     |
| Default database   | H2 (in-memory, for development)           |
| Production database| PostgreSQL                                |
| Security           | Spring Security + OAuth2 Resource Server  |
| Utilities          | Lombok, Jackson, jQuery (oneteme)         |

---

## Project Structure

```
src/main/java/org/usf/inspect/server/
├── InspectApplication.java            # Spring Boot entry point
├── InspectServerConfiguration.java    # Main server auto-configuration
├── PurgeScheduler.java                # Scheduled purge trigger
├── PartitionScheduler.java            # Scheduled partition trigger
├── PartitionProperties.java           # Partition configuration binding
├── Utils.java                         # Shared utilities
├── JsonUtils.java                     # JSON helpers
│
├── controller/                        # REST API controllers
│   ├── TraceController.java           # Trace ingestion endpoint (v4/trace)
│   ├── RequestController.java         # Query endpoint (v3/query)
│   ├── ScriptController.java          # Script execution endpoint
│   ├── CacheController.java           # Cache management
│   ├── MetadataController.java        # Field/filter metadata
│   ├── JQueryController.java          # JQuery DSL endpoint
│   └── PurgeController.java           # Manual purge trigger
│
├── service/
│   ├── TraceService.java              # Trace ingestion logic + dispatcher bridge
│   ├── TracePersistenceService.java   # Async batch persistence to DB
│   ├── RequestService.java            # Query execution logic
│   ├── ScriptService.java             # Script handling
│   └── PurgeService.java             # Parallel purge with virtual threads
│
├── dao/
│   ├── TraceDao.java                  # DB write operations for traces
│   ├── RequestDao.java                # DB read operations for queries
│   └── PurgeDao.java                  # DB delete operations for purge
│
├── model/                             # Domain models (sessions, requests, etc.)
├── dto/                               # Data Transfer Objects for responses
├── mapper/                            # ResultSet → model mappers
├── config/                            # DB table/column/API configurations
├── metadata/                          # Field metadata descriptors
├── validation/                        # Custom validation annotations
├── event/                             # Application events (unsaved traces)
└── exception/                         # Global exception handling
```

---

## Data Model

The server persists 14 types of telemetry data to the database:

### Sessions
| Table           | Description                                      |
|-----------------|--------------------------------------------------|
| `e_main_ses`    | Main application sessions (batch, scheduled jobs)|
| `e_rst_ses`     | REST/HTTP incoming sessions (inbound requests)   |
| `e_rst_ses_stg` | REST session stages (timeline steps)             |

### Requests (outbound calls made during a session)
| Table           | Description              |
|-----------------|--------------------------|
| `e_rst_rqt`     | Outbound REST/HTTP calls |
| `e_smtp_rqt`    | SMTP (mail) requests     |
| `e_ftp_rqt`     | FTP requests             |
| `e_ldap_rqt`    | LDAP directory requests  |
| `e_dtb_rqt`     | JDBC database requests   |
| `e_lcl_rqt`     | Local (in-process) calls |

### Infrastructure & Monitoring
| Table        | Description                                       |
|--------------|---------------------------------------------------|
| `e_env_ins`  | Instance environment (app name, version, host...) |
| `e_ins_trc`  | Instance trace statistics (pending, attempts)     |
| `e_exc_inf`  | Exception details                                 |
| `e_log_ent`  | Application log entries                           |
| `e_rsc_usg`  | Machine resource usage (heap, disk)               |
| `e_usr_acn`  | User actions (UI interactions)                    |

---

## REST API

### Trace Ingestion — `POST/PUT /v4/trace`

These endpoints are called by **collectors** to push data to the server.

| Method | Endpoint                                    | Description                                   |
|--------|---------------------------------------------|-----------------------------------------------|
| POST   | `/v4/trace/instance`                        | Register a new application instance           |
| PUT    | `/v4/trace/instance/{id}/session`           | Push a batch of event traces for an instance  |
| GET    | `/v4/trace/queue`                           | Peek the in-memory dispatcher queue           |
| POST   | `/v4/trace/state/{state}`                   | Change the dispatcher state (ENABLE/DISABLE)  |

**Flow:** The server validates the instance UUID, wraps traces in `InstanceTrace` metadata, and dispatches them to the internal `TraceDispatcherHub`. If the dispatcher is overloaded or disabled, it returns `503 SERVICE_UNAVAILABLE` with a `TraceFail` response.

---

### Query API — `GET /v3/query`

Used by the frontend or API consumers to retrieve and aggregate telemetry data.

| Endpoint                                         | Description                                       |
|--------------------------------------------------|---------------------------------------------------|
| `/v3/query/instance/{id}`                        | Get instance environment details                  |
| `/v3/query/instance/{id}/trace`                  | Get dispatch trace stats for an instance          |
| `/v3/query/instance/{id}/resource/usage`         | Get heap/disk resource usage history              |
| `/v3/query/instance/{id}/log/entry`              | Get log entries for an instance                   |
| `/v3/query/request/{type}/hosts`                 | Get distinct hosts for a given request type       |
| `/v3/query/session/main`                         | Query main sessions                               |
| `/v3/query/session/rest`                         | Query REST inbound sessions                       |
| `/v3/query/request/rest`                         | Query outbound REST requests                      |
| `/v3/query/request/jdbc`                         | Query JDBC database requests                      |
| `/v3/query/request/smtp`                         | Query SMTP requests                               |
| `/v3/query/request/ftp`                          | Query FTP requests                                |
| `/v3/query/request/ldap`                         | Query LDAP requests                               |

Queries are built using the **jQuery DSL** (oneteme library), supporting dynamic column selection, filtering, and ordering via HTTP parameters.

---

### Metadata API — `GET /metadata`

| Endpoint              | Description                                                    |
|-----------------------|----------------------------------------------------------------|
| `/metadata/aggregate` | List available aggregate fields (response time, error counts)  |
| `/metadata/filter`    | List available filter fields (method, host, path, status, ...) |

---


## Core Features

### 1. Dispatch & Buffering
Incoming traces are not saved immediately. They are first placed in an in-memory queue managed by `TraceDispatcherHub`. A background thread flushes the queue to the database at a configurable interval.

```yaml
inspect:
  server:
    scheduling:
      interval: 30s        # Flush interval
    tracing:
      queue-capacity: 500000  # Max in-memory traces
```

If the buffer is full, new traces are rejected with `503 SERVICE_UNAVAILABLE`.

### 2. Data Partitioning
Tables can be partitioned by **DAY** or **MONTH** to manage large volumes of data efficiently. Partitioning runs on a cron schedule (default: last day of each month).

```yaml
inspect:
  server:
    partition:
      enabled: true
      schedule: "0 0 0 L * ?"
      http-session: DAY
      http-request: DAY
      jdbc-request: DAY
```

### 3. Automatic Purge
Old traces are automatically deleted based on a configurable retention policy. Each registered instance can declare its own `retentionMaxAge` in its configuration. The purge runs in parallel using **Java 21 virtual threads** for high throughput.

```yaml
inspect:
  server:
    purge:
      enabled: true
      # schedule and depth are instance-driven via retention config
```

The purge follows a dependency-aware order (e.g., stages are deleted before parent requests).

### 4. Security
The server supports two security modes, switchable via Spring profiles:

- **No security** (`NoSecurityConfig`): all endpoints are open (useful for development).
- **OAuth2 JWT** (`SecurityConfig`): endpoints are protected using a JWT resource server. Configure with:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-identity-provider/
```

### 5. Error Handling
- `GlobalExceptionHandler` catches all controller-level exceptions and returns structured HTTP error responses.
- `DispatchProcessingException` distinguishes retryable vs non-retryable dispatch failures.
- `PayloadTooLargeException` is thrown when incoming payloads exceed the configured limit (default: 50 MB).
- Traces that fail to be saved emit an `UnsavedEventTraceEvent`, which the `TraceService` intercepts to log a REPORT-level `LogEntry` back into the queue.

---

## Configuration Reference

### Full `application.yml` structure

```yaml
server:
  port: 9001

spring:
  datasource:
    url: jdbc:postgresql://host:5432/inspect
    username: ***
    password: ***
    driver-class-name: org.postgresql.Driver

inspect:
  server:
    scheduling:
      interval: 30s                  # Dispatch flush interval
    tracing:
      queue-capacity: 500000         # In-memory queue size
    partition:
      enabled: true
      schedule: "0 0 0 L * ?"        # Cron: last day of month
      http-session: DAY              # Partition granularity: DAY or MONTH
      http-request: DAY
      jdbc-request: DAY
    purge:
      enabled: false                 # Enable auto-purge
```

### Environment Variables

| Variable                           | Type     | Default        | Description                          |
|------------------------------------|----------|----------------|--------------------------------------|
| `INSPECT_ENABLED`                  | boolean  | `false`        | Enable/disable the server             |
| `INSPECT_DISPATCH_DELAY`           | int      | `30`           | Dispatch flush interval               |
| `INSPECT_DISPATCH_UNIT`            | string   | `SECONDS`      | Time unit for dispatch delay          |
| `INSPECT_DISPATCH_BUFFER_SIZE`     | int      | `50`           | Initial buffer size                   |
| `INSPECT_DISPATCH_BUFFER_MAX_SIZE` | int      | `-1` (no limit)| Max buffer size before rejection      |
| `INSPECT_PARTITION_ENABLED`        | boolean  | `false`        | Enable table partitioning             |
| `INSPECT_PARTITION_SCHEDULE`       | string   | `0 0 0 L * ?`  | Cron for partition run                |
| `INSPECT_PARTITION_SESSION_HTTP`   | string   | `MONTH`        | Partition granularity for REST sessions|
| `INSPECT_PARTITION_REQUEST_HTTP`   | string   | `MONTH`        | Partition granularity for REST requests|
| `INSPECT_PARTITION_REQUEST_JDBC`   | string   | `MONTH`        | Partition granularity for JDBC requests|
| `INSPECT_PURGE_ENABLED`            | boolean  | `false`        | Enable automatic purge                |
| `INSPECT_PURGE_SCHEDULE`           | string   | `0 0 1 * * ?`  | Cron for purge run                    |
| `INSPECT_PURGE_DEPTH`              | int      | `90`           | Retention depth in days               |

---

## Database Support

| Database   | Profile | Notes                              |
|------------|---------|------------------------------------|
| H2         | default | In-memory, for development/testing |
| PostgreSQL | `pg`    | Recommended for production          |

Schema is initialized via `schema.sql` (H2) or `schema-pg.sql` (PostgreSQL) on startup. An incremental migration script `schema-pg-update.sql` is available for schema upgrades.

---

## Building & Running

### Prerequisites
- Java 21+
- Maven 3.8+

### Build
```bash
mvn clean package
```

### Run (development, H2)
```bash
java -jar target/inspect-server-1.5.0.jar
```

### Run (production, PostgreSQL)
```bash
java -jar target/inspect-server-1.5.0.jar \
  --spring.profiles.active=pg \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/inspect \
  --spring.datasource.username=user \
  --spring.datasource.password=secret
```

### Docker
A `dockerfile` is provided at the repository root for containerized deployments.

---
