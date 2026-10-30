# Direct-APK Updates via GitHub Releases - Implementation Plan

Repo: `https://github.com/Mhdibrahim01/Qaf` | `applicationId = com.aistudio.wordbooks.nxvkqp` | `versionCode = 7`, `versionName = "3.1.0"`

## How it works (end to end)
1. You push a git tag `v3.2.0` -> **GitHub Actions** builds a signed release APK and publishes a **GitHub Release** with `Qaf-3.2.0.apk` + `update.json`.
2. The app (on launch, max once / 12 h) fetches `update.json`, compares `versionCode`.
3. If newer: dialog "Update available" -> user taps **Update** -> app downloads the APK (progress) -> opens Android's installer -> user taps **Install** once. (Android requires this tap; it cannot be silent.)
4. First time only: user must allow "Install unknown apps" for Qaf.
5. Users can also use a permanent link: `https://github.com/Mhdibrahim01/Qaf/releases/latest/download/Qaf.apk` (always newest) - share this once.

## Preconditions / decisions
| Item | Status / action |
|---|---|
| **Repo must be public** (or use a separate public repo only for releases) | Unknown. If the repo is private, the app can't download releases without a token and **you must never embed a token in the APK**. Recommended: create public repo `Qaf-releases` and have CI publish there. |
| **Same signing key forever** | Release uses `my-upload-key.jks` (alias `upload`). If key changes, updates fail with "package conflict" and users must uninstall (losing data). Back up the keystore safely. |
| Keystore in CI | `*.jks` is gitignored -> store as GitHub secrets (below). `build.gradle.kts` already reads `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` env vars - **do not modify the signingConfigs block** (AI Studio comment says it's required). |
| Security note | `build.gradle.kts` L33/L35 has a hardcoded fallback password (`wordbooks2026`) committed to git. Rotate the keystore password and rely on env vars if the repo is/will be public. |
| versionCode | Must increase every release (Android rejects equal/lower). CI derives it from the tag (below). |

## Step 1 - Version from tag (build.gradle.kts, minimal change)
Allow CI to override version without editing the file each time:
```kotlin
// in defaultConfig (keep existing values as fallback)
versionCode = (System.getenv("APP_VERSION_CODE") ?: "7").toInt()
versionName = System.getenv("APP_VERSION_NAME") ?: "3.1.0"
```
Convention: tag `v3.2.0` -> versionName `3.2.0`, versionCode = `GITHUB_RUN_NUMBER + 100` (monotonic, simple) **or** `major*10000 + minor*100 + patch`. Pick one and never go backwards (current 7, so start >= 8).

## Step 2 - GitHub secrets
`Settings -> Secrets and variables -> Actions`:
- `KEYSTORE_BASE64` = `[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks"))`
- `STORE_PASSWORD`, `KEY_PASSWORD`
(If publishing to a separate releases repo: `RELEASES_REPO_TOKEN` = fine-grained PAT with Contents: write on that repo only.)

## Step 3 - Workflow `.github/workflows/release.yml`
```yaml
name: Release APK
on:
  push:
    tags: ['v*']
permissions:
  contents: write
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - uses: gradle/actions/setup-gradle@v4
      - name: Decode keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > "$RUNNER_TEMP/release.jks"
      - name: Version from tag
        run: |
          TAG="${GITHUB_REF_NAME#v}"
          echo "APP_VERSION_NAME=$TAG" >> $GITHUB_ENV
          echo "APP_VERSION_CODE=$((GITHUB_RUN_NUMBER + 100))" >> $GITHUB_ENV
      - name: Build release APK
        env:
          KEYSTORE_PATH: ${{ runner.temp }}/release.jks
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew :app:assembleRelease --no-daemon
      - name: Prepare assets
        run: |
          APK=$(ls app/build/outputs/apk/release/*.apk | head -n1)
          cp "$APK" "Qaf-${APP_VERSION_NAME}.apk"
          cp "$APK" "Qaf.apk"
          SHA=$(sha256sum Qaf.apk | cut -d' ' -f1)
          cat > update.json <<EOF
          {"versionCode": $APP_VERSION_CODE, "versionName": "$APP_VERSION_NAME",
           "apkUrl": "https://github.com/Mhdibrahim01/Qaf/releases/download/v${APP_VERSION_NAME}/Qaf-${APP_VERSION_NAME}.apk",
           "sha256": "$SHA", "notes": "See release notes"}
          EOF
      - name: Publish release
        uses: softprops/action-gh-release@v2
        with:
          generate_release_notes: true
          files: |
            Qaf-*.apk
            Qaf.apk
            update.json
```
Notes: confirm JDK (check `gradle/gradle-daemon-jvm.properties`; Gradle here is 9.x so 17 or 21). Make sure the gradle wrapper is committed (it is). If the tag is on a separate releases repo, replace the last step with `repository: Mhdibrahim01/Qaf-releases` + `token`.

Manual release flow: `git tag v3.2.0 && git push origin v3.2.0`.

## Step 4 - Android manifest and FileProvider
```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
               android:resource="@xml/file_paths" />
</provider>
```
`res/xml/file_paths.xml`:
```xml
<paths><cache-path name="updates" path="updates/" /></paths>
```
(Internet permission already present. Check `data_extraction_rules.xml` doesn't back up the cache.)

## Step 5 - In-app updater (new package `com.example.data.update`)
Files:
- `UpdateInfo.kt` - `@Serializable data class UpdateInfo(val versionCode: Int, val versionName: String, val apkUrl: String, val sha256: String, val notes: String = "")`
- `UpdateRepository.kt`
```kotlin
class UpdateRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val manifestUrl =
        "https://github.com/Mhdibrahim01/Qaf/releases/latest/download/update.json"

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000; readTimeout = 10_000
            }
            conn.inputStream.bufferedReader().use { json.decodeFromString<UpdateInfo>(it.readText()) }
        }.getOrNull()
    }

    fun isNewer(info: UpdateInfo): Boolean = info.versionCode > BuildConfig.VERSION_CODE

    /** Downloads to cache/updates/, verifies SHA-256, returns file or null. */
    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
            val out = File(dir, "update.apk")
            val conn = URL(info.apkUrl).openConnection() as HttpURLConnection
            val total = conn.contentLengthLong.coerceAtLeast(1)
            val md = MessageDigest.getInstance("SHA-256")
            conn.inputStream.use { input -> out.outputStream().use { o ->
                val buf = ByteArray(64 * 1024); var done = 0L
                while (true) { val n = input.read(buf); if (n < 0) break
                    o.write(buf, 0, n); md.update(buf, 0, n); done += n
                    onProgress(done.toFloat() / total) } } }
            val hex = md.digest().joinToString("") { "%02x".format(it) }
            if (hex.equals(info.sha256, ignoreCase = true)) out else { out.delete(); null }
        }.getOrNull()
    }
}
```
- `ApkInstaller.kt`
```kotlin
fun installApk(context: Context, apk: File) {
    if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return  // user returns, taps Update again
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
```
- ViewModel: `updateState: StateFlow<UpdateUiState>` (Idle, Available(info), Downloading(progress), ReadyToInstall, Error). `checkForUpdate(force=false)` throttled by a `last_update_check` timestamp in SharedPreferences (12 h); skipped when offline.
- UI: `UpdateDialog` in `WordBooksAppShell.kt` (same place as other dialogs) + a "Check for updates" row and "Version 3.1.0" label in `SettingsScreen`. Offer "Later" (remember skipped `versionCode` for 3 days).
- Check on app start from `WordBooksAppShell` (`LaunchedEffect(Unit)`).

## Step 6 - Safety rules
- Only download from the exact GitHub host/URL pattern; reject other hosts; HTTPS only.
- **Verify SHA-256** (above). Android also verifies the signature matches the installed app, so a tampered APK from another key can't install.
- Never execute downloaded code outside the system installer; never embed tokens.
- Fail silently on network errors; never block app startup.

## Step 7 - Optional: content-only updates (no APK)
Assets JSON (grammar/reading, ~1 MB) can ship via the same release as `content-v12.json` + manifest entry `contentVersion`; app downloads to `filesDir/content/`, validates (schema + sha256), and falls back to bundled assets on failure. Do after the APK updater works; reuse the repo's existing import code (`DataImportRepository`, `GrammarRepository`, `ReadingRepository`).

## Step 8 - Test checklist
1. Build locally with `APP_VERSION_CODE=8` signed with the same key; install v7, host a fake `update.json` (local server / Gist), confirm prompt -> download -> install -> app restarts as v8 with data kept.
2. Wrong sha256 -> error shown, nothing installed.
3. No internet -> no dialog, no crash.
4. Same/lower versionCode -> no prompt.
5. Run on Android 8+ (unknown-sources prompt) and Android 7 (minSdk 24).
6. Tag a pre-release `v3.1.1-test` in CI and confirm release assets + `latest/download/update.json` resolve (note: `latest` ignores pre-releases).

## Rollout order
1. Decide public repo vs. releases repo, back up keystore, rotate password.
2. Steps 1-3 (CI) -> push tag, verify a downloadable signed APK.
3. Steps 4-6 (in-app updater) in a release that users install manually **once** (every older version has no updater).
4. Share the permanent link `.../releases/latest/download/Qaf.apk`.
5. Step 7 later.

## Limits (be upfront with users)
- One tap on "Install" is mandatory; Play Protect may show a warning for non-Play APKs.
- Users on versions before the updater must manually install once.
- GitHub download limits/availability apply; Google Play is more robust long-term.
