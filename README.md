# android-ubuntu-image

Android asset library containing a versioned Ubuntu 24.04 ARM64 root filesystem.
The image includes bash, Python, git, curl, an SSH client and ripgrep. It can be
used by any host capable of installing and running a Linux ARM64 filesystem.

Maven coordinate: `io.github.meteor149:ubuntu-image:24.04-1`.

## Build

Building the AAR requires JDK 21 and Android SDK 36. Building the image additionally
requires Docker BuildKit with ARM64 emulation and Node.js 24 for descriptor tooling.
Node.js is a build-machine requirement for metadata generation.

```bash
./gradlew buildRuntime
node --test tools/generate-runtime-manifest.test.mjs
./gradlew assembleRelease
```

The image recipe lives in `runtime/rootfs/Containerfile`, with its Ubuntu source
and image version in `runtime/versions.env`. Windows uses the PowerShell scripts;
Linux uses the Bash scripts. Archives and descriptors are generated into ignored
`runtime/dist` and are not committed. Existing artifacts can be supplied with
`-PUBUNTU_IMAGE_DIST=/absolute/artifact/path`.

The AAR contains only `assets/runtime/ubuntu-arm64.tar.zst` and
`assets/runtime/ubuntu-image-manifest.json`. The descriptor records the archive's
SHA-256, compressed size, minimum free space, Linux architecture, image version
and Ubuntu source. No host code, Android permissions or native launcher is bundled.
A diagnostic AAR can be built with an unavailable descriptor, but cannot be published.

## Publication

Versions and Maven coordinates are configured in `gradle.properties`.

```bash
./gradlew publish                 # build/maven-repository
./gradlew publishToMavenLocal
./gradlew publish -PUBUNTU_MAVEN_URL=https://your-repository.example/releases
```

Remote repositories use `UBUNTU_MAVEN_USERNAME` and `UBUNTU_MAVEN_PASSWORD`.
The publication includes an AAR, sources, documentation, POM and Gradle metadata.
Publishing refuses unavailable or checksum-invalid artifacts.

The Maven Central workflow runs on manual dispatch or a published GitHub Release.
Release tags must match `v<version>`. It uses `MAVEN_CENTRAL_USERNAME`,
`MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY_ID`, `SIGNING_PASSWORD` and `GPG_KEY_CONTENT`.
Keys are loaded in memory. The workflow uploads, validates and releases a signed
Central deployment; ordinary source pushes only run the build workflow.

For direct Central publication, provide the corresponding
`ORG_GRADLE_PROJECT_mavenCentral*` and `ORG_GRADLE_PROJECT_signingInMemory*`
environment variables, then run:

```bash
./gradlew publishAndReleaseToMavenCentral -PMAVEN_CENTRAL_PUBLISH=true --no-configuration-cache
```

## Integration

See [asset layout and image contract](docs/integration.md). The image has no Maven
dependencies. The consuming app owns installation, execution and persistent data.

## License

Build and packaging sources use [Apache License 2.0](LICENSE). Ubuntu packages
retain their upstream licenses, including the notices inside the image.
