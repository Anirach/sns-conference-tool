plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    api(project(":identity"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
}
