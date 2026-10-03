import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    id("com.android.library") version "8.10.0"
    `maven-publish`
    id("com.vanniktech.maven.publish.base") version "0.35.0"
}

group = providers.gradleProperty("UBUNTU_MAVEN_GROUP").get()
version = providers.gradleProperty("UBUNTU_IMAGE_VERSION").get()
// Central publishing is opt-in so local builds do not require credentials or signing keys.
if (providers.gradleProperty("MAVEN_CENTRAL_PUBLISH").getOrElse("false").toBoolean()) {
    configure<MavenPublishBaseExtension> {
        publishToMavenCentral(automaticRelease = true, validateDeployment = true)
        signAllPublications()
        coordinates(project.group.toString(), providers.gradleProperty("UBUNTU_ARTIFACT_ID").get(), project.version.toString())
    }
}

apply(from = rootProject.file("gradle/runtime-artifacts.gradle.kts"))

android {
    namespace = "ai.meteor.ubuntu.image"
    compileSdk = 36
    defaultConfig { minSdk = 28 }
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/image/assets"))
    androidResources.noCompress += "zst"
    publishing.singleVariant("release") { withSourcesJar() }
}
tasks.named("preBuild") { dependsOn("prepareRuntimeAssets") }
apply(from = rootProject.file("gradle/publish-ubuntu.gradle.kts"))

tasks.register<Exec>("buildRuntime") {
    group = "runtime"
    description = "Builds the Ubuntu image archive and descriptor."
    workingDir(rootDir)
    if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        commandLine("pwsh", "-NoProfile", "-File", "runtime/build-runtime.ps1")
    } else {
        commandLine("bash", "runtime/build-runtime.sh")
    }
}
