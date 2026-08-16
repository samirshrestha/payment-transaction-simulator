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

// Google Drive sync leaves desktop.ini litter in every folder (see .gitignore); it isn't part of
// this project's resources but Gradle's resource copying otherwise trips over it as a duplicate.
tasks.withType<AbstractCopyTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
