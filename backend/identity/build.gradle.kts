plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-mail")
    api("com.nimbusds:nimbus-jose-jwt:9.40")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
}
