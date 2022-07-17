import kotlin.io.path.readText

job("Build") {
    gradlew("openjdk:11", "build")
}

job("Publish") {
    startOn {
        gitPush { enabled = false }
    }
    container("openjdk:11") {
        env["SPACE_USER"] = Secrets("space_user")
        env["SPACE_TOKEN"] = Secrets("space_token")
        kotlinScript { api ->

            val spaceUser = System.getenv("SPACE_USER")
            val spaceToken = System.getenv("SPACE_TOKEN")

            // write version to the build directory
            api.gradlew("version")

            //read version from build file
            val version = java.nio.file.Path.of("build/project-version.txt").readText()

            api.space().projects.automation.deployments.start(
                project = api.projectIdentifier(),
                targetIdentifier = TargetIdentifier.Key("maps-kt"),
                version = version,
                // automatically update deployment status based on a status of a job
                syncWithAutomationJob = true
            )
            api.gradlew(
                "publishAllPublicationsToSpaceRepository",
                "-Ppublishing.space.user=\"$spaceUser\"",
                "-Ppublishing.space.token=\"$spaceToken\"",
            )
        }
    }
}