
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2018_2.project
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_2.version

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2019.1"

project {

    buildType(Snapshot)
    buildType(ReleaseCandidate)
    buildType(FinalRelease)

    params {
        param("env.GIT_BRANCH", "unused") // TODO remove once we get rid of this parameter on the root project
    }
}

object Snapshot : Build(
    name = "Snapshot",
    gradleTask = "devSnapshot",
    branchFilter = """
        +:refs/heads/*
        -:refs/heads/master
        -:refs/heads/release/*
    """.trimIndent(),
    artifactoryRepoKey = "libs-snapshot-local",
    isFinal = false
)

object ReleaseCandidate : Build(
    name = "Release Candidate",
    gradleTask = "candidate",
    branchFilter = "+:refs/heads/release/*",
    artifactoryRepoKey = "libs-release-local",
    isFinal = false
)

object FinalRelease : Build(
    name = "Final Release",
    gradleTask = "final",
    branchFilter = "+:refs/heads/master",
    artifactoryRepoKey = "libs-release-local",
    isFinal = true
)

abstract class Build(
    name: String,
    gradleTask: String,
    branchFilter: String?,
    artifactoryRepoKey: String,
    isFinal: Boolean
) : BuildType({
    this.name = name

    artifactRules = """
        nodecore-cli/build/distributions => nodecore-cli
        nodecore-miners-pop/build/distributions => nodecore-miners-pop
        nodecore-miners-pow/build/distributions => nodecore-miners-pow
    """.trimIndent()

    params {
        param("system.artifactory_repoKey", artifactoryRepoKey)
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            tasks = "$gradleTask clean build installDist veriblock-core:artifactoryPublish nodecore-grpc:artifactoryPublish nodecore-ucp:artifactoryPublish"
            buildFile = "build.gradle"
            gradleParams = "%gradle.cli.params%"
        }
        fun deployDocker(stepName: String, dockerfilePath: String, imageName: String) {
            dockerCommand {
                this.name = "Build $stepName Docker Image"
                commandType = build {
                    source = path {
                        path = dockerfilePath
                    }
                    namesAndTags = """
					    ${if (isFinal) imageName else ""}
					    $imageName:%build.number%
				    """.trimIndent()
                    commandArgs = "--pull"
                }
            }
            dockerCommand {
                this.name = "Push $stepName Docker Image"
                commandType = push {
                    namesAndTags = """
					    ${if (isFinal) imageName else ""}
					    $imageName:%build.number%
				    """.trimIndent()
                    removeImageAfterPush = false
                }
                param("dockerfile.path", dockerfilePath)
            }
        }
        deployDocker("Nodecore CLI","nodecore-cli/Dockerfile", "veriblock.azurecr.io/nodecore-cli")
        deployDocker("Nodecore PoP Miner","nodecore-miners-pop/Dockerfile", "veriblock.azurecr.io/nodecore-pop-miner")
        deployDocker("Nodecore PoW Miner","nodecore-miners-pow/Dockerfile", "veriblock.azurecr.io/nodecore-pow-miner")
    }

    if (branchFilter != null) {
        triggers {
            vcs {
                enabled = true
                this.branchFilter = branchFilter
                perCheckinTriggering = true
                groupCheckinsByCommitter = true
                enableQueueOptimization = false
            }
        }
    }

    features {
        sshAgent {
            teamcitySshKey = "github"
            param("secure:passphrase", "credentialsJSON:d9d1fd02-aa80-4456-9f17-43a2354af355")
        }
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_7"
            }
        }
    }
})
