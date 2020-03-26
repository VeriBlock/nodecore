import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.task
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.testing.jacoco.tasks.JacocoReport

fun Project.prettyVersion(): String {
    var version = rootProject.version.toString()
    if (version.contains("+")) {
        version = version.substring(0, version.length - 8).replace("+", ".")
        if (version.endsWith("develop")) {
            version = version.substring(0, version.length - 8)
        }
    }
    return version
}

val Project.sourceSets: SourceSetContainer
    get() = extensions.getByName("sourceSets") as SourceSetContainer

fun Project.setupJar(
    specTitle: String,
    packagePath: String
) {
    tasks.named<Jar>("jar") {
        archiveFileName.set("${project.name}-${prettyVersion()}.jar")
        manifest.attributes.apply {
            set("Name", packagePath.replace('.', '/'))
            set("Specification-Title", specTitle)
            set("Specification-Version", prettyVersion())
            set("Specification-Vendor", "Xenios SEZC")
            set("Implementation-Title", packagePath)
            set("Implementation-Version", prettyVersion())
            set("Implementation-Vendor", "Xenios SEZC")
        }
    }
}

fun Project.setupSourcesJar(): Jar {
    return tasks.create<Jar>("sourcesJar") {
        getArchiveClassifier().set("sources")
        from(sourceSets["main"].allSource)
    }
}

fun Project.publish(artifactName: String, sourcesJar: Jar) {
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            register<MavenPublication>("mavenJava") {
                groupId = "veriblock"
                artifactId = artifactName
                version = prettyVersion()

                from(components["java"])
                artifact(sourcesJar) {
                    classifier = "sources"
                }
            }
        }
    }
}

fun Project.customTests(name: String, config: Test.() -> Unit = {}) {
    // Get extensions
    val idea = extensions.getByName("idea") as? IdeaModel

    sourceSets.create("test-$name") {
        java {
            compileClasspath += sourceSets.getByName("main").output + configurations.named("testRuntimeClasspath").get()
            runtimeClasspath += output + compileClasspath
        }
    }

    task<Test>("test${name.split("-").joinToString("") { it.capitalize() }}") {
        description = "Run $name tests"
        group = "verification"
        testClassesDirs = sourceSets["test-$name"].output.classesDirs
        classpath = sourceSets["test-$name"].runtimeClasspath
        outputs.upToDateWhen { false }

        config()
    }

    idea?.apply {
        module {
            testSourceDirs = testSourceDirs + sourceSets["test-$name"].java.srcDirs
            testSourceDirs = testSourceDirs + sourceSets["test-$name"].resources.srcDirs
        }
    }
}

fun Project.setupJacoco() {
    apply(plugin = "jacoco")

    tasks.named<JacocoReport>("jacocoTestReport") {
        additionalSourceDirs.from(files(sourceSets.getByName("main").allJava.srcDirs))
        sourceDirectories.from(files(sourceSets.getByName("main").allSource.srcDirs))
        classDirectories.from(files(sourceSets.getByName("main").output))

        reports {
            xml.isEnabled = true
            html.isEnabled = false
            csv.isEnabled = false
        }
    }
}
