# Stride — Foundation Document

*Approved 16 Aug 2026. This is the reference every later architecture/design decision should trace back to.*

## Mission

Most running apps pick a side. Couch-to-5K apps are warm and encouraging but stop mattering once someone can actually run 5K. Serious training apps (Strava, Runna, TrainingPeaks) are powerful but assume a runner who already knows what a tempo interval is. **Stride** doesn't force that choice: the same app takes someone from their first 60-second jog/walk interval to negative-split 5K pacing, without the beginner feeling like a tourist or the advanced runner feeling like they've outgrown it.

## Target audience

**Beginner Track** — sedentary/lightly active adult starting from zero; intimidated by pace jargon and GPS watches; needs permission to walk and a plan that doesn't punish a missed Tuesday.

**Pro Track** — established runner chasing a specific 5K time; fluent in HR zones and tempo; wants precision, not encouragement; trains with music/podcasts and won't tolerate an app that interrupts either.

## Core value proposition

- One continuous journey, not two apps — same account/history carries a runner from week-1 intervals to sub-20 pacing.
- Coaching that lives in your ears, not your eyes — voice cues over Spotify/podcasts, not a screen you have to look at mid-stride.
- A plan that bends instead of breaking — the schedule adapts around missed days rather than declaring the runner "off track."
- Design built for motion — every screen assumes the reader is bouncing, sweating, and glancing for under a second.

## Tech stack decisions

