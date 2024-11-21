# Etapa de construcción con OpenJDK 20 y Maven
FROM openjdk:20-slim AS build

# Instalar Maven
RUN apt-get update && apt-get install -y maven

# Establecer el directorio de trabajo
WORKDIR /usr/src/app

# Copiar el código fuente al contenedor
COPY . ./

# Ejecutar Maven para limpiar y empaquetar el proyecto
RUN mvn clean package -DskipTests

# Etapa para la ejecución con OpenJDK 20
FROM openjdk:20-slim

# Establecer el directorio de trabajo
WORKDIR /usr/src/app

# Copiar el archivo JAR desde la etapa de construcción
COPY --from=build /usr/src/app/target/microservicio04-0.0.1-SNAPSHOT.jar ./app.jar

# Exponer el puerto en el que correrá la app
EXPOSE 8081

# Configurar el ENTRYPOINT para ejecutar el JAR
ENTRYPOINT ["java", "-jar", "app.jar"]