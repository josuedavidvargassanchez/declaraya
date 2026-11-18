FROM eclipse-temurin:21-jre
WORKDIR /app
# la aplicacion no corre como root: usuario de sistema sin shell, dueño solo de /app (incluye uploads/)
RUN groupadd --system app && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/uploads && chown -R app:app /app
COPY --chown=app:app target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
