
plugins {
    idea
    kotlin("jvm") version "1.5+"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5+"
    id("org.jetbrains.dokka") version "1+"
    id("maven-publish")
    id("com.jfrog.artifactory") version "4.21.0"
    id("fr.brouillard.oss.gradle.jgitver") version "0.6.1"
}

group = "io.rsug"
version = "0.0.1-2"
jgitver {
    strategy(fr.brouillard.oss.jgitver.Strategies.CONFIGURABLE)
}

repositories {
    jcenter()
    mavenCentral()
    // see https://stackoverflow.com/questions/48242437/how-to-add-a-maven-repository-by-url-using-kotlinscript-dsl-build-gradle-kts
//    maven{
//        url = uri("https://dl.bintray.com/pdvrieze/maven")
//    }
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

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5+")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:+")
    runtimeOnly("com.fasterxml.woodstox:woodstox-core:6+")   //6.2.5

    implementation("com.github.xmlet:xsdParser:1.1.3")

    testImplementation(kotlin("test-junit"))
    // шихта всякая для изучений:
//    testImplementation(files("C:/workspace/Karlutka/libs/com.sap.xi.mapping.tool.lib.filter.jar"))
//    testImplementation(files("C:/workspace/Karlutka/libs/com.sap.aii.mapping.api.filter.jar"))
//    testImplementation(files("C:/workspace/Karlutka/libs/com.sap.xi.mapping.tool.lib_api.jar"))
//    testRuntimeOnly(files("C:/workspace/Karlutka/libs/com.sap.aii.utilxi.core.jar"))
//    testRuntimeOnly(files("C:/workspace/Karlutka/libs/sap.com~tc~logging~java~impl.jar"))
//    testRuntimeOnly(files("C:/workspace/Karlutka/libs/sap.com~tc~bl~guidgenerator~impl.jar"))
//    testRuntimeOnly(files("C:/workspace/Karlutka/libs/com.sap.exception.jar"))
//    testRuntimeOnly(files("C:/workspace/Karlutka/libs/com.sap.aii.utilxi.server.jar"))

//    testImplementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
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
    kotlinOptions.jvmTarget = "11"
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
}
