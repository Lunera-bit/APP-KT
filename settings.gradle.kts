import org.gradle.api.initialization.resolve.RepositoriesMode
import java.io.FileInputStream
import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

fun loadMapboxToken(): String {
    val localProps = Properties()
    val localFile = File(rootDir, "local.properties")
    if (localFile.exists()) {
        FileInputStream(localFile).use { localProps.load(it) }
    }
    return localProps.getProperty("MAPBOX_SECRET_TOKEN")
        ?: System.getenv("MAPBOX_SECRET_TOKEN")
        ?: error("MAPBOX_SECRET_TOKEN not found in local.properties or environment variables")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = loadMapboxToken()
            }
        }
    }
}

rootProject.name = "TiendaCYRYELNative"
include(":app")
