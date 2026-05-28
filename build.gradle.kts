plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "net.labymod.intellij"

repositories {
    mavenCentral()

    intellijPlatform {
        releases()
        marketplace()
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
intellijPlatform {
    pluginConfiguration {
        name = "Single Hotswap"

        ideaVersion {
            sinceBuild = "233"
            untilBuild = provider { null }
        }
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.1")

        // https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html#bundled-and-other-plugins
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("com.intellij.properties")
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        options.release = 17
    }
}
