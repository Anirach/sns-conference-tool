pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "sns-backend"

include("app", "common", "identity", "profile", "event", "interest", "matching", "chat", "notification", "sns")
