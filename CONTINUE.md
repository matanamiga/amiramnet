# PHANTOM — continuation guide (handoff)

A local DLAM: the **phone is the brain** (vision LLM + agent loop), the **PC is
the hands** (a thin gate that screenshots + executes). Nothing goes to the cloud.

## Status (all phases done, CI green)
| Phase | What | State |
|---|---|---|
| 0 | Python core: phone brain + PC gate | ✅ 17 tests |
| 1 | `Phantom-Gate.exe` packaging (PyInstaller) | ✅ |
| 2 | One-line Termux installer | ✅ |
| 3 | Native Android APK shell (Kotlin) | ✅ CI-compiled |
| 4 | LAM loop ported into the APK | ✅ CI-compiled |
| 5 | Token-secured gate + pairing (code/QR) | ✅ |
| 6 | Embedded on-device LLM (MediaPipe, no Ollama) | ✅ CI-compiled |
| + | mDNS discovery, foreground service, QR scan, signed release, CI | ✅ |

CI: <https://github.com/matanamiga/amiramnet/actions> — every push builds the
debug APK and uploads it as the `phantom-debug-apk` artifact.

## Get the APK
1. Open the latest green CI run → **Artifacts** → `phantom-debug-apk` → unzip →
   `app-debug.apk`.
2. On the phone: enable "install from unknown sources", install.

## Two ways to run (pick in the app)
**A — with Ollama (proven path):** install Termux + Ollama + a vision model via
`installer/termux_install.sh`; in the app leave "on-device" unchecked.

**B — embedded, no Ollama/Termux (new, Phase 6):**
1. Get a MediaPipe **vision** `.task` model (e.g. Gemma 3n) and host it at a
   direct download URL.
2. In the app: paste the URL → **Download model** (GBs, once) → tick **Use
   on-device model** → **Run on-device**.

## What is NOT yet verified — do this next
1. **Runtime on a real device.** The on-device path is *compile-verified only*.
   Install on a phone with ≥6–8 GB RAM and confirm `OnDeviceLlm` actually loads
   the `.task` model and `session.addImage(...)` accepts the screenshot. The
   MediaPipe vision API surface can differ between model builds.
2. **Pin `tasks-vision`.** It is currently `0.10.+` (dynamic) in
   `android/app/build.gradle.kts` to dodge a non-existent version. Once you
   confirm a working version at runtime, pin it for reproducible builds.
3. **Pick/host the model.** Decide on Gemma 3n vs another MediaPipe vision
   `.task`; confirm the license and a stable download URL.
4. **Tune.** `OnDeviceLlm` (maxTokens, temperature), screenshot scale
   (`PHANTOM_SCREENSHOT_SCALE`), and step cap — balance speed vs accuracy.
5. **Real-device E2E + screenshots**, then cut a **signed release APK**
   (`PHANTOM_KEYSTORE` env, see `docs/ANDROID.md`).

## Where things live
```
phantom/                  Python brain + gate
  pc/        worker.py (gate) · screen.py · actions.py · pairing.py · discovery.py
  phone/     agent.py (loop) · tools.py · memory.py · pc_client.py
  llm/       ollama_provider.py · mock_provider.py
android/app/src/main/java/tech/phantom/app/
  core/      Agent · VisionLlm · OllamaClient · OnDeviceLlm · ModelManager
             Tools · Memory · PcClient · Pairing · GateDiscovery
  MainActivity · PhantomService · ScanActivity · TermuxRunner
installer/   gate_entry.py · termux_install.sh
scripts/     build_gate.sh/.bat (EXE)
docs/        ARCHITECTURE · SETUP · ANDROID · BUILD · ROADMAP
```

## Dev loop
- Python tests: `python -m pytest -q`
- Android: pushed to CI builds the APK; locally `cd android && ./gradlew assembleDebug`
- Gate EXE: `scripts/build_gate.bat` (Windows) → `dist/Phantom-Gate.exe`

## Reminder on performance
On-device inference is **not faster** than Ollama — same model class, same
phone. Phase 6's win is packaging (one APK, no Termux), not speed.
