# ParkFlow — Community-Driven Smart Parking Assistant

> **Project status:** Planning / pre-development

## Repository structure

```text
Spott/
├── android/                  # Android application workspace
├── backend/                  # Firebase services and configuration
├── docs/
│   └── uml/
│       └── use-cases.puml     # Spott use-case diagram (PlantUML)
├── scripts/                  # Data import and development utilities
└── README.md                 # Project overview
```

## Project overview

ParkFlow is a proposed Android application that helps drivers find street parking by combining community reports, parking map data, and phone sensor signals. Think of it as a **Waze-inspired experience for parking**: when one driver is about to leave a parking spot, another driver looking for parking nearby can be notified and guided toward it.

> **Important:** A reported space is **not a reservation or a guarantee of availability**. Parking legality, access rules, and restrictions must be checked independently.

## The problem

Finding street parking in busy areas can take time, increase traffic, and frustrate drivers. Traditional parking maps may show where parking is allowed or where parking facilities exist, but usually do not indicate whether an individual street-side space has just become available.

ParkFlow aims to bridge that gap using timely community contributions.

## How it should work

1. **Detect parking:** The phone identifies a likely transition from traveling in a vehicle to walking. The app saves an approximate parking location, with user confirmation when needed.
2. **Recognize a return:** When the driver comes back near their saved parking position, the app may ask: **“Hey! Are you leaving your parking spot?”** Users can also report departure manually.
3. **Report an upcoming departure:** If the driver answers **Yes**, nearby users actively searching for parking can see that a spot *may soon become available*.
4. **Confirm departure:** After sufficient evidence or explicit confirmation that the driver has left, the report becomes **Recently vacated** and receives a timestamp.
5. **Find a spot:** Other users can view recent reports on a map, filter relevant results, and launch turn-by-turn navigation to the approximate location.
6. **Close the feedback loop:** A driver who arrives can report **Available / Occupied**, helping remove stale reports and improve future suggestions.

### Parking report lifecycle

`POTENTIALLY_LEAVING → RECENTLY_VACATED → OCCUPIED / EXPIRED`

The app should never present an unverified report as a guaranteed free parking space.

## Planned features

### Core MVP

- Interactive map with the user's location and nearby parking reports.
- Save and view the last parked-car location; manual adjustment for inaccurate GPS positions.
- Manual **I'm leaving** action and separate **upcoming** / **vacated** states.
- Real-time synchronization of reports between Android devices.
- Timestamps and automatic expiration of outdated reports.
- Mark a suggested parking spot as occupied or still available.
- Open external navigation (e.g., Google Maps) to a reported location.
- Basic filters by distance and report freshness.

### Next iteration

- Assisted parking detection using Activity Recognition and location signals.
- Geofencing near the parked car to prompt the driver about departure.
- Targeted push notifications for users actively searching nearby.
- Confidence indicator based on report age, estimated arrival time, local demand, and confirmations (initially heuristic, not a validated probability).
- Photo uploads for parking reports, with timestamps and moderation/reporting.
- Parking-area heatmap derived from sufficient observed community data.
- Parking categories, where verified: free, paid, public, restricted, etc.


## Android sensors and device signals

| Technology / signal | Intended use | Notes |
| --- | --- | --- |
| Fused Location Provider (GPS + other location sources) | Save the parked-car position; find nearby reports | Position may be inaccurate, especially among buildings or indoors. |
| Activity Recognition API | Infer transitions such as `IN_VEHICLE` to `WALKING` | It detects the phone's activity, not ownership of a car; passengers and public transit may cause false positives. |
| Accelerometer | Motion data / optional sensor demonstration | The API may fuse multiple device signals; accelerometer readings alone cannot reliably prove parking. |
| Geofencing API | Detect a possible return near the parked-car location | Background events can be delayed and are not suitable for second-level guarantees. |
| Bluetooth connection (optional) | Increase confidence when disconnecting from a known car | Only applicable to users who pair a compatible vehicle/device. |

Background location and activity permissions will be requested only when justified, with clear user controls and an always-available manual flow. The architecture will avoid continuous high-accuracy GPS tracking.

## Parking data sources

ParkFlow will combine **relatively static parking information** with **dynamic user reports**.

| Source | Potential contribution | Caveat |
| --- | --- | --- |
| [OpenStreetMap](https://www.openstreetmap.org/) / [Overpass Turbo](https://overpass-turbo.eu/) | Mapped parking facilities, parking-related street tags, roads and geometry | Street-side parking coverage varies; absence of a tag does not mean parking is forbidden. |
| [Timpark](https://www.timpark.ro/) | Official local parking zones, regulations, tariffs and street coverage | Data format, permissions, and programmatic availability must be verified before import. |
| Community reports | Parking departures, photos, occupancy confirmations and missing spots | User reports need validation, deduplication and expiration. |

**Data integration plan:** Collect or import each source separately, normalize the records into a shared internal data model, reconcile overlaps, preserve source attribution and update timestamps, and publish approved data to the backend. An initial Python import script may be used for one-off or periodic ingestion.

### Proposed data model

- **ParkingZone:** mapped street segment or parking area, geometry, access/fee rules, source, verification status.
- **ParkingPosition:** approximate reported parking point, location accuracy, related zone (if known).
- **ParkingEvent:** upcoming departure, confirmed departure, occupancy report, timestamp, expiration, reporter and verification metadata.
- **User:** account details, opt-in notification preferences and contribution history.

Parking areas and individual reports should remain separate: a street known to allow parking is **not** equivalent to a currently free space.

## Proposed technology stack

| Layer | Technology |
| --- | --- |
| Android app | Kotlin, Android Studio, Jetpack Compose |
| Map | Google Maps SDK for Android |
| Location and activity | Fused Location Provider, Activity Recognition API, Geofencing API |
| Authentication | Firebase Authentication |
| Database / real-time updates | Cloud Firestore |
| Image storage | Cloud Storage for Firebase |
| Notifications | Firebase Cloud Messaging |
| Backend validation and event processing | Cloud Functions for Firebase |
| Local preferences / optional offline cache | DataStore / Room |
| Data ingestion | Python, Overpass API, GeoJSON/JSON processing |
| Navigation in MVP | Launch Google Maps via intent; in-app navigation is out of scope initially |


