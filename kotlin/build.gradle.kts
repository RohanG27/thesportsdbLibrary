plugins {
    kotlin("jvm") version "2.4.20"
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka") version "2.2.0"
}

group = "local.sportsdb" // TODO: set the published Maven group before release
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation()
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

// Placeholder coordinates (group "local.sportsdb"); set real ones before publishing anywhere public.
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "sportsdb-kotlin"
            pom {
                name.set("sportsdb-kotlin")
                description.set("Kotlin/JVM client for TheSportsDB API v1 and v2, with typed models, rate limiting, caching and a Java CompletableFuture API.")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit")
                    }
                }
                // TODO: url, developers and scm are also required by Maven Central.
            }
        }
    }
}
