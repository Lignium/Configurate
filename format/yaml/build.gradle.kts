import net.ltgt.gradle.errorprone.errorprone
import org.spongepowered.configurate.build.useAutoValue

plugins {
    id("org.spongepowered.configurate.build.component")
    groovy // For writing tests
}

description = "YAML format loader for Configurate"

tasks.withType(GroovyCompile::class).configureEach {
    // Toolchains need this... for some reason
    options.release.set(indra.javaVersions.target)
}

useAutoValue()
dependencies {
    api(projects.core)
    // When updating snakeyaml, check ConfigurateScanner for changes against upstream
    implementation("org.yaml:snakeyaml:1.+")

    testImplementation("org.codehaus.groovy:groovy:3.+:indy")
    testImplementation("org.codehaus.groovy:groovy-nio:3.+:indy")
    testImplementation("org.codehaus.groovy:groovy-test-junit5:3.+:indy")
    testImplementation("org.codehaus.groovy:groovy-templates:3.+:indy")
}

tasks.compileJava {
    options.errorprone.excludedPaths.set(".*org[\\\\/]spongepowered[\\\\/]configurate[\\\\/]yaml[\\\\/]ConfigurateScanner.*")
    // our vendored version of ScannerImpl has invalid JD, so we have to suppress some warnings
    options.compilerArgs.add("-Xdoclint:-html")
}
