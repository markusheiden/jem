plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "jem"

includeBuild("../serialthreads")
includeBuild("../c64dt")
