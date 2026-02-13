# Utilisation de Java 17 (adaptable selon l'environnement disponible)
FROM openjdk:17-jdk-slim

# Informations du maintainer
LABEL maintainer="insurance-agents-system"
LABEL description="Système multi-agents d'assurance avec Spring AI et LangChain4j"

# Variables d'environnement
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=docker

# Création du répertoire de l'application
WORKDIR /app

# Copie des fichiers nécessaires pour Maven
COPY pom.xml ./
COPY mvnw ./
COPY mvnw.cmd ./
COPY .mvn .mvn

# Installation des dépendances (pour optimiser les layers Docker)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copie du code source
COPY src ./src

# Construction de l'application
RUN ./mvnw clean package -DskipTests

# Création du répertoire pour les uploads
RUN mkdir -p /app/uploads

# Port exposé (selon les spécifications de l'API REST)
EXPOSE 8080

# Commande de démarrage avec optimisations JVM
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar target/demo-0.0.1-SNAPSHOT.jar"]

# Health check pour Docker
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
