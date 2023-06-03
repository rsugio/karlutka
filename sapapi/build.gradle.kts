import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
val kotlin_version: String by project
val xmlutil_version: String by project
val karlutka_version: String by project
val karlutka_group: String by project
val fasterxmluuid_version: String by project
val logback_version: String by project

plugins {
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization")
    `java-library`
}

group = karlutka_group
version = karlutka_version

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("com.fasterxml.uuid:java-uuid-generator:$fasterxmluuid_version")
    implementation("io.github.pdvrieze.xmlutil:core-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:serialutil-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:xmlserializable:$xmlutil_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "17"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "17"
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeTags("Offline")
        excludeTags("Online")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
