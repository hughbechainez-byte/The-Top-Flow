# The Top Flow 24.2 GitHub Update Foundation

Date: 2026-06-29

## Decision

Do not publish 24.1 through the app updater yet. It is a strong Compose note-taking foundation, but it is not full feature parity with 24.0 because recording, playback, settings, and update UI still need Compose ports.

The existing updater also cannot expose 24.1 only to users below 24.0. Its filter is only `versionCode > installedVersionCode`, so publishing 24.1 would also offer it to 24.0 users.

## GitHub Source

- Repository: `https://github.com/hughbechainez-byte/The-Top-Flow`
- Primary branch: `main`
- Compatibility branch: `the-top-flow`
- Local remotes:
  - `origin`: GitHub
  - `gitlab`: old GitLab remote

## 24.2 Goals

1. Restore full feature parity in the Compose host before publishing beyond 24.0.
2. Move update delivery to GitHub as the durable source of truth.
3. Ensure every build from 24.2 onward is downloadable through the app.
4. Keep older installed builds able to discover 24.2 through the current JSONBlob bridge.
5. Add manifest gating so future builds can target update ranges safely.

## Appcast Plan

Use the GitHub raw appcast as the primary URL in 24.2:

`https://raw.githubusercontent.com/hughbechainez-byte/The-Top-Flow/main/appcast.json`

Keep JSONBlob as a temporary fallback until multiple real-device updates have passed:

`https://jsonblob.com/api/jsonBlob/019f13c7-dc3f-7cf2-bf88-038a846852bd`

Add these optional fields to each `versions[]` entry:

- `sha256`
- `sizeBytes`
- `publishedAt`
- `channel`
- `minSourceVersionCode`
- `maxSourceVersionCode`
- `mandatory`

Older clients will ignore unknown fields. 24.2+ should honor them.

## GitHub Releases Plan

For each version from 24.2 onward:

1. Build the signed release APK.
2. Create a Git tag, for example `v24.2`.
3. Create a GitHub Release for that tag.
4. Upload the APK asset as `the-top-flow-24.2.apk`.
5. Set the appcast `apkUrl` to:

`https://github.com/hughbechainez-byte/The-Top-Flow/releases/download/v24.2/the-top-flow-24.2.apk`

6. Include SHA-256 and byte size in the appcast.
7. Commit and push the appcast update to `main`.
8. Update JSONBlob only when the build should be visible to currently installed pre-24.2 clients.

## 24.2 Implementation Tasks

1. Port update check/download/install UI into the Compose host.
2. Add source-version gating in the appcast parser.
3. Add SHA-256 verification after APK download and before install intent.
4. Add a GitHub primary URL plus JSONBlob fallback in `BuildConfig`.
5. Add a release helper script that builds, hashes, copies, tags, and prepares the GitHub Release/appcast payload.
6. Add tests for manifest parsing, newest eligible update selection, range gating, and SHA mismatch rejection.
7. Validate update flow by installing 24.0, pointing JSONBlob to 24.2, and updating through the app.
8. Validate 24.2+ update flow by installing 24.2 and checking the GitHub raw appcast path.

## Acceptance Criteria

- 24.2 has note-taking, rhyme, recording, playback, settings, and update access in the Compose host.
- 24.0 can discover and install 24.2 through JSONBlob when Dave approves publication.
- 24.2 can discover later versions through the GitHub raw appcast.
- Each 24.2+ update has a GitHub Release asset, appcast entry, SHA-256, size, and release notes.
- The app rejects corrupted or mismatched APK downloads.
- JSONBlob is no longer the source of truth after 24.2, only a bridge for older installs.
