plugins {
    base
}

val ktorVersion by extra("2.0.3")

tasks.create("version") {
    group = "publishing"
    val versionFile = project.buildDir.resolve("project-version.txt")
    outputs.file(versionFile)
    doLast {
        versionFile.createNewFile()
        versionFile.writeText(project.version.toString())
        println(project.version)
    }
}

subprojects {
    group = "center.sciprog"
    version = "0.1.0-SNAPSHOT"

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins.withId("maven-publish") {

        configure<PublishingExtension> {
            val vcs = "https://github.com/mipt-npm/maps-kt"

            // Process each publication we have in this project
            publications {
                withType<MavenPublication> {
                    pom {
                        name.set(project.name)
                        description.set(project.description)
                        url.set(vcs)

                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }

                        developers {
                            developer {
                                id.set("SPC")
                                name.set("Scientific programming centre")
                                organization.set("MIPT")
                                organizationUrl.set("https://sciprog.center/")
                            }
                        }

                        scm {
                            url.set(vcs)
                            tag.set(project.version.toString())
                        }
                    }
                }
            }

            val spaceRepo = "https://maven.pkg.jetbrains.space/mipt-npm/p/sci/maven"
            val spaceUser: String? = project.findProperty("publishing.space.user") as? String
            val spaceToken: String? = project.findProperty("publishing.space.token") as? String

            if (spaceUser != null && spaceToken != null) {
                project.logger.info("Adding mipt-npm Space publishing to project [${project.name}]")

                repositories.maven {
                    name = "space"
                    url = uri(spaceRepo)

                    credentials {
                        username = spaceUser
                        password = spaceToken
                    }
                }
            }
        }
    }
}



