import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlin_version: String by project
val xmlutil_version: String by project
val ktor_version: String by project
val karlutka_version: String by project
val karlutka_group: String by project
val fasterxmluuid_version: String by project
val logback_version: String by project

val jfrogMavenRepo: String by project
val jfrogMavenUser: String by project
val jfrogMavenPassw: String by project

plugins {
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization")
    `java-library`
    `maven-publish`
}

group = karlutka_group
version = karlutka_version

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    maven {
        url = uri(jfrogMavenRepo)
        credentials {
            username = jfrogMavenUser
            password = jfrogMavenPassw
        }
    }
}

dependencies {
    implementation("com.fasterxml.uuid:java-uuid-generator:$fasterxmluuid_version")
    implementation("io.github.pdvrieze.xmlutil:core-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:serialutil-jvm:$xmlutil_version")
    implementation("io.github.pdvrieze.xmlutil:xmlserializable:$xmlutil_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    testImplementation(kotlin("test"))
}

publishing {//https://docs.gradle.org/current/userguide/publishing_maven.html
    repositories {
        maven {
            name = "JFROG"
            url = uri(jfrogMavenRepo)
            credentials {
                username = jfrogMavenUser
                password = jfrogMavenPassw
            }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/rsugio/karlutka")
            credentials {
                username = findProperty("gpr.user").toString()
                password = findProperty("gpr.key").toString()
            }
            authentication {
//                val a = Base64.getEncoder().encodeToString("${credentials.username}:${credentials.password}".toByteArray())
                //header("Authorization", "Basic " + a)
            }
        }
    }
    publications {
        create<MavenPublication>("JFROG") {
            groupId = karlutka_group
            artifactId = "sapapi"
            version = karlutka_version

            from(components["java"])
        }
        create<MavenPublication>("GitHubPackages") {
            groupId = karlutka_group
            artifactId = "sapapi"
            version = karlutka_version

            from(components["java"])
        }
    }
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
        //includeTags("Offline")
        //excludeTags("Online")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
