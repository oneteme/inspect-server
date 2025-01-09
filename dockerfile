# Utilisez une image de base contenant JRE 11 slim
FROM openjdk:17.0.1-jdk-slim

# Copiez le fichier JAR généré dans le conteneur
ARG JAR_FILE=./target/*.jar
COPY ${JAR_FILE} app.jar
# Exposez le port sur lequel l'application écoute
EXPOSE 9000

# Commande pour exécuter l'application
ENTRYPOINT ["java", "-jar", "app.jar"]