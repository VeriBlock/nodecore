
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

enum class VersioningScope {
    MAJOR,
    MINOR,
    PATCH
}

fun Any?.asVersioningScope() = VersioningScope.values().find {
    it.name.equals(toString(), ignoreCase = true)
} ?: VersioningScope.PATCH

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val rc: Int = 0,
    val dev: Int = 0,
    val branch: String? = null
) {
    override fun toString(): String = "$major.$minor.$patch" +
        (if (rc > 0) "-rc.$rc" else "") +
        (if (dev > 0) {
            (if (rc > 0) "." else "-") + "dev.$dev"
        } else "") +
        (branch?.let { ".$it" } ?: "")
}

fun GitMetadata.extractVersion(): Version {
    val major: Int
    val minor: Int
    val patch: Int
    val rc: Int
    if (lastTag != null) {
        val split1 = lastTag.drop(1).split("-")
        val split2 = split1[0].split(".")
        major = split2[0].toInt()
        minor = split2[1].toInt()
        patch = split2[2].toInt()
        rc = if (split1.size > 1) {
            split1[1].substringAfter('.').toInt()
        } else {
            0
        }
    } else {
        major = 0
        minor = 0
        patch = 1
        rc = 0
    }
    val branch = if (branch == "main" || branch == "master" || branch.isEmpty()) {
        null
    } else {
        branch.replace("-", ".")
    }
    return Version(major, minor, patch, rc, revisionNumberSinceLastTag, branch)
}

var computedVersion: Version? = null

fun Project.prettyVersion(): String {
    return version().toString()
}

fun Project.version(ignoreComputed: Boolean = false): Version {
    if (!ignoreComputed) {
        computedVersion?.let {
            return it
        }
    }

    val versioningScope = extra["versioning.scope"].asVersioningScope()
    val extractedVersion = gitMetadata().extractVersion()
    val version = when (versioningScope) {
        VersioningScope.MAJOR -> {
            if (extractedVersion.rc > 0 && extractedVersion.minor == 0 && extractedVersion.patch == 0) {
                extractedVersion
            } else {
                extractedVersion.copy(major = extractedVersion.major + 1, minor = 0, patch = 0, rc = 0)
            }
        }
        VersioningScope.MINOR -> {
            if (extractedVersion.rc > 0 && extractedVersion.patch == 0) {
                extractedVersion
            } else {
                extractedVersion.copy(minor = extractedVersion.minor + 1, patch = 0, rc = 0)
            }
        }
        VersioningScope.PATCH -> {
            if (extractedVersion.rc > 0) {
                extractedVersion
            } else {
                extractedVersion.copy(patch = extractedVersion.patch + 1, rc = 0)
            }
        }
    }

    computedVersion = version
    return version
}

fun Version.flatten(scope: VersioningScope) = when (scope) {
    VersioningScope.MAJOR -> copy(minor = 0, patch = 0, rc = 0, dev = 0, branch = null)
    VersioningScope.MINOR -> copy(patch = 0, rc = 0, dev = 0, branch = null)
    VersioningScope.PATCH -> copy(rc = 0, dev = 0, branch = null)
}
fun Version.releaseCandidate() = copy(rc = rc + 1, dev = 0, branch = null)

fun Project.updateVersion(update: Version.(VersioningScope) -> Version) {
    val versioningScope = extra["versioning.scope"].asVersioningScope()
    computedVersion = version(ignoreComputed = true).update(versioningScope)
}

fun Project.pushVersionTag() {
    val version = version()
    if (version.dev > 0 || version.branch != null) {
        error("Attempting to push a non-incremented tag!")
    }
    val git = "git -C $rootDir "
    val versionTag = "v$version"
    (git + "tag -a $versionTag -m $versionTag").execute(rootDir)
    (git + "push origin $versionTag").execute(rootDir)
}
