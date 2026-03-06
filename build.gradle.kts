plugins {
    `java-library`
//    kotlin("jvm")
    id("com.github.ben-manes.versions")
}

val gradleVersionProp: String by project
tasks.wrapper {
    gradleVersion = gradleVersionProp
}

apply(from = "gradle/javafx.gradle.kts")

val openjfxPlatform: String by extra

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

val slf4jVersion: String by project
val logbackVersion: String by project
val annotationVersion: String by project
val serialthreadsVersion: String by project
val c64dtVersion: String by project
val openjfxVersion: String by project
val commonsLangVersion: String by project
val junitPlatformVersion: String by project
val junitVersion: String by project
val assertjVersion: String by project

dependencies {
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")
    runtimeOnly("ch.qos.logback:logback-classic:${logbackVersion}")
    implementation("jakarta.annotation:jakarta.annotation-api:${annotationVersion}")

    implementation("org.serialthreads:serialthreads:${serialthreadsVersion}")
    testImplementation("org.serialthreads:serialthreads:${serialthreadsVersion}:tests")

    implementation("de.heiden.c64dt:assembler:${c64dtVersion}")
    implementation("de.heiden.c64dt:bytes:${c64dtVersion}")
    implementation("de.heiden.c64dt:charset:${c64dtVersion}")
    implementation("de.heiden.c64dt:disk:${c64dtVersion}")
    implementation("de.heiden.c64dt:gui:${c64dtVersion}")

    implementation("org.openjfx:javafx-base:${openjfxVersion}:${openjfxPlatform}")
    implementation("org.openjfx:javafx-controls:${openjfxVersion}:${openjfxPlatform}")
    implementation("org.openjfx:javafx-graphics:${openjfxVersion}:${openjfxPlatform}")

    implementation("org.apache.commons:commons-lang3:${commonsLangVersion}")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${junitPlatformVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter:${junitVersion}")
    testImplementation("org.assertj:assertj-core:${assertjVersion}")
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
