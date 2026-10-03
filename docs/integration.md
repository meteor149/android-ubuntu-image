# Image integration

Add the image asset library to the Android app:

```kotlin
repositories { mavenCentral() }
dependencies {
    implementation("io.github.meteor149:ubuntu-image:24.04-1")
}
android {
    defaultConfig { minSdk = 28 }
    androidResources.noCompress += "zst"
}
```

The AAR has no transitive Maven dependencies or permissions. `noCompress` preserves
the already-compressed archive in the APK without additional ZIP compression.
The host implements storage, archive extraction and execution, or chooses separate
libraries for those responsibilities.

## Asset contract

Read `runtime/ubuntu-image-manifest.json` through `Context.assets`. Schema 1 has
these fields:

```json
{
  "schemaVersion": 1,
  "available": true,
  "imageVersion": "ubuntu-24.04-1",
  "architecture": "arm64",
  "archive": {
    "file": "ubuntu-arm64.tar.zst",
    "sha256": "<64 hexadecimal characters>",
    "compressedBytes": 48870264,
    "minimumFreeBytes": 2147483648
  },
  "source": { "ubuntuImage": "ubuntu:24.04" }
}
```

Sizes and checksum above are illustrative; always use the generated descriptor.
`architecture` names the Linux CPU architecture. `imageVersion` identifies the
filesystem content, independently of the Maven package version. An unavailable
descriptor sets `available=false` and `archive=null`.

The archive is located at `runtime/<archive.file>` and uses tar with Zstandard
compression. Before installation, validate the descriptor schema, compatible CPU
architecture, sufficient free space, compressed size and SHA-256. Extract into a
staging directory, preserve permissions and symbolic links, reject unsafe paths,
and replace the installed image only after a complete extraction. Retain user
configuration and workspaces separately from replaceable image versions.

## Content and updates

Ubuntu 24.04 is the base image. The package list lives in the Containerfile; it
includes bash, ca-certificates, curl, git, libstdc++6, openssh-client, python3 and
ripgrep. Image updates are built and published separately from any consuming app.
The image descriptor contains filesystem data only and defines no execution policy.
