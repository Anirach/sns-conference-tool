plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    api(project(":identity"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    api("com.google.firebase:firebase-admin:9.3.0")
    api("com.eatthepath:pushy:0.15.4")
}
