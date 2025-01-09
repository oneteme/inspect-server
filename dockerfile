# Utilisez une image de base contenant JRE 11 slim
FROM openjdk:11-jre-slim

# Définissez le répertoire de travail dans le conteneur
WORKDIR /app

# Copiez le fichier JAR généré dans le conteneur
COPY $(ls target/inspect-server*.jar | head -n 1) /app/inspect-server.jar

# Exposez le port sur lequel l'application écoute
EXPOSE 9000

# Commande pour exécuter l'application
ENTRYPOINT ["java", "-jar", "inspect-server.jar"]