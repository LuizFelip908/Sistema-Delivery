import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

application {
    mainClass.set("MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runRestaurant") {
    group = "application"
    description = "Inicia o App Restaurante"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("MainKt")
    args("restaurant")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Inicia o App Cliente"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("MainKt")
    args("client")
    standardInput = System.`in`
}
