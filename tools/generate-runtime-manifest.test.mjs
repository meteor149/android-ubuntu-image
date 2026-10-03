import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { mkdtemp, writeFile, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'

const generator = path.join(import.meta.dirname, 'generate-runtime-manifest.mjs')

test('an image manifest only describes the archive, architecture, version and source', async () => {
  const dist = await mkdtemp(path.join(tmpdir(), 'ubuntu-image-'))
  try {
    const content = Buffer.from('fixture rootfs')
    await writeFile(path.join(dist, 'ubuntu-arm64.tar.zst'), content)
    execFileSync(process.execPath, [generator, dist])
    const manifest = JSON.parse(await readFile(path.join(dist, 'image-manifest.json'), 'utf8'))
    assert.deepEqual(Object.keys(manifest).sort(),
      ['schemaVersion', 'available', 'imageVersion', 'architecture', 'archive', 'source'].sort())
    assert.equal(manifest.schemaVersion, 1)
    assert.equal(manifest.architecture, 'arm64')
    assert.equal(manifest.archive.compressedBytes, content.length)
    assert.equal(manifest.archive.sha256, createHash('sha256').update(content).digest('hex'))
    assert.deepEqual(Object.keys(manifest.source), ['ubuntuImage'])
  } finally {
    await rm(dist, { recursive: true, force: true })
  }
})

test('an empty image cannot be marked available', async () => {
  const dist = await mkdtemp(path.join(tmpdir(), 'ubuntu-image-'))
  try {
    await writeFile(path.join(dist, 'ubuntu-arm64.tar.zst'), '')
    assert.throws(() => execFileSync(process.execPath, [generator, dist], { stdio: 'pipe' }))
  } finally {
    await rm(dist, { recursive: true, force: true })
  }
})
