plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    api(project(":identity"))
    api(project(":profile"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-oauth2-client")
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
}
