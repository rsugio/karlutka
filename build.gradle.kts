import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    application
    java
    id("com.github.johnrengelman.shadow") version "7.0.0"
    //id("io.ktor.plugin") version "2.1.0"
    `maven-publish`
}

group = "io.rsug"
version = "0.2.1alpha"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
//    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers") }
}
val ktor_version = "2.1.1"
val kotlinx_serialization_version = "1.4.0"
val exposed_version = "0.39.2"
val xmlutil_version = "0.84.2"
val kaml_version = "0.47.0"
val h2_version = "2.1.214"
val jooq_version = "3.17.3"

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")   // версия не совпадает с котлином
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$kotlinx_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinx_serialization_version")
    implementation("io.github.pdvrieze.xmlutil:core-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlutil_version")
    implementation("com.charleskorn.kaml:kaml:$kaml_version")

    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.apache.commons:commons-email:1.5")

    implementation("io.ktor:ktor-client-java-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")

    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cio-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-sessions-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-locations-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-compression-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-conditional-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-partial-content-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-call-id-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-metrics-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

//    implementation("io.micrometer:micrometer-registry-prometheus:1.6.3")
//    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")        ???

//  если захотим кликхаус
//  implementation("com.clickhouse:clickhouse-jdbc:0.3.2-patch1")

    implementation("com.h2database:h2:$h2_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
//    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")  я использую DSL без DAO
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("io.ktor:ktor-client-core-jvm:2.1.1")
    implementation("io.ktor:ktor-client-auth-jvm:2.1.1")
    implementation("io.ktor:ktor-client-logging-jvm:2.1.1")
//    implementation("org.jooq:jooq:$jooq_version")
//    implementation("org.apache.commons:commons-compress:1.21")


    testImplementation(kotlin("test"))
    testImplementation("commons-io:commons-io:2.11.0")    //BOMInputStream
    //testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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
            version = "0.2.1alpha"

            from(components["java"])
        }
    }
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
//    dependencies {
//    }
}
