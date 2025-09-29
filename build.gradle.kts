plugins {
    kotlin("jvm") version "2.2.0"
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("plugin.spring") version "2.2.0"
}

group = "org.projects"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("software.amazon.awssdk:lambda:2.29.35")
//    runtimeOnly("org.postgresql:postgresql")
//    implementation("org.testcontainers:postgresql:1.19.3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.assertj:assertj-core:3.26.3")
//    testImplementation("org.testcontainers:testcontainers:1.19.3")
//    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
//    testImplementation("org.testcontainers:postgresql:1.19.3")
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}