import org.gradle.api.Project

val Project.Versions: Versions
    get() = Versions(this)

val Project.Dependencies: Dependencies
    get() = Dependencies(this)

class Versions(project: Project) {

    val kotlin = "1.5.10"
    val trikotFoundation = project.property("trikot_foundation_version")
    val trikotStreams = project.property("trikot_streams_version")
    val trikotHttp = project.property("trikot_http_version")
    val trikotKword = project.property("trikot_kword_version")

    val androidGradlePlugin = "7.0.0-beta03"
    val jetpackCompose = "1.0.0-beta08"
    val googleAccompanist = "0.10.0"
}

class Dependencies(project: Project) {
    val trikotFoundation = "com.mirego.trikot:trikotFoundation:${project.Versions.trikotFoundation}"
    val trikotStreams = "com.mirego.trikot:streams:${project.Versions.trikotStreams}"
    val trikotHttp = "com.mirego.trikot:http:${project.Versions.trikotHttp}"
    val trikotKword = "com.mirego.trikot:kword:${project.Versions.trikotKword}"
}
