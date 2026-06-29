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

## Pairing (Phase 5)
The gate is token-secured: on start it prints a `PHANTOM:<code>` and a QR. In
the app, paste that code into **Pairing code** and tap **Pair** — it fills the
PC URL + token (and model) in one step. The token is then sent as
`Authorization: Bearer <token>` on every gate request. (Scanning the QR with any
QR app gives you the same code to paste; in-app camera scanning is future work.)

## Using it
1. Start the gate on your PC (`Phantom-Gate.exe`) and note the URL / pairing code.
2. Open Phantom on the phone, paste the pairing code and tap Pair (or enter the
   PC gate URL + model by hand), type a goal.
3. Choose how to run:
   - **Run via Termux** — drives the `phantom` command in Termux (Phase 3).
   - **Run on-device** — runs the LAM loop *inside the app* (Phase 4), talking
     straight to local Ollama (`http://127.0.0.1:11434`) and the gate. No
     Termux command needed; Ollama must still be reachable on the phone.
4. The brain sees the PC screen and acts; progress streams to the app's log.

## On-device loop (Phase 4)
`core/Agent.kt` is a Kotlin port of `phantom/phone/agent.py`: observe (gate
screenshot) → decide (local Ollama vision) → act (gate), with the same tool
JSON and sliding-window memory. `core/PcClient.kt`, `core/OllamaClient.kt`,
`core/Tools.kt`, and `core/Memory.kt` mirror their Python counterparts. Uses
OkHttp; cleartext HTTP is enabled for LAN/localhost.

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
