plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    api(project(":identity"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")

    // Object storage
    api("software.amazon.awssdk:s3:2.28.29")
    api("com.hypersistence:hypersistence-utils-hibernate-63:3.9.0")
    api("org.apache.opennlp:opennlp-tools:2.3.3")
}
