


## Integration [![Docker Pulls](https://img.shields.io/docker/v/oneteme/inspect-server?style=social)](https://hub.docker.com/r/oneteme/inspect-server)
```SH
docker run --pull=always -d --name inspect-app -e SPRING_DATASOURCE_URL="{{URL}}" -e SPRING_DATASOURCE_USERNAME="{{USER}}" -e SPRING_DATASOURCE_PASSWORD="{{PASS}}" -p 80:80 oneteme/inspect-app:{{version}}
```

## Configuration
```YAML
spring:
  application:
    name: <appName>
    version: <version>
...
inspect:
  enabled: true
  dispatch:
    delay: 30 #sever trace frequency
    unit: SECONDS
    buffer-max-size: -1 #inspect-server only
  purge:
    enabled : false
    schedule: "0 0 0 * * *"
    depth: 90 #en jour
```


## API Reference

| VARIABLE                               | TYPE       | REQUIRED    | 
|----------------------------------------|------------|-------------|
| SPRING_DATASOURCE_URL                  | **string** | x           | 
| SPRING_DATASOURCE_USERNAME             | **string** | x           | 
| SPRING_DATASOURCE_PASSWORD             | **string** | x           |
| INSPECT_ENABLED                       | **string** | false       |
| INSPECT_DISPATCH_DELAY                | **int**    | 30          | 
| INSPECT_DISPATCH_UNIT                 | **string** | SECONDS     |
| INSPECT_DISPATCH_BUFFER_MAX_SIZE       | **int**    | -1          |
| INSPECT_PURGE_ENABLED                  | **string** | false       | 
| INSPECT_PURGE_SCHEDULE               | **string** | 0 0 0 * * * | 
| INSPECT_PURGE_DEPTH                    | **string** | 90          | 

