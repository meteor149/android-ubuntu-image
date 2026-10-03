import { createHash } from 'node:crypto'
import { createReadStream } from 'node:fs'
import { readFile, stat, writeFile } from 'node:fs/promises'
import path from 'node:path'

if (process.argv.length > 3) throw new Error('Only an artifact directory can be supplied')
const root = path.resolve(import.meta.dirname, '..')
const dist = path.resolve(process.argv[2] ?? path.join(root, 'runtime/dist'))
const versions = Object.fromEntries((await readFile(path.join(root, 'runtime/versions.env'), 'utf8'))
  .split(/\r?\n/u).map(line => line.trim()).filter(line => line && !line.startsWith('#'))
  .map(line => [line.slice(0, line.indexOf('=')), line.slice(line.indexOf('=') + 1)]))
for (const name of ['UBUNTU_IMAGE', 'IMAGE_VERSION']) {
  if (!versions[name]) throw new Error(`Missing ${name} in runtime/versions.env`)
}
const file = 'ubuntu-arm64.tar.zst'
const archive = path.join(dist, file)
const compressedBytes = (await stat(archive)).size
if (compressedBytes === 0) throw new Error('The image archive is empty')
const hash = createHash('sha256')
for await (const chunk of createReadStream(archive)) hash.update(chunk)
const manifest = {
  schemaVersion: 1,
  available: true,
  imageVersion: versions.IMAGE_VERSION,
  architecture: 'arm64',
  archive: { file, sha256: hash.digest('hex'), compressedBytes,
    minimumFreeBytes: Math.max(2_147_483_648, compressedBytes * 5) },
  source: { ubuntuImage: versions.UBUNTU_IMAGE },
}
const destination = path.join(dist, 'image-manifest.json')
await writeFile(destination, JSON.stringify(manifest, null, 2) + '\n')
process.stdout.write(`image manifest: ${destination}\n`)
