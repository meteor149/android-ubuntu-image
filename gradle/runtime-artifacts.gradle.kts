import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.security.MessageDigest

val dist = providers.gradleProperty("UBUNTU_IMAGE_DIST")
    .map { rootProject.file(it) }.getOrElse(rootProject.file("runtime/dist"))
val fallback = rootProject.file("runtime/manifest/unavailable.json")
val assetsOutput = layout.buildDirectory.dir("generated/image/assets")

tasks.register("prepareRuntimeAssets") {
    group = "image"
    description = "Validates and stages the Ubuntu image archive and descriptor."
    inputs.files(fileTree(dist))
    inputs.file(fallback)
    outputs.dir(assetsOutput)
    doLast {
        val assets = assetsOutput.get().asFile
        delete(assets)
        val target = assets.resolve("runtime").apply { mkdirs() }
        val manifest = dist.resolve("image-manifest.json").takeIf { it.isFile } ?: fallback
        val document = JsonSlurper().parse(manifest) as Map<*, *>
        check(document["schemaVersion"] == 1) { "Unsupported image descriptor schema" }
        check(document.keys.all { it in setOf("schemaVersion", "available", "imageVersion", "architecture", "archive", "source") }) {
            "Unexpected image descriptor fields"
        }
        check(document["architecture"] == "arm64") { "Unsupported image architecture" }
        check((document["imageVersion"] as? String)?.isNotBlank() == true) { "Missing image version" }
        if (document["available"] == true) {
            val archive = document["archive"] as Map<*, *>
            val name = archive["file"] as String
            check(name.matches(Regex("[A-Za-z0-9._-]+")) && name != "." && name != "..") { "Invalid image filename" }
            val expected = archive["sha256"] as String
            check(expected.matches(Regex("[a-fA-F0-9]{64}"))) { "Invalid image checksum" }
            val file = dist.resolve(name)
            check(file.isFile && file.length() > 0) { "Image archive is missing or empty" }
            check(file.length() == (archive["compressedBytes"] as Number).toLong()) { "Image archive size mismatch" }
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            check(digest.digest().joinToString("") { "%02x".format(it) }.equals(expected, ignoreCase = true)) {
                "Image archive checksum mismatch"
            }
            file.copyTo(target.resolve(name), overwrite = true)
        }
        target.resolve("ubuntu-image-manifest.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(document)) + "\n")
    }
}

tasks.register("validatePublicationArtifacts") {
    dependsOn("prepareRuntimeAssets")
    doLast {
        val document = JsonSlurper().parse(assetsOutput.get().file("runtime/ubuntu-image-manifest.json").asFile) as Map<*, *>
        check(document["available"] == true) { "Build or supply a verified image before publishing" }
    }
}
