import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

val customProperties = Properties()
val customPropertiesFile = rootDir.resolve("credentials.properties")
if (customPropertiesFile.exists()) {
    customPropertiesFile.inputStream().use { inputStream ->
        customProperties.load(inputStream)
    }
} else {
    println("Warning: custom.properties file not found")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://x.klarnacdn.net/mobile-sdk/")
        }
        maven {
            url = uri("https://maven.pkg.github.com/Fiserv/mobile-payments-android")
            credentials {
                username = customProperties["USERNAME"].toString()
                password = customProperties["PASSWORD"].toString()
            }
        }
    }
}

rootProject.name = "Fiserv Mobile Payments Sample"
include(":app")
include(":payment-sdk-shim")
include(":cashapppay-bridge")
include(":merchant-app")
include(":backend-server")
 