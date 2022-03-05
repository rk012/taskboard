import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    application
}

group = "io.github.rk012"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("com.github.ajalt.clikt:clikt:3.4.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks{
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "MainKt"))
        }
    }
}

application {
    mainClass.set("MainKt")
}