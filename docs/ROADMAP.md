# Phantom — roadmap & session log

Phantom is built in phases, each in its own session so it never strains usage
limits. The Python core is provider-local (Ollama) — zero API cost.

## Status

| Phase | Scope | State |
|---|---|---|
| 0 | Python core: phone LAM brain + PC gate, 12 tests | ✅ done |
| 1 | `Phantom-Gate.exe` packaging (PyInstaller) | ✅ done |
| 2 | One-line Termux installer for the brain | ✅ done |
| 3 | Native Android APK shell (Kotlin) driving the Termux brain | ✅ done |
| 4 | Embedded on-device LLM in the APK + built-in camera/vision | ⬜ next |
| 5 | Auto-pairing phone↔PC (QR / discovery) + polish | ⬜ |

## Phase 3 kickoff brief (start here next session)

Goal: a real Android app (icon, installable APK) that is a friendly front-end
to the existing brain — **not** a rewrite of the agent logic.

Recommended approach — thinnest viable native shell:
1. `android/` Gradle project, Kotlin, min SDK 26, single `MainActivity`.
2. UI: a goal text field, a "Run" button, a scrollable log, and a settings
   screen for `PHANTOM_PC_URL` / model (persisted in SharedPreferences).
3. The app does **not** re-implement the loop. It either:
   - **(A, simplest)** shells out to the Termux-installed `phantom` command via
     the Termux:Run / RUN_COMMAND intent, streaming output to the log; or
   - **(B)** ports the loop to Kotlin and calls the local Ollama HTTP API
     (`/api/generate`) and the PC gate directly. Reuses the same JSON protocol
     in `phantom/common/protocol.py` — mirror those models in Kotlin.
   Start with (A) to ship an APK fast, migrate to (B) in Phase 4.
4. Build with `./gradlew assembleDebug` → `app-debug.apk`. Document signing for
   a release APK.

Watch the Max limit: scaffold + UI + one happy-path run is plenty for one
session. Defer the embedded LLM (Phase 4) and pairing (Phase 5).

## Phase 4 kickoff brief (start here next session)
Goal: make the APK self-contained — port the agent loop into Kotlin so it no
longer needs the Termux `phantom` command.
1. Mirror `phantom/common/protocol.py` models in Kotlin data classes.
2. Port the loop (`phantom/phone/agent.py`): call local Ollama
   `http://127.0.0.1:11434/api/generate` (vision, base64 image) and the PC gate
   directly with OkHttp/Retrofit; reuse the same tool JSON + sliding window.
3. Add a screen-capture / camera path for on-device vision if desired.
Keep within the Max limit — porting the loop + one happy-path run is a session.

## Session log
- Session 1: phases 0–3. Rebranded amiranet → Phantom; Python core + EXE
  packaging + Termux installer + native APK shell. Branch
  `claude/code-subagents-project-id3k4o`.
