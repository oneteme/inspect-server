# Inpect Server 

<p align="center">
  <a href="https://spring.io/">
    <img src="https://img.shields.io/badge/SpringBoot-3.2-$.svg?logo=spring&logoColor=white" alt="Angular 16" style="border-radius: 4px;">
  </a>
  <a href="https://github.com/oneteme/jquery/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/jquery-4.0.3-blue.svg" alt="License" style="border-radius: 4px;">
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
    buffer-size: 50 # Initial number of sessions in the buffer
    buffer-max-size: -1 # Maximum number of sessions in the buffer
```


#### API Reference

| VARIABLE                               | TYPE       | REQUIRED    | 
|----------------------------------------|------------|-------------|
| INSPECT_ENABLED                       | **string** | false       |
| INSPECT_DISPATCH_DELAY                | **int**    | 30          | 
| INSPECT_DISPATCH_UNIT                 | **string** | SECONDS     |
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

#### API Reference

| VARIABLE                               | TYPE       | REQUIRED    | 
|----------------------------------------|------------|-------------|
| INSPECT_PURGE_ENABLED                  | **string** | false       | 
| INSPECT_PURGE_SCHEDULE                 | **string** |  0 0 1 * * ?| 
| INSPECT_PURGE_DEPTH                    | **string** | 90          | 


---
