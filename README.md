


## Integration [![Docker Pulls](https://img.shields.io/docker/v/oneteme/inspect-server?style=social)](https://hub.docker.com/r/oneteme/inspect-server)
```SH
docker run --pull=always -d --name inspect-app -e SPRING_DATASOURCE_URL="{{URL}}" -e SPRING_DATASOURCE_USERNAME="{{USER}}" -e SPRING_DATASOURCE_PASSWORD="{{PASS}}" -p 80:80 oneteme/inspect-app:{{version}}
```

## API Reference

| VARIABLE                   | TYPE   | REQUIRED | 
|----------------------------|------------|----------|
| SPRING_DATASOURCE_URL      | **string** | x        | 
| SPRING_DATASOURCE_USERNAME | **string** | x        | 
| SPRING_DATASOURCE_PASSWORD | **string** | x        |
| VERSION                    | **string** | x        | 
