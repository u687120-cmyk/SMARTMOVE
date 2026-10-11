# SMARTMOVE AI architecture

## Purpose

SMARTMOVE AI is a road-risk intelligence layer. It answers **what is happening on the road now** and recommends a safer route; it is not intended to recreate a general-purpose map application.

## Core principle

Risk is time- and condition-dependent. A road segment's current risk must be recalculated from fresh observations rather than saved as a permanent “safe” or “dangerous” label.

```text
GPS / vehicle telemetry ─┐
Traffic observations ────┤
Weather observations ────┤
Hazards and crowd reports┤
Road characteristics ────┤──> Risk engine ──> Current risk + recommendation
Historical accidents ────┘
```

The future risk engine will use configurable prototype weights:

| Input | Initial weight |
| --- | ---: |
| Traffic | 20% |
| Hard braking | 20% |
| Weather | 15% |
| Road hazards | 20% |
| Accident history | 15% |
| Speed pattern | 10% |

Scores from 0–30 are low risk, 31–60 medium risk, and 61–100 high risk. The engine will return a score, level, contributing factors, recommendation, and timestamp.

## Future application flow

```text
Android app (Kotlin / Jetpack Compose)
                │ REST / JSON
                ▼
Node.js + Express API
                │
                ▼
Services: users, trips, risk, hazards, traffic, weather, analytics
                │
                ▼
MySQL
```

## Data credibility rules

The MVP must identify what it demonstrates:

| Category | Examples |
| --- | --- |
| Live data | GPS position; real provider data when actually connected |
| Mock data | Demo traffic, weather, braking observations, hazards |
| Future integration | Camera/radar vehicle distance, live traffic-light state, vehicle telemetry |

Phone GPS alone cannot reliably detect vehicle-ahead distance, traffic-light state, potholes, or every obstruction. These need appropriate sensors, data providers, infrastructure data, computer vision, or crowd reports.

## Phase 1 boundary

Only the repository and Android build foundations exist now. No API, MySQL schema, risk engine, map, authentication, or production data integration is implemented in this phase.
