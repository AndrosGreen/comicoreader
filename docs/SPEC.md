# Comic Reader — Project Spec

A local, offline comic reader for Android, focused on reading comics and organizing them into collections. Personal project first, with the option to publish later (GitHub / F-Droid / Google Play). iOS/iPadOS is a possible future target, so the code stays structured to allow it.

## Principles

- **Incremental.** Build in small milestones. Each milestone must leave the app in a working, buildable state.
- **Local only.** No network access, no analytics, no accounts. All data stays on the device.
- **Simple first.** Prefer the simplest solution that works; add complexity only when a real need appears.
- **KMP-ready.** Keep domain models and business logic free of Android framework types where reasonable, so they can later move to a Kotlin Multiplatform shared module. Platform-specific features (file picking, PDF rendering) sit behind interfaces.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 (dynamic color enabled) |
| Min SDK | 31 (Android 12) |
| Target / compile SDK | Latest stable |
| Build | Gradle with Kotlin DSL, version catalog (`libs.versions.toml`) |
| Database | Room (with KSP) |
| Settings | DataStore (Preferences) |
| Dependency injection | Koin |
| Navigation | Navigation Compose |
| Image loading / thumbnails | Coil 3 |
| Zoom & pan in reader | Telephoto (`zoomable-image-coil3`) |
| Async | Kotlin Coroutines + Flow |
| Archive reading | `java.util.zip.ZipFile` (CBZ) |

New dependencies must not be added without discussion. Prefer permissively licensed libraries (Apache 2.0, MIT, BSD).

## Architecture

Single `app` module for now, organized by layer:

```
com.<name>.comicreader
├── domain/        # Models (Comic, Collection), repository interfaces, use cases. No Android imports.
├── data/
│   ├── db/        # Room entities, DAOs, database
│   ├── files/     # Import, storage paths, thumbnail generation
│   ├── archive/   # Comic archive readers (CbzReader behind a ComicArchiveReader interface)
│   └── repository/ # Repository implementations
├── ui/
│   ├── library/   # Library grid screen
│   ├── collections/ # Collection list & detail screens
│   ├── reader/    # Reader screen
│   ├── components/ # Shared composables
│   └── theme/
└── di/            # Koin modules
```

Pattern: MVVM. Each screen has a `ViewModel` exposing a `StateFlow<UiState>`; composables are stateless where practical.

## Data model

**Comic**
- `id` (Long, auto)
- `title` (String, defaults to file name without extension)
- `fileName` (String, name of the stored file in app storage)
- `format` (enum: CBZ; more later)
- `pageCount` (Int)
- `coverPath` (String?, path to cached cover thumbnail)
- `lastReadPage` (Int, 0-based, default 0)
- `addedAt` (Long, epoch millis)
- `lastOpenedAt` (Long?, epoch millis)

**Collection**
- `id` (Long, auto)
- `name` (String, unique)
- `createdAt` (Long)

**CollectionComic** (join table, many-to-many)
- `collectionId`, `comicId` (composite primary key, foreign keys with cascade delete)
- `position` (Int, order within the collection)

Deleting a comic removes it from all collections. Deleting a collection does **not** delete its comics.

## File handling

- Files are picked with the Storage Access Framework (`ACTION_OPEN_DOCUMENT`, multi-select allowed). No broad storage permissions (never `MANAGE_EXTERNAL_STORAGE`).
- On import, the file is **copied** into app-private storage: `filesDir/comics/<uuid>.cbz`. Copying gives reliable random access via `ZipFile`.
- Cover thumbnails are generated at import from the first page, downscaled, and stored in `filesDir/covers/<comicId>.webp`.
- Deleting a comic deletes its stored file and cover.
- Import of an invalid or unreadable file fails gracefully with a user-visible message; nothing is left half-imported.

## CBZ reading rules

- Page images: entries ending in `.jpg`, `.jpeg`, `.png`, `.webp`, `.avif`, `.gif` (case-insensitive).
- Ignore directories, hidden files, `__MACOSX/` entries, and non-image files (e.g. `ComicInfo.xml` is ignored for now).
- Page order: **natural sort** of full entry paths (so `page2` comes before `page10`).
- Pages are decoded on demand and downsampled to screen size. Never load all pages into memory.

## MVP milestones

Each milestone is done when its acceptance criteria are met, the app builds, and existing tests pass.

