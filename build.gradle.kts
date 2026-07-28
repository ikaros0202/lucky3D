plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
}

tasks.register("qualityGate") {
    group = "verification"
    description = "Runs unit tests, Android lint, and the debug build."
    dependsOn(
        ":app:testDebugUnitTest",
        ":app:lintDebug",
        ":app:assembleDebug",
    )
}
