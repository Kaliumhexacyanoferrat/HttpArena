plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "httparena"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
}

tasks.shadowJar {
    archiveBaseName.set("kotlin-coroutines")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest { attributes["Main-Class"] = "httparena.ServerKt" }
}

kotlin {
    jvmToolchain(21)
}
