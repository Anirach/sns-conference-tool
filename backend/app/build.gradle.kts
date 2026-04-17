plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
    application
}

application {
    mainClass.set("com.sns.app.SnsApplication")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":identity"))
    implementation(project(":profile"))
    implementation(project(":event"))
    implementation(project(":interest"))
    implementation(project(":matching"))
    implementation(project(":chat"))
    implementation(project(":notification"))
    implementation(project(":sns"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.redisson:redisson-spring-boot-starter:3.34.1")
    implementation("com.google.firebase:firebase-admin:9.3.0")
    implementation("com.eatthepath:pushy:0.15.4")
    implementation("org.apache.opennlp:opennlp-tools:2.3.3")
    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("com.hypersistence:hypersistence-utils-hibernate-63:3.9.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("com.redis:testcontainers-redis:2.2.2")
    testImplementation("org.awaitility:awaitility:4.2.2")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("sns-backend.jar")
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs @Tag(\"integration\") tests using Testcontainers (Docker required)"
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter("test")
    // Not wired into `check` so CI can opt-in via `./gradlew integrationTest`
    // and environments without Docker still run unit tests.
}
