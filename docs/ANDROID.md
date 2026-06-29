# Phantom — Android app (Phase 3)

A native Android front-end (icon + installable APK) for the phone brain. It does
**not** re-implement the agent — it runs the `phantom` command already installed
in Termux (see `installer/termux_install.sh`) and shows its output. Porting the
loop into the APK itself is Phase 4.

## One-time Termux setup (so the app may drive it)
1. Install the Phantom brain in Termux (the one-line installer).
2. Allow external apps to run commands. In Termux:
   ```bash
   mkdir -p ~/.termux
   echo "allow-external-apps = true" >> ~/.termux/termux.properties
   termux-reload-settings
   ```
3. Install **Termux:API**-style permission: the first time the app runs a
   command, Android/Termux prompts to grant `RUN_COMMAND`. Approve it.

## Build the APK
The project lives in `android/`. Easiest path: open `android/` in **Android
Studio** (it provides the Gradle wrapper + SDK) and Run.

Command line (Android SDK + a generated Gradle wrapper required):
```bash
cd android
gradle wrapper          # once, if ./gradlew is missing
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```
Install the APK:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> A debug APK is unsigned-for-store but installable for personal use. For a
> shareable release APK, create a keystore and configure `signingConfigs`.

## Using it
1. Start the gate on your PC (`Phantom-Gate.exe`) and note the URL.
2. Open Phantom on the phone, enter the PC gate URL + model, type a goal, Run.
3. The brain (Ollama in Termux) sees the PC screen and acts; output streams to
   the app's log.

## Layout
```
android/
  settings.gradle.kts · build.gradle.kts · gradle.properties
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/java/tech/phantom/app/
      MainActivity.kt          UI: goal + settings + log
      TermuxRunner.kt          sends RUN_COMMAND to Termux
      PhantomResultReceiver.kt receives stdout/stderr back
    src/main/res/...           layout, strings, theme, launcher icon
```
