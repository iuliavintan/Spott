# Spott — Community-Driven Smart Parking Assistant

Spott is a planned Android application that helps drivers find street parking through community reports, parking map data, and optional phone activity signals. Drivers can report an upcoming departure, confirm that they have left, and help nearby drivers discover recently vacated spots.

> **Project status:** Week 8 implements a local CRUD prototype for unpublished parking-report drafts using Room. The authenticated community workflow and backend services are planned.

Reports describe approximate community observations and do not guarantee availability. Drivers must check parking legality, access rules, and restrictions.

## Repository structure

```text
Spott/
├── android/                  # Kotlin / Jetpack Compose Android application
├── backend/                  # Empty placeholder for Firebase services
├── docs/
│   └── uml/
│       ├── README.md          # Diagrams, use cases, and functional rules
│       ├── *.puml             # Use-case, activity, sequence, and class diagrams
│       ├── data-flow.dot      # Data-flow diagram (Graphviz)
│       └── *.svg              # Rendered diagram previews
├── scripts/                  # Empty placeholder for data import utilities
└── README.md                 # Project overview
```

See the [functional documentation](docs/uml/README.md) for diagrams, actors, scenarios, report states, and detailed rules.

## How it should work

Parking operations require an authenticated account.

1. **Save the car location:** The driver selects and confirms a position. Optional device signals can suggest a position for review. Saving it keeps one private car location per account.
2. **Report an upcoming departure:** The driver explicitly publishes a report at the saved or a manually selected position. A return-to-car prompt can suggest this action. The report starts as `POTENTIALLY_LEAVING`.
3. **Confirm or cancel:** Before expiration, the author explicitly confirms departure to set `RECENTLY_VACATED`, or cancels the upcoming report to set `CANCELLED`.
4. **Find parking:** Drivers browse reports on a map, inspect their status and age, filter results, and open external navigation to a selected destination.
5. **Give feedback:** An authenticated driver can report a spot as occupied using recent, accurate device-location evidence near the spot. Available feedback applies to a valid recently vacated report and preserves its expiration time.
6. **Follow an area:** In the planned alerts feature, the driver selects a fixed search center and radius. Matching reports can trigger notifications while that search is active. Moving the phone does not move the search area.

Active reports expire using server time. Closed reports are excluded from live results and new alerts.

## Planned features

### Core MVP

- Account registration, login, and logout.
- Interactive map with nearby parking reports, using current location or a manually selected area.
- Save, view, and adjust the last parked-car location, with explicit confirmation when saving.
- Manual upcoming-departure reporting, explicit departure confirmation, and cancellation of an upcoming report.
- Real-time synchronization of reports between Android devices.
- Timestamps and automatic expiration of outdated reports.
- Report a spot as available or occupied, with proximity checks for occupied feedback.
- Open external navigation (e.g., Google Maps) to a reported location.
- Basic filters by distance and report freshness.

### Next iteration

- Assisted parking suggestions using Activity Recognition and location signals, always confirmed by the driver.
- Geofencing near the parked car to suggest a departure report.
- Active searches with a fixed, user-selected center and radius, and targeted push notifications.
- Confidence indicator based on report age, estimated arrival time, local demand, and confirmations (initially heuristic, not a validated probability).
- Parking-area heatmap derived from sufficient observed community data.
- Parking categories, where verified: free, paid, public, restricted, etc.


## Android sensors and device signals

| Technology / signal | Intended use | Notes |
| --- | --- | --- |
| Fused Location Provider (GPS + other location sources) | Suggest the parked-car position, find nearby reports, and provide location evidence for occupied feedback | Position accuracy and freshness are checked for occupied feedback. |
| Activity Recognition API | Suggest parking from transitions such as `IN_VEHICLE` to `WALKING` | The driver reviews and confirms the suggested parking position. |
| Accelerometer | Optional motion signal | Motion readings can support a suggestion; the driver confirms parking. |
| Geofencing API | Suggest a possible return near the parked car | The driver chooses whether to publish a departure report. |
| Bluetooth connection (optional) | Support parking suggestions when disconnecting from a known car | Requires a paired compatible vehicle or device. |

Location and activity permissions are opt-in, with manual saving, reporting, and area selection available. Occupied feedback requires accepted device-location evidence.

## Parking data sources

Spott will combine parking reference information with dynamic community reports.

| Source | Potential contribution | Caveat |
| --- | --- | --- |
| [OpenStreetMap](https://www.openstreetmap.org/) / [Overpass Turbo](https://overpass-turbo.eu/) | Mapped parking facilities, parking-related street tags, roads and geometry | Street-side parking coverage varies; absence of a tag does not mean parking is forbidden. |
| [Timpark](https://www.timpark.ro/) | Official local parking zones, regulations, tariffs and street coverage | Data format, permissions, and programmatic availability must be verified before import. |
| Community reports | Upcoming departures, confirmed departures, and availability feedback | Reports need validation, deduplication, and expiration. |

Imported parking data will retain source attribution, update timestamps, and verification status. Parking zones describe mapped parking rules; community reports describe observed availability.

### Proposed data model

The [domain model](docs/uml/domain-classes.puml) describes `User`, `SavedCarLocation`, `ParkingDataSource`, `ParkingZone`, `ParkingPosition`, `ParkingReport`, `AvailabilityFeedback`, and `ParkingSearch`.

## Proposed technology stack

| Layer | Technology |
| --- | --- |
| Android app | Kotlin, Android Studio, Jetpack Compose |
| Map | Google Maps SDK for Android |
| Location and activity | Fused Location Provider, Activity Recognition API, Geofencing API |
| Authentication | Firebase Authentication |
| Database / real-time updates | Cloud Firestore |
| Notifications | Firebase Cloud Messaging |
| Backend validation and event processing | Cloud Functions for Firebase |
| Local preferences / optional offline cache | DataStore / Room |
| Data ingestion | Python, Overpass API, GeoJSON/JSON processing |
| Navigation in MVP | Launch Google Maps via intent |


