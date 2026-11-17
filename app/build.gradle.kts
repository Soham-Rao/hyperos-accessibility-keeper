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
        applicationId = "io.github.soham.accessibilitykeeper"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
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
        val expected = components.map { (it as Map<*, *>)["coordinate"].toString() }.toSet()
        val actual = configurations.getByName("releaseRuntimeClasspath")
            .resolvedConfiguration.resolvedArtifacts.map { it.moduleVersion.id.toString() }.toSet()
        check(actual == expected) {
            "Runtime dependencies changed. Review licenses and update legal/runtime-dependencies.json and notices. " +
                "Added: ${actual - expected}; removed: ${expected - actual}"
        }
    }
}
tasks.named("preBuild") {
    dependsOn(generateLegalAssets, verifyLicenseInventory)
}
