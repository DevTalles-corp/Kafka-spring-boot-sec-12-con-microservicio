FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system bistro
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER bistro
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]