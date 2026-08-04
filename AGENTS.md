# AGENTS.md

## Cursor Cloud specific instructions

This repo is a **single-module native Android app** (Kotlin + Jetpack Compose + Room),
package `com.financetracker.evolva`, Gradle module `:app`. There is no web/backend
service and no `package.json`. All data is local (Room + DataStore).

### Toolchain (already provisioned in the VM snapshot)

- JDK 21 is on `PATH`; Gradle wrapper is `./gradlew` (Gradle 9.6.1).
- Android SDK lives at `~/android-sdk` (`cmdline-tools`, `platform-tools`,
  `platforms;android-37.0`, `build-tools;37.0.0`, plus `emulator` +
  `system-images;android-35;google_apis;x86_64`). It is baked into the snapshot,
  so the startup/update script does **not** reinstall it.
- `local.properties` (gitignored) points Gradle at the SDK via `sdk.dir`. The
  update script recreates it on startup; if Gradle ever reports "SDK location not
  found", run `printf 'sdk.dir=%s\n' "$HOME/android-sdk" > local.properties`.
- Non-interactive shells do **not** source `~/.bashrc`, so `ANDROID_HOME` may be
  unset in a fresh command. Gradle still works (it reads `local.properties`), but
  for `adb`/`emulator`/`sdkmanager` commands, first run:
  `export ANDROID_HOME=$HOME/android-sdk && export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH`

### Build / lint / test / run

- Build (dev): `./gradlew :app:assembleDebug` → APK at
  `app/build/outputs/apk/debug/app-debug.apk`. First build auto-installs
  `build-tools;36.0.0` (AGP requirement) — expected, not an error.
- Lint: `./gradlew :app:lintDebug`. The task runs and produces a full report, but
  currently **exits non-zero** because the app code has pre-existing lint errors
  (e.g. `LocalContextGetResourceValueCall`). That is a codebase issue, not an
  environment problem — do not treat the non-zero exit as broken setup. Report:
  `app/build/intermediates/.../lint-results-debug.txt`.
- Unit tests: `./gradlew :app:testDebugUnitTest`. Runs clean but reports
  `NO-SOURCE` — there are currently no `src/test` / `src/androidTest` sources.
- Install to a running device/emulator: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Emulator caveat (important)

The Cloud VM has **no `/dev/kvm`** (no nested virtualization), so the Android
emulator can only run with software CPU emulation (`-accel off`, TCG). It does
boot and the app builds/installs/launches and renders, but:

- Boot takes several minutes and the whole system is very slow.
- `systemui` / `system_server` frequently show "isn't responding" ANR dialogs,
  and swiftshader can log `Failed to find ColorBuffer`, after which `adb exec-out
  screencap` returns an all-black frame until things settle.
- Reliable multi-step UI automation is impractical. Prefer
  build + lint + unit-test tasks for verification; use the emulator only for
  quick smoke checks and be patient (idle, then a single action + screenshot).

Start a headless emulator (AVD `test_avd` already exists):

```
emulator -avd test_avd -no-window -no-audio -no-snapshot -no-boot-anim \
  -gpu swiftshader_indirect -accel off -memory 3072 -cores 4 &
adb wait-for-device
# poll: adb shell getprop sys.boot_completed   # "1" == booted
```

Disabling animations reduces load a bit:
`adb shell settings put global window_animation_scale 0` (and
`transition_animation_scale`, `animator_duration_scale`).
