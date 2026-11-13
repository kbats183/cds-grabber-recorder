plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    application

    // Shadow plugin — лучший вариант для fat-jar
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.icpclive"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        group = "com.github.icpc.live-v3"
    }
}

val ktor_version: String by project

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.github.ajalt.clikt:clikt:4.3.0")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    implementation("com.github.icpc.live-v3:org.icpclive.cds.full:$cds_version")
    implementation("com.github.icpc.live-v3:org.icpclive.cds.yandex:$cds_version")

    implementation("ch.qos.logback:logback-classic:1.5.6")
}

tasks.shadowJar {
//    archiveClassifier.set("") // jar без "-all"

    mergeServiceFiles()       // ← ключевой момент!!!
}



application {
    // Shadow plugin подставит это значение в MANIFEST.MF
    mainClass.set("org.icpclive.reactions.recorder.ApplicationKt")
}
