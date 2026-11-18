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

// The continuation clock uses the continuations of the JDK, which are an internal API.
val continuationExports = "java.base/jdk.internal.vm=ALL-UNNAMED"

application {
    mainClass = "de.heiden.jem.models.c64.C64Serial"
    // JavaFX loads native libraries.
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED", "--add-exports=$continuationExports")
}

java {
    // https://docs.gradle.org/current/userguide/toolchains.html
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
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
        "-parameters",
        "--add-exports=$continuationExports"
    ))

    options.errorprone {
        disableAllChecks = true
        option("NullAway:AnnotatedPackages", "de.heiden")
        // error("NullAway")
    }
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

// Pass the video system of the emulator to its JVM, e.g. ./gradlew run -Djem.video=ntsc
tasks.withType<JavaExec>().configureEach {
    System.getProperty("jem.video")?.let { systemProperty("jem.video", it) }
}

tasks.named<JavaExec>("run") {
    // The JavaFX plugin puts JavaFX on the module path.
    jvmArgs("--enable-native-access=javafx.graphics", "--add-exports=$continuationExports")
    // The JavaFX plugin accesses the task extensions at execution time.
    notCompatibleWithConfigurationCache("JavaFX plugin is not compatible with the configuration cache")
}

// Run the emulator with byte code transformation via the serial threads agent instead of the transforming class loader.
tasks.register<JavaExec>("runAgent") {
    group = ApplicationPlugin.APPLICATION_GROUP
    description = "Runs the C64 emulator with byte code transformation via the serial threads agent."
    mainClass = "de.heiden.jem.models.c64.C64SerialAgent"
    // Put JavaFX on the module path, like the JavaFX plugin does for the run task.
    // For "run", this is done by the JavaFX plugin. For this task we need to reproduce this manually.
    val runtimeClasspath = sourceSets.main.get().runtimeClasspath
    val javafxModules = runtimeClasspath.filter { it.name.startsWith("javafx-") }
    classpath = runtimeClasspath - javafxModules
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "--module-path", javafxModules.asPath,
            "--add-modules", javafx.modules.joinToString(","),
            "--enable-native-access=javafx.graphics",
            "--add-exports=$continuationExports",
        )
    })
    doFirst {
        // The serial threads jar declares the agent in its manifest (Premain-Class).
        val agentJar = classpath.files.single { it.name.startsWith("serialthreads") && it.name.endsWith(".jar") }
        jvmArgs("-javaagent:$agentJar")
    }
}

// Benchmark the clocks. Each clock is executed in its own JVM without the JaCoCo agent.
tasks.register<JavaExec>("clockBenchmark") {
    group = "verification"
    description = "Benchmarks the clocks."
    mainClass = "de.heiden.jem.components.clock.ClockBenchmark"
    classpath = sourceSets.test.get().runtimeClasspath
    jvmArgs("--add-exports=$continuationExports")
}

tasks.shadowJar {
    archiveClassifier = ""
    manifest {
        attributes(
            "Implementation-Title" to "JemC64",
            "Implementation-Version" to archiveVersion,
            // JavaFX loads native libraries.
            "Enable-Native-Access" to "ALL-UNNAMED",
            "Add-Exports" to "java.base/jdk.internal.vm",
            // "Launcher-Agent-Class" to "org.serialthreads.agent.Agent",
        )
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    jvmArgs(
        "-Xmx4g",
        // Configure locale etc. for reproducible tests.
        "-Duser.language=en",
        "-Duser.country=US",
        "-Duser.timezone=UTC",
        // Prevent warnings from test setups. Avoid conflicts with JaCoCo.
        "-Xshare:off",
        "--add-exports=$continuationExports"
    )

    ignoreFailures = false
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

// Test the clocks executing their components sequentially on a single carrier thread,
// so all components and events are executed by the same thread.
// The other clocks need multiple carrier threads, so this is not possible in the JVM of the other tests.
val singleCarrierTest = tasks.register<Test>("singleCarrierTest") {
    group = "verification"
    description = "Tests the sequential thread and fiber based clocks on a single carrier thread."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter {
        includeTestsMatching("de.heiden.jem.components.clock.threads.SequentialClockTest")
        includeTestsMatching("de.heiden.jem.components.clock.threads.SequentialYieldClockTest")
        includeTestsMatching("de.heiden.jem.components.clock.loom.SequentialFiber*ClockTest")
    }
    systemProperty("jdk.virtualThreadScheduler.parallelism", "1")
    systemProperty("jdk.virtualThreadScheduler.maxPoolSize", "1")
}

tasks.check {
    dependsOn(singleCarrierTest)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}
