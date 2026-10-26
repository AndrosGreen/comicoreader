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
| Navigation | Navigation Compose, type-safe routes (`kotlinx-serialization-core` + Kotlin serialization plugin) |
| Image loading / thumbnails | Coil 3 |
| Zoom & pan in reader | Telephoto (`zoomable-image-coil3`) |
| Async | Kotlin Coroutines + Flow |
| Archive reading | `java.util.zip.ZipFile` (CBZ) |
| Testing | JUnit 4, `kotlinx-coroutines-test`; JVM tests for pure logic and ViewModels (with fakes); instrumented tests for DAOs and repositories (in-memory Room) and for code that needs Android (`ContentResolver`, image decoding) |

New dependencies must not be added without discussion. Prefer permissively licensed libraries (Apache 2.0, MIT, BSD).

## Architecture

Single `app` module for now, organized by layer:

```
com.androsgreen.comicreader
├── domain/        # Models (Comic, Collection, CollectionSummary), repository and importer interfaces, result types, use cases. No Android imports.
├── data/
│   ├── db/        # Room entities, DAOs, database
│   ├── files/     # Importer, app-private file storage, cover generation
│   ├── archive/   # Comic archive readers (CbzReader behind a ComicArchiveReader interface)
│   └── repository/ # Repository implementations
├── ui/            # ComicReaderApp (nav host + bottom bar), Routes
│   ├── library/   # Library grid screen
│   ├── collections/ # Collection list & detail screens
│   ├── reader/    # Reader screen
│   ├── components/ # Shared composables (ComicGrid)
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
- `name` (String, unique, case-sensitive: "Batman" and "batman" may coexist)
- `createdAt` (Long)

**CollectionComic** (join table, many-to-many)
- `collectionId`, `comicId` (composite primary key, foreign keys with cascade delete)
- `position` (Int, 0-based order within the collection; a comic is appended at max + 1, and removals may leave gaps)

Deleting a comic removes it from all collections. Deleting a collection does **not** delete its comics.

## File handling

- Files are picked with the Storage Access Framework (`ACTION_OPEN_DOCUMENT`, multi-select allowed). No broad storage permissions (never `MANAGE_EXTERNAL_STORAGE`).
- On import, the file is **copied** into app-private storage: `filesDir/comics/<uuid>.cbz`. Copying gives reliable random access via `ZipFile`. The original is only read, never modified or deleted (the app only gets read access to it); an imported comic therefore takes space twice until the user deletes the original.
- Cover thumbnails are generated at import from the first page in reading order, fitted inside 600×900 px (never upscaled), encoded as WebP quality 80, and stored in `filesDir/covers/<uuid>.webp`, sharing the uuid of the comic's file.
- Deleting a comic deletes its library entry, its stored file and its cover — never the original.
- Import of an invalid or unreadable file fails gracefully with a user-visible message (can't read the file / not a CBZ / no pages / first page unreadable / unexpected error); nothing is left half-imported.

## CBZ reading rules

- Page images: entries ending in `.jpg`, `.jpeg`, `.png`, `.webp`, `.avif`, `.gif` (case-insensitive).
- Ignore directories, hidden files and folders (any path segment starting with `.`), anything under a `__MACOSX` folder, and non-image files (e.g. `ComicInfo.xml` is ignored for now). Both `/` and `\` are accepted as path separators.
- Page order: **natural sort** of full entry paths — digit runs compare by value (`page2` before `page10`), letters ignore case, and paths sort folder by folder (`ch2/` before `ch10/`).
- A corrupt or unreadable file is an error; an archive with no images opens with zero pages and is rejected by the importer.
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
- Instrumented tests for DAOs and repositories (in-memory database); JVM tests for mappers.
- Data layer wired into Koin.

### M2 — Import & library
- `CbzReader` implementing `ComicArchiveReader` (list pages, open page stream), with unit tests including natural sort and ignored entries.
- Import flow: pick one or more CBZ files → copy → read page count → generate cover → save to DB.
- Library screen: adaptive grid of covers with titles, sorted by most recently added. Empty state with an import button; import progress and failure messages.
- Delete a comic: long-press a cover, then confirm.

### M3 — Collections
- Bottom navigation with two tabs, Library and Collections; each tab keeps its own back stack.
- Collections screen: rows with the first comic's cover, name and comic count; create and rename through a name dialog (trimmed; blank and duplicate names rejected inline); rename/delete from a row's overflow menu; delete with confirmation (comics are kept).
- Collection detail screen: grid of its comics in the order they were added; long-press → confirm to remove a comic from the collection (it stays in the library); closes itself if the collection is deleted.
- Add a comic to collections from the library: long-press opens an action sheet (Add to collections, Delete); the checklist dialog pre-checks current memberships and saves only the changes. With no collections, it points to the Collections tab.

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
- Detect duplicate imports (importing the same file twice currently creates two entries)
- Startup cleanup of unreferenced files (a process death mid-copy can leave one behind)
- Compact dialog titles for very long comic titles (delete and remove-from-collection confirmations)
- Create a collection from inside the "Add to collections" dialog (deferred in M3 in favour of pointing to the Collections tab)
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
- 2026-10-26 — DAO tests are instrumented (`androidTest`, run with `./gradlew connectedAndroidTest`) against an in-memory Room database — real Android SQLite, no Robolectric. Only `kotlinx-coroutines-test` added (`runTest`); Flows are tested with `first()` and a `Channel` collector instead of Turbine, to keep test dependencies minimal.
- 2026-10-26 — Repository interfaces live in `domain/` and speak only domain models. Creating or renaming a collection returns `CollectionNameResult` (`Success(collectionId)` / `NameTaken`) instead of throwing: a taken name is an expected user outcome, and a sealed result makes callers handle it. The data layer catches Room's constraint exception and maps it, so no storage types reach `domain`. Name trimming/blank validation is left to the UI layer.
- 2026-10-26 — Repositories add no explicit `Dispatchers.IO` around Room calls — Room's `suspend`/`Flow` APIs already run queries on its own background executor, so `withContext` would only add a thread hop. Non-Room I/O (file copy, cover generation in M2) still uses `Dispatchers.IO` explicitly.
- 2026-10-26 — Mapper tests run on the JVM (`test/`); repository tests are instrumented against an in-memory DB, because the key behaviour (Room's constraint exception → `CollectionNameResult.NameTaken`) needs real SQLite and fake DAOs would mostly test the fakes. `CollectionRepositoryImpl` takes an injectable `clock` for `createdAt`.
- 2026-10-26 — Koin wiring is verified by an instrumented test that resolves each repository from the graph the app's `Application` starts (Koin `single`s are lazy, so a successful launch proves nothing) — no `koin-test` dependency needed. The database file is `comic_reader.db`.
- 2026-10-26 — `ComicArchiveReader.open(file)` returns a `Closeable` `ComicArchive` (page names + `openPage(index)`) so the reader can keep one `ZipFile` open while paging. An unreadable/corrupt file throws `InvalidArchiveException` (an I/O failure, unlike the expected `NameTaken` outcome); an archive with no images opens with an empty page list and the importer decides to reject it. Natural sort compares digit runs by value, letters case-insensitively, and treats `/` as lowest so paths sort folder by folder; entries with Windows `\` separators are understood. Test archives are generated inside tests — no binary fixtures in the repo.
- 2026-10-26 — `ComicFileStore` (`data/files/`) owns `comics/<uuid>.cbz` and `covers/<comicId>.webp` (later renamed to `covers/<uuid>.webp`, see below) under a root dir passed in (`filesDir` in the app, a temp dir in tests). A failed or cancelled copy deletes its partial file; a process death mid-copy can still leave an unreferenced file (harmless, startup cleanup is backlog). Tests get real `content://` Uris by inserting into MediaStore Downloads — a `FileProvider` declared in the test APK runs under the test package's UID and the app cannot open it.
- 2026-10-26 — Covers are pre-generated at import rather than decoded from the archive's first page each time the library shows them: full-size pages are often 2–10 MB, Coil only disk-caches network loads, so the grid would re-decode every cover after each restart. Generating at import also rejects files whose first page can't be decoded. Cost: one small WebP per comic.
- 2026-10-26 — Cover thumbnails fit inside 600×900 px (aspect kept, never upscaled), lossy WebP quality 80 (~50–80 KB). Sized for tablets and foldables, not just phone grid cells; the height cap stops tall strips and spreads from producing huge thumbnails. Decoded with `ImageDecoder` straight to the target size (no full-size bitmap) and written via a `.tmp` file + rename so a failure never leaves a partial cover.
- 2026-10-26 — Covers are named after the comic's file (`covers/<uuid>.webp`), superseding `covers/<comicId>.webp` — the id only exists after the insert, which would have forced insert → cover → update (plus a row rollback) and let the library briefly show a comic without its cover. Now every file is prepared first and the row is inserted last, complete.
- 2026-10-26 — Import: `ComicImporter` (domain interface, takes a document Uri as a `String` so `domain` stays Android-free) imports one file and returns `ImportResult.Imported` or `Failed(name, ImportFailure)` (`CANNOT_READ_FILE`, `NOT_A_CBZ`, `NO_PAGES`, `FIRST_PAGE_UNREADABLE` — an enum so the UI owns the wording). The ViewModel loops over picked files, which gives it per-file progress. `ComicImporterImpl` lives in `data/files/`; on failure it deletes the copied file and cover; the final insert runs under `NonCancellable` so cancellation can't strand a row whose files were cleaned up. Title = display name without extension.
- 2026-10-26 — Import picker (`OpenMultipleDocuments`) offers `application/x-cbz` (what Android's media store reports), `application/vnd.comicbook+zip`, `application/zip`, `application/x-zip-compressed` and `application/octet-stream` — providers label CBZ inconsistently, and the importer rejects non-CBZ content anyway. `ComicImporter` never throws except for cancellation; unexpected errors become `ImportFailure.UNEXPECTED`. The library reports failures once per batch in a snackbar (the reason for one file, a count for several), delivered through UI state and acknowledged by the screen so rotation can't drop them.
- 2026-10-26 — Library grid uses `GridCells.Adaptive(100.dp)`: 3 columns on a typical phone, more on tablets and foldables. Icons are local vector drawables (Material Symbols, Apache 2.0) instead of the `material-icons` dependency. `lifecycle-runtime-compose` is declared explicitly for `collectAsStateWithLifecycle`; `kotlinx-coroutines-test` is also a `testImplementation` for JVM ViewModel tests.
- 2026-10-26 — `ComicRepository.deleteComic` owns the whole deletion: row first (memberships cascade), then the stored file and cover via `ComicFileStore`, so no caller can forget the files and a file-deletion failure only leaves unreferenced files rather than an entry without a file. The user's original document is never touched (the app only holds read access to it). Library delete is a long-press (with an accessibility label) → confirmation dialog whose state lives in the ViewModel, so it survives rotation; no undo, since files are really deleted.
- 2026-10-26 — Navigation uses **type-safe routes** (`@Serializable` objects/classes) — arguments such as `collectionId` are checked at compile time, and it is the approach Navigation now recommends. Adds the Kotlin serialization plugin (same version as Kotlin) and `kotlinx-serialization-core` 1.12.0 (declared explicitly; Navigation already pulled in 1.7.3 transitively).
- 2026-10-26 — App structure: a Material 3 bottom `NavigationBar` with two top-level tabs, Library and Collections. Each tab is a nested graph and switching tabs saves/restores its back stack, so Collection detail stays open inside the Collections tab. The bottom bar shows only for destinations inside a tab graph, leaving room for a full-screen reader (M4). Screens keep their own `Scaffold`/top bar; the outer `Scaffold`'s insets are consumed so they aren't applied twice.
- 2026-10-26 — Collections list rows show the name, comic count and the cover of the collection's first comic by position (one `observeSummaries()` query with correlated subqueries, exposed as `CollectionSummary`). Row actions (Rename, Delete) live in an overflow menu — more discoverable than long-press. Names are trimmed; blank names and `NameTaken` show an inline error and keep the dialog open. All dialog state lives in the ViewModel so it survives rotation.
- 2026-10-26 — Screens that need a route argument receive it as a Koin parameter (`koinViewModel { parametersOf(id) }`, read from the type-safe route in the nav graph) rather than via `SavedStateHandle.toRoute()`, keeping ViewModels plain classes that are easy to unit-test on the JVM; Navigation still restores the argument after process death. Collection detail observes `observeCollection(id)` and closes itself when the collection disappears. The cover grid is shared from `ui/components/ComicGrid`, with a per-screen long-press action (Library: delete; Collection detail: remove from collection, with confirmation).
- 2026-10-26 — Library long-press opens a bottom action sheet (Add to collections, Delete) — larger targets than a per-card menu and it names the comic; it is driven by ViewModel state so the shared grid stays unchanged. "Add to collections" is a checklist with current memberships pre-checked; Save applies only the difference (add newly checked at the end, remove unchecked). No inline collection creation: with no collections the dialog explains where to create one and offers "Go to Collections", which switches tabs.
- 2026-10-26 — Reader pages load through Coil from an already-open archive: a `ComicPage(archive, fileName, index)` model with a custom `Fetcher` (streams the zip entry) and `Keyer` (`comic-page:<fileName>:<index>`), registered on the app's singleton `ImageLoader` (`ComicReaderApplication` implements `SingletonImageLoader.Factory`). One `ZipFile` stays open per reading session; Coil decodes each page on demand at the requested size and keeps recent pages in its memory cache. Pages aren't disk-cached by Coil — they already live in app storage.
