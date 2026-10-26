# CLAUDE.md

Android comic reader app (Kotlin, Jetpack Compose). The full product and technical spec is in `docs/SPEC.md`. Read it before starting any task.

## How we work

- Work on **one milestone or one small task at a time**, as defined in `docs/SPEC.md`. Do not start the next milestone unless asked.
- **Propose a short plan before writing code** for any change touching more than a couple of files, and wait for approval.
- Keep changes small and focused. No unrelated refactors or "while I'm here" changes.
- **Do not add new dependencies** without asking first. Use the version catalog (`gradle/libs.versions.toml`) for any approved dependency.
- Follow the package structure and data model in the spec. If something in the spec seems wrong or blocks the task, say so instead of silently deviating.
- When a decision changes, propose an update to `docs/SPEC.md` (including the decision log) as part of the change.
- Never commit or modify signing keys, keystores, or `local.properties`.
- No network access in the app. No analytics.
- Pull request descriptions must not include Claude session links (`https://claude.ai/code/session_...`).

## Code conventions

- Kotlin, Compose, Material 3. MVVM: one `ViewModel` per screen exposing `StateFlow<UiState>`.
- `domain/` must not import Android framework classes.
- Use coroutines and Flow; do I/O on `Dispatchers.IO` (Room's `suspend`/`Flow` calls already run off the main thread and need no extra dispatcher).
- Write tests for non-UI logic: JVM unit tests (`test/`) for pure logic such as archive reading and mappers; instrumented tests (`androidTest/`, in-memory Room) for DAOs and repositories.

## Commands

- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew test`
- Instrumented tests (device/emulator needed): `./gradlew connectedAndroidTest` (prints nothing per test on success; for a summary run `./gradlew installDebug installDebugAndroidTest` then `adb shell am instrument -w com.androsgreen.comicreader.test/androidx.test.runner.AndroidJUnitRunner`)
- Lint: `./gradlew lint`

After making changes, run the build and unit tests and report the results. When `data/` or `di/` code changes, also run the instrumented tests if an emulator is available, and say so if it isn't.
