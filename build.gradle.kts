import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm") version "1.5.0-M2"    // version "1.4.32"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.0-M2"
    id("org.jetbrains.dokka") version "1.4.30"
}

group = "com.pinternals"
version = "0.0.1-build3"

repositories {
    jcenter()
    mavenCentral()
//    maven(url = "https://dl.bintray.com/pdvrieze/maven")
    // see https://stackoverflow.com/questions/48242437/how-to-add-a-maven-repository-by-url-using-kotlinscript-dsl-build-gradle-kts
    maven{
        url = uri("https://dl.bintray.com/pdvrieze/maven")
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    implementation("net.devrieze:xmlutil-jvm:0.81.1")
    implementation("net.devrieze:xmlutil-serialization-jvm:0.81.1")
    runtimeOnly("com.fasterxml.woodstox:woodstox-core:6+")

    implementation("com.github.xmlet:xsdParser:1.1.3")

    // шихта всякая для изучений:
    testImplementation(kotlin("test-junit"))
    testImplementation(files("C:/workspace/Karlutka/libs/com.sap.xi.mapping.tool.lib.filter.jar"))
    testImplementation(files("C:/workspace/Karlutka/libs/com.sap.aii.mapping.api.filter.jar"))
    testImplementation(files("C:/workspace/Karlutka/libs/com.sap.xi.mapping.tool.lib_api.jar"))
    testRuntimeOnly(files("C:/workspace/Karlutka/libs/com.sap.aii.utilxi.core.jar"))
    testRuntimeOnly(files("C:/workspace/Karlutka/libs/sap.com~tc~logging~java~impl.jar"))
    testRuntimeOnly(files("C:/workspace/Karlutka/libs/sap.com~tc~bl~guidgenerator~impl.jar"))
    testRuntimeOnly(files("C:/workspace/Karlutka/libs/com.sap.exception.jar"))
    testRuntimeOnly(files("C:/workspace/Karlutka/libs/com.sap.aii.utilxi.server.jar"))

//    testImplementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
}
