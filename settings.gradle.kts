rootProject.name = "demo1"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        // mavenLocal()
        maven("https://gitlab.com/api/v4/projects/75787729/packages/maven")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // mavenLocal()
        maven("https://gitlab.com/api/v4/projects/75787860/packages/maven")
        maven("https://gitlab.com/api/v4/projects/75787729/packages/maven")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApplication")
// include(":androidApplication")
include(":composeLibrary")
