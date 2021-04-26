plugins {
    val kotlin_plugin = "1.5.0-RC"
    idea
    kotlin("jvm") version kotlin_plugin
    id("org.jetbrains.kotlin.plugin.serialization") version kotlin_plugin
    id("org.jetbrains.dokka") version "1.4.30"
    id("maven-publish")
//    id("com.jfrog.artifactory") version "4.21.0"
//    id("fr.brouillard.oss.gradle.jgitver") version "0.6.1"
}

group = "io.rsug"
version = "0.0.1-build6"
//jgitver {
//    strategy(fr.brouillard.oss.jgitver.Strategies.CONFIGURABLE)
//}

repositories {
    mavenCentral()
    maven{
        requireNotNull(property("gpr.user"))
        requireNotNull(property("gpr.key"))
        url = uri("https://maven.pkg.github.com/pdvrieze/xmlutil")
        credentials {
            username = property("gpr.user") as String
            password = property("gpr.key") as String
        }
        content {
            includeGroup("io.github.pdvrieze.xmlutil")
        }
    }
}

val kotlinx_serialization_version: String by project
val xmlutil_version: String by project
val woodstox_version: String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlutil_version")
    runtimeOnly("com.fasterxml.woodstox:woodstox-core:$woodstox_version")
    implementation("com.github.xmlet:xsdParser:1.1.3")
    testImplementation(kotlin("test-junit"))
}

publishing {
    requireNotNull(property("gpr.user"))
    requireNotNull(property("gpr.key"))
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/rsugio/karlutka")
            credentials {
                username = property("gpr.user") as String
                password = property("gpr.key") as String
            }
        }
    }
    publications {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group as String
                artifactId = "karlutka"
                version = project.version as String

                from(components["java"])
            }
        }
    }
}

tasks.test {
    useJUnit()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "16"
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
}
