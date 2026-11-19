FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-noble
WORKDIR /app
RUN mkdir -p /var/lib/ccm/documents
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
