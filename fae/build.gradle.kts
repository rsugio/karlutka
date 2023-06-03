import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val karlutka_version: String by project
val karlutka_group: String by project

val kotlinx_serialization_version: String by project
val xmlutil_version: String by project
val kaml_version: String by project
val h2_version: String by project
val camel_version: String by project
val snakeyaml_version: String by project
val fasterxmluuid_version: String by project
val kotlinx_coroutines_version: String by project
val logback_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin") version "2.3.0"
    application
}

group = karlutka_group
version = karlutka_version

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation(project(":sapapi"))
    implementation(fileTree(mapOf("dir" to "../libs", "include" to listOf("*.jar"))))

    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.fasterxml.uuid:java-uuid-generator:$fasterxmluuid_version")
    implementation("io.github.pdvrieze.xmlutil:core-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:serialutil-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:xmlserializable:$xmlutil_version")

    implementation("com.charleskorn.kaml:kaml:$kaml_version")
    implementation("org.snakeyaml:snakeyaml-engine:$snakeyaml_version")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinx_serialization_version")

    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cio-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")    // обработка 404 и ошибок обработки
    implementation("io.ktor:ktor-server-compression-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-java-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-logging-jvm:$ktor_version")

    implementation("com.h2database:h2:$h2_version")

    implementation("org.apache.camel:camel-core:$camel_version")
    implementation("org.apache.camel:camel-core-model:$camel_version")
    implementation("org.apache.camel:camel-core-catalog:$camel_version")
    implementation("org.apache.camel:camel-base:$camel_version")
    implementation("org.apache.camel:camel-catalog:$camel_version")
    implementation("org.apache.camel:camel-management:$camel_version")
    implementation("org.apache.camel:camel-api:$camel_version")
    implementation("org.apache.camel:camel-http:$camel_version")
    implementation("org.apache.camel:camel-componentdsl:$camel_version")
    implementation("org.apache.camel:camel-xml-io-dsl:$camel_version")
    implementation("org.apache.camel:camel-yaml-dsl:$camel_version")
    implementation("org.apache.camel:camel-support:$camel_version")     //?
    implementation("org.apache.camel:camel-ftp:$camel_version")
    implementation("org.apache.camel:camel-kafka:$camel_version")
    implementation("org.apache.camel:camel-jsonpath:$camel_version")
    implementation("org.apache.camel:camel-seda:$camel_version")
    implementation("org.apache.camel:camel-endpointdsl:$camel_version")
    implementation("org.apache.camel:camel-core-languages:$camel_version")
    implementation("org.apache.camel:camel-xpath:$camel_version")
    implementation("org.apache.camel:camel-util:$camel_version")
    implementation("org.apache.camel:camel-amqp:$camel_version")
    implementation("org.apache.camel:camel-telegram:$camel_version")
    implementation("org.apache.camel:camel-ognl:$camel_version")
    implementation("org.apache.camel:camel-attachments:$camel_version")

    testImplementation(kotlin("test"))
}


application {
//    applicationDefaultJvmArgs = listOf("-Xms64m -Xmx196m")
    applicationName = "FakeAdapterEngine"
    mainClass.set("MainKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "17"        //1.8
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "17"
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeTags("Offline")
        //excludeTags("Online")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