### M0 — Project setup
- Project created (Empty Activity, Compose), minSdk 31, version catalog in use.
- Dependencies from the tech stack added; Koin initialized in an `Application` class.
- App launches to a placeholder screen.

### M1 — Data layer
- Room entities, DAOs, and database for Comic, Collection, CollectionComic.
- Repository interfaces in `domain`, implementations in `data`.
- Unit tests for DAOs (in-memory database).

### M2 — Import & library
- `CbzReader` implementing `ComicArchiveReader` (list pages, open page stream), with unit tests including natural sort and ignored entries.
- Import flow: pick one or more CBZ files → copy → read page count → generate cover → save to DB.
- Library screen: grid of covers with titles, sorted by most recently added. Empty state with an import button.
- Delete a comic (with confirmation).

### M3 — Collections
- Collections screen: list, create, rename, delete (with confirmation).
- Collection detail screen: grid of its comics.
- Add a comic to one or more collections (from the library, via long-press or menu). Remove a comic from a collection.

### M4 — Reader
- Open a comic from library or collection.
- Horizontal paging (left-to-right), one page at a time.
- Pinch to zoom, double-tap to zoom, pan when zoomed.
- Tap center to toggle a minimal overlay (title, page X of Y, back button); immersive full-screen otherwise.

### M5 — Reading progress
- Current page is saved while reading and restored when reopening.
- Library shows a simple progress indicator per comic.
- `lastOpenedAt` updated on open.

## Out of scope for MVP (backlog)

Roughly in priority order, not committed:

- PDF support (`PdfRenderer`)
- Right-to-left reading mode (manga)
- Vertical continuous scroll (webtoons)
- Double-page spreads on tablets / landscape
- Reorder comics within a collection
- Search and sort options in the library
- `ComicInfo.xml` metadata (series, issue number, etc.)
- CBR / CB7 support
- Linking external folders instead of copying (SAF persistent URIs)
- Backup / export of library data
- iOS / iPadOS via Kotlin Multiplatform

## Decision log

Record significant decisions here (date — decision — reason).

- Native Kotlin + Compose, KMP-ready structure — Android is priority; keeps iOS possible without a rewrite.
- minSdk 31 — native AVIF/WebP decoding, modern storage model only, dynamic color.
- Copy files on import (not reference) — simpler and more reliable for MVP; linking folders is a later feature.
- CBZ only for MVP — trivial to support with standard library; CBR has library/licensing complexity.
- 2026-10-19 — Package / applicationId is `com.androsgreen.comicreader` — the template's `com.example.*` cannot be published to Google Play, and `applicationId` is not changeable after release.
- 2026-10-19 — No `org.jetbrains.kotlin.android` plugin — AGP 9 has built-in Kotlin support and actively rejects the standalone plugin. Kotlin is still pinned to 2.4.21 through the `kotlin-compose` plugin alias in the version catalog.
- 2026-10-19 — Views libraries (`appcompat`, `com.google.android.material`) dropped — the app is Compose-only; the XML theme is a bare `android:Theme.Material.Light.NoActionBar` and all theming lives in `ui/theme`.
- 2026-10-19 — Toolchain pinned at M0: AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.21, KSP 2.3.12, Compose BOM 2026.09.00, compileSdk/targetSdk 37, Java 11 target.
- 2026-10-25 — `ComicFormat` stored as its enum name (TEXT) using Room's built-in enum support, no TypeConverter — readable in the DB and safe to extend with new formats; enum constants must never be renamed (ordinal storage was rejected as fragile to reordering).
- 2026-10-25 — Room schemas exported (`exportSchema = true`) to `app/schemas/` via the KSP `room.schemaLocation` arg and committed — they record what is installed on devices, feed `MigrationTestHelper` (wired as androidTest assets), and make schema changes visible in review. KSP arg chosen over the `androidx.room` Gradle plugin to avoid a new dependency.
- 2026-10-25 — Migration policy: until the first public release the v1 schema may change in place (reinstall / clear data). After release, every schema change bumps the DB version, ships a `Migration` with a `MigrationTestHelper` test, and commits the new schema JSON. No `fallbackToDestructiveMigration` — silently wiping a user's library is unacceptable.
- 2026-10-26 — Collection names are unique **case-sensitively** ("Batman" and "batman" may coexist); lists sort by name with `COLLATE NOCASE` for display only. A duplicate name makes `CollectionDao.insert`/`rename` throw (`ABORT`) and the repository maps it to a user-facing error — `REPLACE` was rejected because it deletes the existing collection and cascades its memberships.
