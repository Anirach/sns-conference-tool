FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :app:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app/build/libs/sns-backend.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
