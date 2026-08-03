import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room3)
}

android {
    namespace = "com.lucky3d.app"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lucky3d.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(
            providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(false),
        )
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    ksp(libs.room3.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.bundles.unit.test)
    testImplementation(libs.androidx.room3.testing)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.room3.testing)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

val verifySeedAndPrepackagedDatabase by tasks.registering {
    group = "verification"
    description = "Verifies the approved seed and bundled Room database artifacts."

    val seedFile = rootProject.layout.projectDirectory.file("data/fc3d-seed.json")
    val databaseFile = layout.projectDirectory.file("src/main/assets/database/lucky3d.db")
    inputs.files(seedFile, databaseFile)

    doLast {
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02X".format(it) }
        }

        val seed = seedFile.asFile
        val database = databaseFile.asFile
        check(seed.exists()) { "Missing approved seed: $seed" }
        check(database.exists()) { "Missing prepackaged Room database: $database" }
        check(sha256(seed) == "7D90B6074551476D5FDEDC989F001B20A9C3336476438F819E3F770C1757F4EA") {
            "Approved seed SHA-256 changed"
        }
        val issueCount = Regex(""""issue"\s*:""").findAll(seed.readText()).count()
        check(issueCount == 3334) { "Expected 3334 seed draws, found $issueCount" }
        check(sha256(database) == "89B2263DA8973DDDA3856382CCB8B939A7AC615631C769103D74FB71E81B66F2") {
            "Prepackaged database SHA-256 changed; rebuild and review it explicitly"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifySeedAndPrepackagedDatabase)
}
