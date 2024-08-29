plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.1"
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
            sinceBuild = "203"
            untilBuild = provider { null }
        }
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2.0.2")

        // https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html#bundled-and-other-plugins
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("com.intellij.properties")

        instrumentationTools()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
}
