FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r mackllc && useradd -r -g mackllc mackllc
COPY target/*.jar app.jar
USER mackllc
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
