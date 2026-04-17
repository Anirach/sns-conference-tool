plugins {
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    java
}

allprojects {
    group = "com.sns"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.5")
        }
    }

    dependencies {
        "implementation"("org.slf4j:slf4j-api")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.junit.jupiter:junit-jupiter")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
        options.encoding = "UTF-8"
    }
}
