plugins {
    `java-library`
//    kotlin("jvm")
    alias(libs.plugins.versions)
}

tasks.wrapper {
    gradleVersion = providers.gradleProperty("gradleVersion").get()
}

apply(from = "gradle/javafx.gradle.kts")

val openjfxPlatform: String by extra
val openjfxVersion = libs.versions.openjfx.get()

group = "de.heiden"
version = "1.0-SNAPSHOT"
base {
    archivesName = "jemc64"
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    // https://docs.gradle.org/current/userguide/toolchains.html
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        // Use Eclipse Temurin (provided by Adoptium).
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.compileJava {
//    options.compilerArgs += listOf("--enable-preview")
//    options.compilerArgs += listOf("--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED")
//    options.release = null
}
tasks.compileTestJava {
//    options.compilerArgs += listOf("--enable-preview")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))

    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    implementation(libs.jakarta.annotation.api)

    implementation(libs.serialthreads)
    testImplementation("org.serialthreads:serialthreads:${libs.versions.serialthreads.get()}:tests")

    implementation(libs.bundles.c64dt)

    // openjfx requires a platform classifier, not expressible in the version catalog
    implementation("org.openjfx:javafx-base:${openjfxVersion}:${openjfxPlatform}")
    implementation("org.openjfx:javafx-controls:${openjfxVersion}:${openjfxPlatform}")
    implementation("org.openjfx:javafx-graphics:${openjfxVersion}:${openjfxPlatform}")

    implementation(libs.commons.lang3)

    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "JemC64",
            "Implementation-Version" to archiveVersion,
            // "Launcher-Agent-Class" to "org.serialthreads.agent.Agent",
            "Main-Class" to "de.heiden.jem.models.c64.C64Serial"
        )
    }

    // TODO markus 2021-05-04: Deduplicate files.
    duplicatesStrategy = DuplicatesStrategy.WARN
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.test {
    useJUnitPlatform()

    // ignore failing tests
    ignoreFailures = true
}
