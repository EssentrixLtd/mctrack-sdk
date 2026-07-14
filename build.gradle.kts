plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "net.mctrack"
version = providers.gradleProperty("sdkVersion").orElse("1.0.0-SNAPSHOT").get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.code.gson:gson:2.10.1")
    implementation("org.yaml:snakeyaml:2.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

val generatedBuildConstantsDir = layout.buildDirectory.dir("generated/sources/buildConstants/java/main")

sourceSets {
    named("main") {
        java.srcDir(generatedBuildConstantsDir)
    }
}

val generateBuildConstants = tasks.register("generateBuildConstants") {
    val versionString = project.version.toString()
    inputs.property("version", versionString)
    outputs.dir(generatedBuildConstantsDir)

    doLast {
        val outputDir = generatedBuildConstantsDir.get().asFile.resolve("com/mctrack/common")
        outputDir.mkdirs()

        outputDir.resolve("BuildConstants.java").writeText(
            """
            package com.mctrack.common;

            public final class BuildConstants {
                public static final String VERSION = "$versionString";

                private BuildConstants() {
                }
            }
            """.trimIndent()
        )
    }
}

tasks.named("compileJava") {
    dependsOn(generateBuildConstants)
}

tasks.named("sourcesJar") {
    dependsOn(generateBuildConstants)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("MCTrack SDK")
                description.set("Java SDK for sending session, payment, profile, and custom events to MCTrack.")
                url.set("https://mctrack.net")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("essentrix")
                        name.set("Essentrix Ltd")
                        url.set("https://essentrix.ltd")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/EssentrixLtd/mctrack-sdk.git")
                    developerConnection.set("scm:git:ssh://git@github.com/EssentrixLtd/mctrack-sdk.git")
                    url.set("https://github.com/EssentrixLtd/mctrack-sdk")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl)
            credentials {
                username = providers.gradleProperty("ossrhUsername").orNull
                    ?: System.getenv("OSSRH_USERNAME")
                password = providers.gradleProperty("ossrhPassword").orNull
                    ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey = providers.gradleProperty("signingKey").orNull
        ?: System.getenv("SIGNING_KEY")
    val signingPassword = providers.gradleProperty("signingPassword").orNull
        ?: System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}
