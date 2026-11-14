pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "uid-getter"
include(":app")
include(":wirebare-kernel")
project(":wirebare-kernel").projectDir = File(rootProject.projectDir, "wirebare-kernel/wirebare-kernel")
