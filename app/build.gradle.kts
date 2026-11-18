import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Signing details arrive through the environment so that neither the keystore
// nor its passwords live in the repository. CI sets them from repository
// secrets; without them the release build is simply left unsigned, which keeps
// `assembleRelease` usable locally for checking that the build still works.
val keystorePath: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "io.github.haku4130.noscrollguard"
    compileSdk = 36

    defaultConfig {
        applicationId = if (providers.gradleProperty("verificationBuild").orNull == "true")
            "io.github.soham.accessibilitykeeper.verification" else "io.github.soham.accessibilitykeeper"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "io.github.haku4130.noscrollguard.VerificationInstrumentation"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // No shrinking: the accessibility and WorkManager entry points are
            // resolved by name, and there are no proguard rules to protect them.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    testImplementation("junit:junit:4.13.2")
}

// Package canonical repository notices so source and offline app text cannot drift.
val legalAssetsDirectory = layout.buildDirectory.dir("generated/legalAssets")
val generateLegalAssets by tasks.registering(Sync::class) {
    from(rootProject.file("LICENSE")) {
        into("legal")
        rename { "MIT.txt" }
    }
    from(rootProject.file("legal")) { into("legal") }
    from(rootProject.files("PRIVACY.md", "TERMS.md", "SECURITY.md", "docs/PERMISSIONS.md")) { into("legal") }
    into(legalAssetsDirectory)
}
android.sourceSets.getByName("main").assets.srcDir(legalAssetsDirectory)

// A dependency change requires reviewing its license and refreshing the notices.
val verifyLicenseInventory by tasks.registering {
    val inventory = rootProject.file("legal/runtime-dependencies.json")
    inputs.file(inventory)
    doLast {
        val document = groovy.json.JsonSlurper().parse(inventory) as Map<*, *>
        val components = document["components"] as List<*>
        val reviewed = components.associate { val item = it as Map<*, *>; item["coordinate"].toString() to item["artifact_sha256"].toString() }
        listOf("debugRuntimeClasspath", "releaseRuntimeClasspath").forEach { configuration ->
            val artifacts = configurations.getByName(configuration).resolvedConfiguration.resolvedArtifacts
            val actual = artifacts.map { it.moduleVersion.id.toString() }.toSet()
            check(actual == reviewed.keys) {
                "Runtime dependencies changed. Review licenses and update legal/runtime-dependencies.json and notices. " +
                    "Added: ${actual - reviewed.keys}; removed: ${reviewed.keys - actual}"
            }
            artifacts.forEach { artifact ->
                val digest = MessageDigest.getInstance("SHA-256")
                artifact.file.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var count = input.read(buffer)
                    while (count != -1) { digest.update(buffer, 0, count); count = input.read(buffer) }
                }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                val coordinate = artifact.moduleVersion.id.toString()
                check(hash == reviewed[coordinate]) { "Runtime artifact checksum differs from the reviewed inventory: $coordinate. Review the artifact before updating its checksum." }
            }
        }
    }
}
tasks.named("preBuild") {
    dependsOn(generateLegalAssets, verifyLicenseInventory)
}
