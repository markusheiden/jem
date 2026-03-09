plugins {
    application
    jacoco
    alias(libs.plugins.javafx)
    alias(libs.plugins.shadow)
    alias(libs.plugins.versions)
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
}

repositories {
    // mavenLocal()
    mavenCentral()
}

application {
    mainClass = "de.heiden.jem.models.c64.C64Serial"
}

java {
    // https://docs.gradle.org/current/userguide/toolchains.html
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        // Use Eclipse Temurin (provided by Adoptium).
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

javafx {
    version = libs.versions.openjfx.get()
    modules("javafx.base", "javafx.controls", "javafx.graphics")
}

configurations.all {
    resolutionStrategy.failOnDynamicVersions()
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

tasks.withType<AbstractArchiveTask> {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.jar {
    enabled = false
}

tasks.startScripts {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveClassifier = ""
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

    ignoreFailures = false

    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}
