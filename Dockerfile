# moments4j — runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# non-root user
RUN useradd -r -u 1001 moments && mkdir -p /app/uploads /app/logs && chown -R moments:moments /app
USER moments

COPY target/moments4j-*.jar app.jar

# uploads/logs are meant to be mounted
VOLUME ["/app/uploads", "/app/logs"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
