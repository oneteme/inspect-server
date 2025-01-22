FROM eclipse-temurin:17.0.13_11-jre-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENV SPRING_PROFILES_ACTIVE=pg
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]