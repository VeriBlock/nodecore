import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task

data class GitMetadata(
    val commitHash: String,
    val branch: String,
    val revisionNumber: Int,
    val revisionNumberSinceLastTag: Int,
    val lastTag: String? = null
)

var computedGitMetadata: GitMetadata? = null

fun Project.gitMetadata(): GitMetadata {
    computedGitMetadata?.let {
        return it
    }

    val git = "git -C $rootDir "
    val lastTag = (git + "describe --abbrev=0").execute(rootDir).ifEmpty { null }
    val branch = (git + "symbolic-ref --short HEAD").execute(rootDir)
    val revision = (git + "rev-list --count HEAD").execute(rootDir).toIntOrNull() ?: 0
    val revisionSinceLastTag = if (lastTag != null) {
        (git + "rev-list --count $lastTag..HEAD").execute(rootDir).toIntOrNull() ?: 0
    } else {
        revision
    }
    val hash = (git + "rev-parse --short HEAD").execute(rootDir)
    val metaData = GitMetadata(hash, branch, revision, revisionSinceLastTag, lastTag)

    computedGitMetadata = metaData
    return metaData
}

fun Project.gitMetadataJson(): String {
    val metaData = gitMetadata()
    return """
        {
          "version": "${project.prettyVersion()}",
          "gitCommitHash": "${metaData.commitHash}",
          "gitBranch": "${metaData.branch}",
          "gitRevisionNumber": "${metaData.revisionNumber}"
        }
    """.trimIndent()
}

fun Task.generateProjectMetadata(generatedResourcesDir: File) {
    outputs.dir(generatedResourcesDir)
    outputs.upToDateWhen {
        val file = File(generatedResourcesDir, "project-metadata.json")
        if (file.exists()) {
            file.readText() == project.gitMetadataJson()
        } else {
            false
        }
    }
    doFirst {
        if (!generatedResourcesDir.exists()) {
            generatedResourcesDir.mkdirs()
        }
        File(generatedResourcesDir, "project-metadata.json").writeText(project.gitMetadataJson())
    }
}
