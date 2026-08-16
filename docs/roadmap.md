# Roadmap

Status as of 16 Aug 2026. See [`docs/foundation.md`](foundation.md) for the reasoning behind each decision referenced here.

- [ ] **Phase 1 — UI/UX Wireframing & Prototyping**
  Low-fi flows for onboarding, home/plan overview, active-run (both tracks), post-run summary, shoe log, Live Track share. Hi-fi mockup of the active-run screen specifically. Clickable prototype for onboarding → first run. Accessibility pass on mockups.

- [x] **Phase 2 — Project Setup**
  GitHub repo, multi-module Gradle structure (`:app`, `:core:*`, `:feature:*`), version catalog, CI (lint/test/assemble on every PR), issue/PR templates.

- [x] **Phase 3 — Core Architecture**
  Navigation graph (type-safe Navigation Compose, animated transitions honoring reduced-motion), Room schema (runs, plans, plan sessions, session pacing steps, shoes — `session_steps` added for the walk/run pacing spec), repository layer over Room with Hilt DI end-to-end (`HomeScreen` reads live data through the whole chain), M3 theme with the effort/recovery extended-color system and `animateColorAsState` in real use.

- [x] **Pulled forward — real vertical slice (ahead of Phase 4/5/6)**
  Home's empty state had no path to anything else, so a real slice landed early rather than leaving it a dead end — and grew further once GPS/weather/audio questions came up before their phases were scheduled:
  - Onboarding creates a real plan + session (week-1 `WalkRunPreset`); Run Summary has working RPE capture.
  - Active Run: real countdown timer off the session's steps, voice cues on every walk/run transition (interim `TextToSpeech` engine — see below), in-run tabs for Run / Track / Weather.
  - **Real GPS tracking** (outdoor sessions): `FusedLocationProviderClient` updates accumulate a live distance (haversine between fixes, used as the recorded run's real distance once enough fixes exist) and elevation gain from GPS altitude (±10–20m accuracy — a real caveat, not a precise source). The Track tab draws the raw path on a `Canvas` polyline; there's no basemap under it yet, that's still the real MapLibre + Protomaps PMTiles work from Phase 5.
  - **Real weather** (`:core:weather`, Open-Meteo, no API key): fetched from the runner's actual location on the pre-run environment screen and again in-run, with a live `WeatherAnimation` (Canvas-drawn rain/snow/sun/fog/clouds, condition-driven, reduced-motion aware) rather than a static icon.
  - **Real music control**: play/pause/skip dispatched as standard media-button key events (`MusicController`) — works with whatever's already playing (Spotify, YouTube Music, a local player). This is control, not browsing/picking a song — see docs/foundation.md's three-tier music plan for that distinction.
  - History and Insights are real too now: a shoe log (add/retire, mileage bar) and run list, and computed stats (weekly volume, streak, best pace, longest run) from actual run rows — no chart yet, that needs more logged runs to be meaningful.
  - The pre-run environment screen now offers a shoe picker too — a completed run accumulates its distance onto the chosen shoe automatically, connecting two previously-separate pieces.
  - **Live Track**: real one-tap "share my current GPS location" via the system share sheet — genuinely working, but honestly scoped down from the wireframe's persistent auto-updating link. That needs a backend to host and push updates to a watcher; a client-only app can't do that alone. Labeled as a snapshot share, not the full feature, right on the screen.
  - Honest gaps this slice does NOT close: voice cues and location tracking both stop if the app is backgrounded (no foreground service yet — that's the real Phase 4 `MediaSessionService` architecture); no basemap tiles; no persistent Live Track link. Build, lint, and unit tests all verified clean on real Gradle/AGP/SDK, and build-verified on device (Galaxy S23, Android 15) — not just compiled.

- [~] **Phase 4 — Media3 Audio Engine** (real MediaSessionService landed 16 Aug 2026; several sub-items still open)
  - [x] **`MediaSessionService` + `ExoPlayer` wiring**: `RunAudioService` (a real `MediaSessionService`) + a shared `ExoPlayer` (`AudioModule`) replace the interim `AndroidVoiceCueSpeaker`. `Media3VoiceCueSpeaker` synthesizes each cue via TTS-to-file (`CueSynthesizer`) and plays it through the player. GPS tracking's backgrounding survival was already real (`RunSessionService`, prior work) — this closes the matching gap for voice cues.
  - [x] **Real audio focus/ducking cycle** — an explicit `AudioFocusRequest(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)` request/release around each cue (`Media3VoiceCueSpeaker`), not Media3's automatic focus handling — that was tried first and crashes on-device for any usage other than `USAGE_MEDIA`/`USAGE_GAME`. Verified for real on an emulator: system `MediaFocusControl` logs show `requestAudioFocus() ... req=3` (transient-duck) through `PLAYING` to `abandonAudioFocus()`, matching the spec's "request focus, play a clip, release focus" exactly. Not yet tested against real third-party apps (Spotify/YouTube Music/a podcast app) — only verified that focus request/release/ducking-type are correct, not the cross-app listening experience.
  - [ ] Cue-script trigger engine (distance/time/HR-zone triggers) — cues still fire off the same walk/run step transitions as before, not a general trigger system yet.
  - [ ] Offline voice pack — cues are still synthesized via on-device TTS every time, not bundled pre-recorded clips with a TTS fallback.
  - [ ] Doze/background-execution testing — not yet tested with the device actually in Doze, only foregrounded/backgrounded during an active screen session.
  - [ ] Spotify App Remote SDK integration (tier 2) and in-app local-file player (tier 1) — tier 3 (`MusicController`, media-button dispatch) is the only tier implemented.
  - Two real bugs found and fixed while getting this far, both only reproducible on an actual device, not by compiling or unit-testing: (1) starting an outdoor run's foreground service without location permission actually granted crashed with a `SecurityException` — `RunSessionService` now falls back to a `dataSync`-typed foreground service when permission isn't there instead of crashing; (2) `RunAudioService` needs to call `startForeground()` explicitly in `onCreate()` — relying on `MediaSessionService`'s own automatic promotion wasn't fast enough and the OS killed the process with `ForegroundServiceDidNotStartInTimeException`.

- [ ] **Phase 5 — GPS & Fitness Tracking**
  GPS smoothing (raw fixes are noisier than the haversine-between-fixes approach above), Health Connect read/write, scoped permission flows, a real persistent/auto-updating Live Track link (needs backend infra — the client-only snapshot-share version already works), condition-aware coaching adjustments beyond the pace-target text already shown (hydration cues, cold warm-up extension), MapLibre + Protomaps PMTiles route mapping with on-demand regional download/caching to replace the raw Canvas polyline.

- [ ] **Phase 6 — Custom Workouts, Adaptive Scheduling & Performance Insights**
  Interval/tempo builder, negative-split pacing calculator, adaptive scheduler (compress/shift/regress), shoe mileage tracking tied to run completion, `:feature:insights` dashboard (weather-normalized pace trend, training load/consistency, HR zone distribution, VO2 max trend, personal bests).

- [ ] **Phase 7 — Polish & Animations**
  M3 Expressive motion pass tuned against real run data, deliberate micro-interactions on in-run controls, Baseline Profiles + macrobenchmarks, full accessibility/reduced-motion audit. Optional enhancement: ML Kit Pose Detection-assisted stretch form feedback (front camera, opt-in, pre/post-run only — see docs/foundation.md for what this can and can't actually verify).
