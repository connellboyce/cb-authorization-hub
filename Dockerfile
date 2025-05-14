FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY target/*.jar app.jar

ENTRYPOINT ["sh", "-c", "java -Dspring.data.mongodb.uri=$MONGODB_URI -jar app.jar"]