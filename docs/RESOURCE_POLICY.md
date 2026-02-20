# Resource Policy

## Goals
- Keep APK/AAB size stable as assets grow.
- Avoid hardcoded media lists inside large Activity classes.
- Track the heaviest resources before each release.

## Applied in this refactor
- `MusicPlayActivity` local track metadata was moved to `LocalMusicCatalog`.
- Release build now enables `minifyEnabled` + `shrinkResources` in `app/build.gradle`.
- Added `scripts/audit_resources.ps1` to report largest files in `res/`.

## Workflow
1. Run `.\scripts\audit_resources.ps1` from project root.
2. If new media files are large, decide:
   - keep local for offline core UX, or
   - stream/download from server for optional content.
3. Keep naming consistent by feature prefix (`music_`, `chat_`, `walk_`, etc.).
