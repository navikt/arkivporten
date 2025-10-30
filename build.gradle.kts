plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    id("io.github.tabilzad.inspektor") version "0.8.7-alpha"
}

swagger {
    documentation {
        generateRequestSchemas = true
        hideTransientFields = true
        hidePrivateAndInternalFields = true
        deriveFieldRequirementFromTypeNullability = true
        info {
            title = "Arkivporten API"
            description = "API for document management and archiving in Arkivporten"
            version = "0.0.1"
            contact {
                name = "NAV Team Esyfo"
                url = "https://github.com/navikt/arkivporten"
            }
        }
    }

    pluginOptions {
        format = "yaml" // or json
    }
}

group = "no.nav.syfo"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
}
dependencies {
    implementation(libs.datafaker)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.config.yaml)
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-apache-jvm")
    implementation("io.ktor:ktor-serialization-jackson")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-call-id")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-status-pages")
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger)
    implementation(libs.logstash)
    implementation(libs.jackson.datatype.jsr310)
    // Inspektor Swagger UI
    implementation("io.github.tabilzad:ktor-docs-plugin-ui:0.8.7-alpha")
    // Database
    implementation(libs.bundles.database)
    // Metrics and Prometheus
    implementation(libs.ktor.server.micrometer)
    implementation(libs.micrometer.prometheus)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.bundles.testcontainers) // Will want this eventually
}
application {
    mainClass.set("no.nav.syfo.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "no.nav.syfo.ApplicationKt"
    }

    register("printVersion") {
        doLast {
            println(project.version)
        }
    }

    shadowJar {
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        mergeServiceFiles()
        archiveFileName.set("app.jar")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    test {
        useJUnitPlatform()
    }
}