| Area | Decision | Why |
|---|---|---|
| UI | Jetpack Compose, `derivedStateOf` around live metrics, `animateColorAsState` for effort/recovery shifts | Gate recomposition on a run screen that updates continuously for 20–60 min |
| Design system | Material 3 Expressive, custom branded `ColorScheme` (dynamic color opt-in) | Bolder motion/shape language fits an emotional, effort-driven context |
| Audio | Media3 `MediaSessionService` + `ExoPlayer`, `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` for cues | Cues survive backgrounding; ducking is *requested*, not force-set to an exact % — the other app decides its own attenuation |
| Health data | Health Connect (single read/write layer), Wear OS as a Phase 2+ companion | Runs show up correctly across the whole health-app ecosystem, not siloed |
| Color | Warm (orange/red) = action/effort, cool (teal) = recovery, cross-fades live during a run | Passive heads-up display via color, independent of reading text |
| Type | Condensed display face for live numerals, tabular figures mandatory | Digits must not jitter width while updating mid-run |
| Location | Android Fused Location Provider for GPS pace/distance | Standard, battery-aware, already required for Live Track and outdoor weather |
| Maps | MapLibre (open-source renderer) + Protomaps PMTiles (open OSM data as a static, downloadable file) | No API key, no tile-server bill, no rate limit — `tile.openstreetmap.org` explicitly forbids embedded-app use in its [tile usage policy](https://operations.osmfoundation.org/policies/tiles/), so it isn't a real option for a shipped app; PMTiles sidesteps the problem by not needing a live server at all |

## Feature specifications

### Dual tracks
- **Beginner:** C25K-style progressive overload (8–9 weeks), post-run RPE (1–10) logging, guided voice-led warm-up/cool-down, "First 5K" milestone framing.
- **Pro:** custom interval/tempo builder, VO2 max from wearable data via Health Connect, negative-split pacing with live ahead/behind cues, HR zones via Karvonen reserve method.

### Smart Audio Engine
Foreground `MediaSessionService` running for the run's duration. Cues fire off distance/time/HR-zone triggers, request transient duck focus, play a 2–4s clip, release focus. Offline-capable via bundled voice pack + TTS fallback.

**Music sources — three tiers, not one:**
- **Local files (in-app player):** Stride's own `ExoPlayer` instance plays MP3s (and other formats ExoPlayer supports) picked from the device via the system file/media picker — full control, since it's Stride's own player session ducking itself for cues, no external app involved.
- **Spotify (real integration):** the [Spotify App Remote SDK](https://developer.spotify.com/documentation/android/) lets Stride browse/control a signed-in user's Spotify playback directly, if they have the Spotify app installed. This is genuinely available and free, though production-scale API quota requires Spotify's app review. Ducking still applies the same way during cues.
- **Amazon Music, Hoopla, and everything else:** no public SDK exists for third-party apps to browse or control these — unlike Spotify, there's no "App Remote" equivalent to integrate against for either (Hoopla, a library-lending audiobook/media app, publishes no developer API at all). Stride can't offer a picker/browse surface for them. What it *can* do, and does today via `MusicController`, is generic play/pause/skip: standard Android media-button key events (`KEYCODE_MEDIA_PLAY_PAUSE`/`NEXT`/`PREVIOUS`), the same signal a Bluetooth headset button sends. Any app with a normal Android media session — which includes Amazon Music and Hoopla — responds to these, so basic transport control works without a dedicated integration. Ducking (the spec above) applies the same way regardless of which app is playing.

### Session pacing (walk/run breakdown and pace targets)
No session starts with a blank pace field. Before a session begins, the app states the plan for that specific session — not just "run for 28 minutes":

- **Beginner (walk/run intervals):** a named preset drives the breakdown — e.g. "4:00 walk at 3.0mph, then 1:00 run at 5.0mph, repeated" — shown plainly before the runner taps start, not discovered mid-run. The default preset is recommended from the runner's current plan week and recent RPE trend (a string of "felt hard" sessions holds at the current preset rather than advancing, same logic as the adaptive scheduler's regress-and-repeat), and is always editable before starting.
- **Pro (tempo/intervals):** the existing custom interval builder (rep count, work/rest, target pace or HR zone) *is* this same concept at higher precision — no separate system needed.
- **Session length:** asked explicitly up front — "how long do you want to train today?" — for freeform sessions, or shown/editable against the plan's target for a scheduled one. The chosen length and preset together determine how many walk/run reps actually fit, rather than the runner guessing.
- All speeds are stored as one canonical unit (m/s) and converted to mph/pace-per-km/pace-per-mile only at display time, so unit bugs can't creep in between GPS, storage, and the UI.

### Warm-up, cool-down, and stretching
Every session — Beginner or Pro — opens and closes with a guided stretch routine, voice-led through the same audio engine as interval cues (dynamic stretches pre-run: leg swings, walking lunges; static stretches post-run: calf, quad, hamstring), not just a generic "warm up first" reminder.

- **v1 (reliable):** a guided hold-timer per stretch, cued by voice ("hold for 20 seconds… and switch sides"), with a short library of named stretches the runner can swap between. This is honest compliance, not sensed compliance — it guides and times, it doesn't verify the runner's body position.
- **Later enhancement, not v1:** actual movement/form checking would use Google's ML Kit Pose Detection (on-device, free, no API key) via the front camera, propping the phone up before a stretch. Worth being precise about what this can and can't do: it can track rough body landmark positions and rep-like motion, it can't judge whether a stretch is being done *correctly* or is actually effective — so it's scoped as assistive posture feedback for runners who opt in and prop their phone, not as a "compliance monitor," and it only works for the pre/post-run session (never mid-run).

### Adaptive Scheduling
On a missed session, in order of preference: **compress** (shift remaining sessions later in the week) → **shift** (slide the whole plan forward) → **regress-and-repeat** (repeat the last comfortable week on a long gap or low RPE trend). The runner is never shown a failed plan, only a recalculated one.

### Hardware tracking
- **Shoe mileage:** multiple shoe profiles, auto-accumulated distance, configurable retirement threshold (default 500km/~300mi) with a proactive nudge.
- **Live Track:** one-tap, time-limited location-share link, auto-expiring, reduced-frequency polling independent of run-GPS, optional inactivity alert.

### Route mapping (offline-first, no API key)
Outdoor sessions trace the runner's GPS path over a real basemap without depending on a live tile server:

- **Renderer:** MapLibre Android SDK — an open-source, unauthenticated fork of the old Mapbox GL renderer. No key required to draw a map, only to choose what tiles you feed it.
- **Tile data:** Protomaps PMTiles — OpenStreetMap data pre-baked into a single static file, readable over plain HTTP range-requests. No tile server to run, no per-request auth or billing, and no exposure to `tile.openstreetmap.org`'s usage policy (which bars exactly this kind of embedded-app traffic).
- **"Download based on where you're running":** the first time GPS reports a location outside the currently cached region, the app fetches a PMTiles extract for that local area (city/county-sized, a few MB) and caches it on-device — prompted on Wi-Fi by default, with a manual "download this area" option for travel. Once cached, the basemap renders fully offline; only the GPS fix itself needs no network at all.
- **The runner's actual path** is drawn as a polyline from the app's own location samples, independent of the basemap — the map underneath can be missing or stale (e.g. mid-download) and the route trace still renders correctly.

### Environment: outdoor vs. treadmill, and weather adaptation
Every session starts with one question the app asks, not assumes: **"Running outside or on a treadmill today?"** — defaulted to the runner's last choice, one tap to change.

- **Outdoor:** GPS drives pace/distance as normal. On session start, the app pulls current conditions (temperature, humidity, wind, precipitation) for the runner's location from a weather API (Open-Meteo — no key/cost, good enough accuracy for training adaptation over a paid tier). Conditions feed two things:
  - **Live coaching adjustments:** heat/humidity above a threshold shortens the recommended pace target and adds hydration cues; cold extends the warm-up; wind/rain get an acknowledgment cue ("windy out there — pace will look slower, that's expected") rather than silence that lets the runner think they're losing fitness.
  - **Session metadata:** temperature/humidity/conditions are stored against the run record, not just used live and discarded — this is what makes the statistics feature below able to explain a slow run instead of just flagging it.
- **Treadmill:** GPS is switched off for pace/distance; the session instead reads belt speed/incline if the treadmill exposes Bluetooth FTMS data, or falls back to manual pace entry adjusted by accelerometer-based cadence. No weather is fetched or shown — the coaching engine's interval/tempo logic works identically either way, since it's already time- and effort-driven, not GPS-driven, per the audio engine spec above.
- Location permission for weather is requested only when "Outdoor" is chosen, following the same scoped-permission approach as Health Connect above.

### Performance Insights (statistics)
A dedicated insights view (`:feature:insights`, reading from `:core:data`) turns the run history the app is already collecting into trends the runner can act on, for both tracks:

- **Pace trend:** rolling-average pace per week/month, plotted against goal pace (Pro) or plan-expected pace (Beginner) — shown *weather-normalized* where outdoor sessions have conditions attached, so a hot-week slowdown reads as expected rather than as lost fitness.
- **Training load & consistency:** planned vs. completed sessions, current streak, and a rolling weekly-volume chart — this is where the adaptive scheduler's compress/shift/regress history becomes visible to the runner instead of invisible.
- **HR zone distribution** across recent sessions (Pro Track primarily, shown once HR data exists for Beginner).
- **VO2 max trend** over time where Health Connect provides it, rather than only a single current value.
- **Personal bests** — fastest 1K/5K, longest run, longest streak — surfaced as quiet milestones rather than competitive pressure, consistent with the peak-end framing used in the run-summary screen.

## Roadmap

See [`docs/roadmap.md`](roadmap.md) for the full seven-phase plan and current status.
