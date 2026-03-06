plugins {
    `application`
    alias(libs.plugins.javafx)
    alias(libs.plugins.shadow)
    alias(libs.plugins.versions)
}

javafx {
    version = libs.versions.openjfx.get()
    modules("javafx.base", "javafx.controls", "javafx.graphics")
}

group = "de.heiden"
version = "1.0-SNAPSHOT"
base {
    archivesName = "jemc64"
}

application {
    mainClass = "de.heiden.jem.models.c64.C64Serial"
}

repositories {
    // mavenLocal()
    mavenCentral()
}

java {
    // https://docs.gradle.org/current/userguide/toolchains.html
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        // Use Eclipse Temurin (provided by Adoptium).
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

dependencies {
    implementation(platform(libs.spring.boot.bom))

    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    compileOnly(libs.jakarta.annotation.api)

    implementation(libs.serialthreads)

    implementation(libs.bundles.c64dt)

    implementation(libs.commons.lang3)

    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(testFixtures(libs.serialthreads))
}

tasks.shadowJar {
    manifest {
        attributes(
            "Implementation-Title" to "JemC64",
            "Implementation-Version" to archiveVersion,
            // "Launcher-Agent-Class" to "org.serialthreads.agent.Agent",
        )
    }
}

tasks.test {
    useJUnitPlatform()

    // ignore failing tests
    ignoreFailures = true
}
