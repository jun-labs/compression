import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.jpa")
    kotlin("plugin.spring")
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")

    // SpringBoot
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Compression
    implementation("com.github.luben:zstd-jni:1.5.0-4")
    implementation("org.xerial.snappy:snappy-java:1.1.10.4")
    implementation("org.msgpack:jackson-dataformat-msgpack:0.9.8")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
