plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
val releaseSigningProperties = listOf(
    "YAMAHA_RELEASE_STORE_FILE",
    "YAMAHA_RELEASE_KEY_ALIAS",
    "YAMAHA_RELEASE_STORE_PASSWORD",
    "YAMAHA_RELEASE_KEY_PASSWORD"
).associateWith { providers.gradleProperty(it) }

val validateReleaseSigningCredentials = tasks.register("validateReleaseSigningCredentials") {
    group = "verification"
    description = "Require owner-controlled signing properties for release artifacts."
    doLast {
        val missing = releaseSigningProperties.filterValues {
            it.orNull.isNullOrEmpty()
        }.keys
        check(missing.isEmpty()) {
            "Release signing requires Gradle properties: " + missing.joinToString() +
                ". Configure them outside the repository. No unsigned release will be produced."
        }
        check(file(releaseSigningProperties.getValue("YAMAHA_RELEASE_STORE_FILE").get()).isFile) {
            "Release signing keystore is unavailable. Check YAMAHA_RELEASE_STORE_FILE."
        }
    }
}

android {
    namespace = "com.styl15hh1.rn301controller"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.styl15hh1.rn301controller"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "1.2.0"
    }
    signingConfigs {
        create("release") {
            storeFile = releaseSigningProperties.getValue("YAMAHA_RELEASE_STORE_FILE")
                .orNull?.takeIf { it.isNotEmpty() }?.let { file(it) }
            keyAlias = releaseSigningProperties.getValue("YAMAHA_RELEASE_KEY_ALIAS").orNull
            storePassword = releaseSigningProperties.getValue("YAMAHA_RELEASE_STORE_PASSWORD").orNull
            keyPassword = releaseSigningProperties.getValue("YAMAHA_RELEASE_KEY_PASSWORD").orNull
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    bundle { language { enableSplit = false } }
    lint { abortOnError = true }
    testOptions.unitTests.isIncludeAndroidResources = true
}
// Release-only task dependencies also cover aggregate build/assemble invocations.
// No secret values are task inputs or part of failure messages.
tasks.matching { it.name == "preReleaseBuild" || it.name == "validateSigningRelease" }
    .configureEach { dependsOn(validateReleaseSigningCredentials) }

tasks.withType<Test>().configureEach { inputs.dir("src/main/res") }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
