# Roadmap

Status as of 16 Aug 2026. See [`docs/foundation.md`](foundation.md) for the reasoning behind each decision referenced here.

- [ ] **Phase 1 — UI/UX Wireframing & Prototyping**
  Low-fi flows for onboarding, home/plan overview, active-run (both tracks), post-run summary, shoe log, Live Track share. Hi-fi mockup of the active-run screen specifically. Clickable prototype for onboarding → first run. Accessibility pass on mockups.

- [x] **Phase 2 — Project Setup**
  GitHub repo, multi-module Gradle structure (`:app`, `:core:*`, `:feature:*`), version catalog, CI (lint/test/assemble on every PR), issue/PR templates.

- [ ] **Phase 3 — Core Architecture**
  Navigation graph, Room schema (runs, plans, sessions, shoe profiles, cue scripts), ViewModel/repository conventions, Hilt DI across module boundaries.

- [ ] **Phase 4 — Media3 Audio Engine**
  `MediaSessionService` + `ExoPlayer` wiring, audio focus/ducking cycle (tested against Spotify/YouTube Music/a podcast app), cue-script trigger engine, offline voice pack + TTS fallback, Doze/background-execution testing.

- [ ] **Phase 5 — GPS & Fitness Tracking**
  Fused location + GPS smoothing, Health Connect read/write, scoped permission flows, Live Track implementation, outdoor/treadmill session toggle, Open-Meteo weather fetch + condition-aware coaching adjustments (heat/humidity pace guidance, hydration cues, cold warm-up extension), MapLibre + Protomaps PMTiles route mapping with on-demand regional download/caching.

- [ ] **Phase 6 — Custom Workouts, Adaptive Scheduling & Performance Insights**
  Interval/tempo builder, negative-split pacing calculator, adaptive scheduler (compress/shift/regress), shoe mileage tracking tied to run completion, `:feature:insights` dashboard (weather-normalized pace trend, training load/consistency, HR zone distribution, VO2 max trend, personal bests).

- [ ] **Phase 7 — Polish & Animations**
  M3 Expressive motion pass tuned against real run data, deliberate micro-interactions on in-run controls, Baseline Profiles + macrobenchmarks, full accessibility/reduced-motion audit.
