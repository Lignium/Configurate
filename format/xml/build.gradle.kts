plugins {
    id("org.spongepowered.configurate.build.component")
}

description = "XML format loader for Configurate"

dependencies {
    api(projects.core)
    testImplementation("com.google.guava:guava:latest.release")
}
