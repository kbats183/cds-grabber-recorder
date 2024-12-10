plugins {
    kotlin("jvm") version "2.0.10"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("plugin.serialization") version "2.0.20"
//    id("com.gradleup.shadow")
}

group = "org.icpclive"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        group = "com.github.icpc.live-v3"
    }
    gradlePluginPortal()
}

val ktor_version: String by project

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.ajalt.clikt:clikt:4.3.0")
    implementation("io.ktor:ktor-client-core:${ktor_version}")
    implementation("io.ktor:ktor-client-cio:${ktor_version}")
    implementation("io.ktor:ktor-client-auth:${ktor_version}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktor_version}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktor_version}")
    implementation("com.github.icpc.live-v3:org.icpclive.cds.full:7db2182b7787a185624cf4f0369943ecd7c8d739")
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

application {
    mainClass = "org.icpclive.reactions.recorder.ApplicationKt"
}

tasks.withType<JavaExec> {
    this.args = listOfNotNull(
        project.properties["live.dev.config"]?.let { "--config-directory=$it" },
//        project.properties["live.dev.jobs"]?.let { "--jobs=$it" },
    )
    this.workingDir = File("config")
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
        archiveClassifier.set("standalone")
        destinationDirectory = rootDir.resolve("artifacts/")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) }
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        } + sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar)
    }
}


tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.icpclive.reactions.recorder.ApplicationKt"
    }
}


tasks.named<Jar>("jar") {
    archiveClassifier = "just"
}

//tasks {
//    register<Sync>("release") {
//        destinationDir = rootDir.resolve("artifacts/")
//        preserve { include("*") }
//        from(tasks.named("shadowJar"))
//    }
//
//    shadowJar {
//        mergeServiceFiles()
//
//        archiveClassifier = null
//    }
//}

tasks.test {
    useJUnitPlatform()
}


kotlin {
//    jvmToolchain(21)
}
