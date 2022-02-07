import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "io.github.rk012"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":core"))
    implementation("com.github.ajalt.clikt:clikt:3.4.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}