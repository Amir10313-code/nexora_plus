# نکسورا پلاس / Nexora Plus — v3.0

Android app (Kotlin, Jetpack Compose, Material 3, Room, DataStore, WorkManager). Offline-first.

## Build on GitHub
1. Push this folder to a repository.
2. Open **Actions → Android CI**. Every push runs unit tests and builds the debug APK (artifact `nexora-plus-debug-apk`).
3. Release build: run the workflow by hand (**Run workflow**). For a signed build add the secrets
   `NEXORA_KEYSTORE_BASE64`, `NEXORA_KEYSTORE_PASSWORD`, `NEXORA_KEY_ALIAS`, `NEXORA_KEY_PASSWORD`; without them the release APK is unsigned.

Local: `./gradlew testDebugUnitTest assembleDebug` (JDK 17, Android SDK 35).

## Occasion updates
Bundled data: `app/src/main/assets/occasions/<region>.json`. To enable online updates, host the same files
and set the address in *Settings → Occasion updates*, using `{region}` as a placeholder, e.g.
`https://raw.githubusercontent.com/<user>/<repo>/main/occasions/{region}.json`.
A file is only applied when it passes validation and has a higher `datasetVersion`.
Entries with `"verified": false` are shown with an “Unverified” label.
