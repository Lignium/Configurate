plugins {
    id("org.spongepowered.configurate.build.component")
}

description = "HOCON format loader for Configurate"

dependencies {
    api(projects.core)
    implementation("com.typesafe:config:1.+")
    testImplementation("com.google.guava:guava:latest.release")
}
