plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("host.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("manualTestClient") {
    group = "application"
    description = "Interactive REPL client for manually exercising a running Host server, without a Terminal."
    mainClass.set("host.tools.ManualTestClientKt")
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
}

// Google Drive sync leaves desktop.ini litter in every folder (see .gitignore); it isn't part of
// this project's resources but Gradle's resource copying otherwise trips over it as a duplicate.
tasks.withType<AbstractCopyTask>().configureEach {
    exclude("**/desktop.ini")
}
