import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    application
    java
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("io.ktor.plugin") version "2.3.0"
    `maven-publish`
}

group = "io.rsug"
version = "0.2.2"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}
val ktor_version = "2.3.0"
val kotlinx_serialization_version = "1.5.0"
val xmlutil_version = "0.86.0"
val kaml_version = "0.53.0"
val h2_version = "1.4.200"
val camel_version = "4.0.0-M3"

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("ch.qos.logback:logback-classic:1.4.7") //1.2.11
    implementation("com.fasterxml.uuid:java-uuid-generator:4.1.0")
    implementation("com.sun.mail:javax.mail:1.6.2")             //implementation("org.apache.commons:commons-email:1.5")
    implementation("commons-codec:commons-codec:1.15")

    implementation("io.github.pdvrieze.xmlutil:core-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:serialutil-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:xmlserializable:$xmlutil_version")
    implementation("com.charleskorn.kaml:kaml:$kaml_version")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")   // версия не совпадает с котлином
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

    testImplementation(kotlin("test"))
    testImplementation("commons-io:commons-io:2.11.0")    //BOMInputStream
    implementation("org.apache.camel:camel-core:$camel_version")
    //implementation("org.apache.camel:camel-http:$camel_version")
    implementation("org.apache.camel:camel-base:$camel_version")
    implementation("org.apache.camel:camel-main:$camel_version")
    implementation("org.apache.camel:camel-componentdsl:$camel_version")
    implementation("org.apache.camel:camel-xml-io-dsl:$camel_version")
    //compileOnly("org.apache.camel:spi-annotations:$camel_version")
}


application {
    applicationDefaultJvmArgs = listOf("-Xms64m -Xmx196m")
    mainClass.set("MainKt")
}

publishing {//https://docs.gradle.org/current/userguide/publishing_maven.html
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.rsug"
            artifactId = "karlutka"
            version = "0.2.2"

            from(components["java"])
        }
    }
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
//    dependencies {
//    }
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
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
