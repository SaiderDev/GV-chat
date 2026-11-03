// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.20" apply false

    // The google-services plugin is required to parse the google-services.json file
    id("com.google.gms.google-services") version "4.3.15" apply false
}

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}

tasks {
    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }
}
