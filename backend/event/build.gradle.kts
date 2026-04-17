plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    api(project(":identity"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.hibernate.orm:hibernate-spatial")
    api("org.locationtech.jts:jts-core:1.19.0")
}
