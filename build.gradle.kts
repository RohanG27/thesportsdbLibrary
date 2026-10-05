plugins {
    kotlin("jvm") version "2.4.20"
    `java-library`
    id("org.jetbrains.dokka") version "2.2.0"
}

group = "local.sportsdb" // TODO: set the published Maven group before release
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

dependencies {
    api("com.squareup.okhttp3:okhttp:5.5.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

tasks.test {
    useJUnitPlatform {
        // Live tests call the real API; run them with: ./gradlew liveTest
        excludeTags("live")
    }
}

tasks.register<Test>("liveTest") {
    description = "Runs tests against the real TheSportsDB API (free key 123 unless THESPORTSDB_API_KEY is set)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("live") }
}

dokka {
    moduleName.set("sportsdb-kotlin")
    dokkaSourceSets.main {
        includes.from("docs/dokka-module.md")
        jdkVersion.set(11)
    }
}
