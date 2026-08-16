# Roadmap

Status as of 16 Aug 2026. See [`docs/foundation.md`](foundation.md) for the reasoning behind each decision referenced here.

- [ ] **Phase 1 — UI/UX Wireframing & Prototyping**
  Low-fi flows for onboarding, home/plan overview, active-run (both tracks), post-run summary, shoe log, Live Track share. Hi-fi mockup of the active-run screen specifically. Clickable prototype for onboarding → first run. Accessibility pass on mockups.

- [x] **Phase 2 — Project Setup**
  GitHub repo, multi-module Gradle structure (`:app`, `:core:*`, `:feature:*`), version catalog, CI (lint/test/assemble on every PR), issue/PR templates.

- [x] **Phase 3 — Core Architecture**
  Navigation graph (type-safe Navigation Compose, animated transitions honoring reduced-motion), Room schema (runs, plans, plan sessions, session pacing steps, shoes — `session_steps` added for the walk/run pacing spec), repository layer over Room with Hilt DI end-to-end (`HomeScreen` reads live data through the whole chain), M3 theme with the effort/recovery extended-color system and `animateColorAsState` in real use.

- [x] **Pulled forward — minimal real vertical slice (ahead of Phase 4/5/6)**
  Home's empty state had no path to anything else, so a thin real slice landed early rather than leaving it a dead end: Onboarding creates a real plan + session (from the week-1 `WalkRunPreset`), a functional Outdoor/Treadmill toggle, an Active Run screen with a real countdown timer driven by the session's steps (time-based only — no GPS or Media3 cues yet, those are still genuinely Phase 4/5), and a Run Summary screen with working RPE capture. History, Insights, and Live Track remain Phase 5/6 placeholders. Build-verified on device (Galaxy S23, Android 15), not just compiled.

- [ ] **Phase 4 — Media3 Audio Engine**
  `MediaSessionService` + `ExoPlayer` wiring, audio focus/ducking cycle (tested against Spotify/YouTube Music/a podcast app), cue-script trigger engine, offline voice pack + TTS fallback, Doze/background-execution testing. Three music-source tiers: in-app local-file player, real Spotify App Remote SDK integration, and duck-only coexistence for everything else (Amazon Music included — no public control SDK exists for it). Voice-led warm-up/cool-down stretch routine (hold-timer + named stretches) rides the same cue engine.

- [ ] **Phase 5 — GPS & Fitness Tracking**
  Fused location + GPS smoothing, Health Connect read/write, scoped permission flows, Live Track implementation, outdoor/treadmill session toggle, Open-Meteo weather fetch + condition-aware coaching adjustments (heat/humidity pace guidance, hydration cues, cold warm-up extension), MapLibre + Protomaps PMTiles route mapping with on-demand regional download/caching.

- [ ] **Phase 6 — Custom Workouts, Adaptive Scheduling & Performance Insights**
  Interval/tempo builder, negative-split pacing calculator, adaptive scheduler (compress/shift/regress), shoe mileage tracking tied to run completion, `:feature:insights` dashboard (weather-normalized pace trend, training load/consistency, HR zone distribution, VO2 max trend, personal bests).

- [ ] **Phase 7 — Polish & Animations**
  M3 Expressive motion pass tuned against real run data, deliberate micro-interactions on in-run controls, Baseline Profiles + macrobenchmarks, full accessibility/reduced-motion audit. Optional enhancement: ML Kit Pose Detection-assisted stretch form feedback (front camera, opt-in, pre/post-run only — see docs/foundation.md for what this can and can't actually verify).
