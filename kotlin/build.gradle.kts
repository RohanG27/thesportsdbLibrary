import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar

plugins {
    kotlin("jvm") version "2.4.20"
    `java-library`
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("org.jetbrains.dokka") version "2.2.0"
}

group = "io.github.rohang27"
// Release builds pass -PreleaseVersion=x.y.z (the release workflow takes it from the kotlin/vx.y.z tag).
version = providers.gradleProperty("releaseVersion").getOrElse("0.1.0-SNAPSHOT")

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
    moduleName.set("thesportsdb-client")
    dokkaSourceSets.main {
        includes.from("docs/dokka-module.md")
        jdkVersion.set(11)
    }
}

mavenPublishing {
    configure(KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"), sourcesJar = SourcesJar.Sources()))
    publishToMavenCentral(automaticRelease = true)
    // Signs only when a key is configured (CI); local publishToMavenLocal stays unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates("io.github.rohang27", "thesportsdb-client", version.toString())
    pom {
        name.set("thesportsdb-client")
        description.set("Kotlin/JVM client for TheSportsDB API v1 and v2, with typed models, rate limiting, caching and a Java CompletableFuture API.")
        inceptionYear.set("2026")
        url.set("https://github.com/RohanG27/thesportsdbLibrary")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
            }
        }
        developers {
            developer {
                id.set("RohanG27")
                name.set("RohanG27")
                url.set("https://github.com/RohanG27")
            }
        }
        scm {
            url.set("https://github.com/RohanG27/thesportsdbLibrary")
            connection.set("scm:git:https://github.com/RohanG27/thesportsdbLibrary.git")
            developerConnection.set("scm:git:ssh://git@github.com/RohanG27/thesportsdbLibrary.git")
        }
    }
}
