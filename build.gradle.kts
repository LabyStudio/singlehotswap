plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.13.1"
}

group = "net.labymod.intellij"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
intellij {
    pluginName.set("Single Hotswap")
    version.set("2023.1")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf("Kotlin", "Groovy", "java", "properties"))

    // Compatibility with future IDE versions
    updateSinceUntilBuild.set(false)
}

dependencies {

}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
}
