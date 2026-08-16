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

## Feature specifications

### Dual tracks
- **Beginner:** C25K-style progressive overload (8–9 weeks), post-run RPE (1–10) logging, guided voice-led warm-up/cool-down, "First 5K" milestone framing.
- **Pro:** custom interval/tempo builder, VO2 max from wearable data via Health Connect, negative-split pacing with live ahead/behind cues, HR zones via Karvonen reserve method.

### Smart Audio Engine
Foreground `MediaSessionService` running for the run's duration. Cues fire off distance/time/HR-zone triggers, request transient duck focus, play a 2–4s clip, release focus. Offline-capable via bundled voice pack + TTS fallback.

### Adaptive Scheduling
On a missed session, in order of preference: **compress** (shift remaining sessions later in the week) → **shift** (slide the whole plan forward) → **regress-and-repeat** (repeat the last comfortable week on a long gap or low RPE trend). The runner is never shown a failed plan, only a recalculated one.

### Hardware tracking
- **Shoe mileage:** multiple shoe profiles, auto-accumulated distance, configurable retirement threshold (default 500km/~300mi) with a proactive nudge.
- **Live Track:** one-tap, time-limited location-share link, auto-expiring, reduced-frequency polling independent of run-GPS, optional inactivity alert.

## Roadmap

See [`docs/roadmap.md`](roadmap.md) for the full seven-phase plan and current status.
