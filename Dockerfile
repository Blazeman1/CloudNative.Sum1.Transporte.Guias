FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN apt-get update && apt-get install -y maven
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar

# Crear directorio para EFS
RUN mkdir -p /app/efs/guias_tmp

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
