# moments4j — runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# non-root user
RUN useradd -r -u 1001 moments && mkdir -p /app/uploads /app/logs && chown -R moments:moments /app
USER moments

COPY target/moments4j-*.jar app.jar
COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# uploads/logs are meant to be mounted; /app/data holds the sqlite db
# (production should use postgres instead — see application-prod.yml)
VOLUME ["/app/uploads", "/app/logs", "/app/data"]

EXPOSE 8080

ENTRYPOINT ["docker-entrypoint.sh"]
