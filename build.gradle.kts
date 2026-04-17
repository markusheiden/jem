import net.ltgt.gradle.errorprone.errorprone

plugins {
    application
    jacoco
    alias(libs.plugins.javafx)
    alias(libs.plugins.error.prone)
    alias(libs.plugins.shadow)
    alias(libs.plugins.build.time.tracker)
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
        languageVersion = JavaLanguageVersion.of(26)
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

    implementation("org.slf4j:slf4j-api")
    runtimeOnly("ch.qos.logback:logback-classic")
    compileOnly("jakarta.annotation:jakarta.annotation-api")

    implementation(libs.serialthreads)

    implementation(libs.bundles.c64dt)

    implementation("org.apache.commons:commons-lang3")

    errorprone(libs.error.prone)
    errorprone(libs.nullaway)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation(testFixtures(libs.serialthreads))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        // Persist parameter names for reflection.
        "-parameters"
    ))

    // Disable all checks, as we only want to use the NullAway checks of the errorprone plugin.
    // This needs to be configured by the project currently though.
    options.errorprone.disableAllChecks = true
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

    ignoreFailures = true


    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}
