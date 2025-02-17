plugins {
    `java-platform`
    id("org.spongepowered.configurate.build.publishing")
}

description = "Dependency alignment for all Configurate modules"

indra {
    configurePublications {
        from(components["javaPlatform"])
    }
}

dependencies {
    constraints {
        api(projects.core)
        api(projects.extra.extraKotlin)
        api(projects.extra.extraGuice)
        api(projects.extra.extraDfu2)
        api(projects.extra.extraDfu3)
        api(projects.extra.extraDfu4)
        api(projects.tool)
        api(projects.format.gson)
        api(projects.format.hocon)
        api(projects.format.jackson)
        api(projects.format.xml)
        api(projects.format.yaml)
    }
}
