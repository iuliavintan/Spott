# Spott — Functional Documentation

Spott combines an Android application and backend services for saving a car location, sharing departure reports, discovering parking, and receiving nearby alerts. [Week 8](../milestones/week8.md) implements local unpublished draft CRUD. The authenticated parking functionality described here remains planned.

The [project overview](../../README.md#planned-features) separates the core MVP from the next iteration. Assisted parking suggestions, geofencing, active-search alerts, heatmaps, and the confidence indicator belong to the next iteration.

## Diagrams

| Diagram | Source | Preview | Description |
| --- | --- | --- | --- |
| Use cases | [PlantUML](use-cases.puml) | [SVG](use-cases.svg) | Actors and goals, including departure cancellation. |
| Activity | [PlantUML](parking-activity.puml) | [SVG](parking-activity.svg) | Saving a car location and the parking-report lifecycle, including decisions and exceptions. |
| Sequence | [PlantUML](parking-sequence.puml) | [SVG](parking-sequence.svg) | Fixed-area search, departure reporting, alerts, and proximity checks. |
| Domain classes | [PlantUML](domain-classes.puml) | [SVG](domain-classes.svg) | Entities, values, responsibilities, and relationships. |
| Data flow, level 1 | [Graphviz DOT](data-flow.dot) | [SVG](data-flow.svg) | Processes, external services, data stores, and information flow. |

![Spott use-case diagram](use-cases.svg)

## Actors and boundary

The Spott boundary includes the Android app and its backend. Device location/activity services, the navigation provider, and the push delivery provider exchange information with Spott from outside that boundary.

- **Unauthenticated Driver:** someone creating an account or logging in, including an existing account holder whose session has ended.
- **Authenticated Driver:** someone with an active session using the parking features.
- **Device Location & Activity Services:** supply location and activity signals for parking suggestions, nearby discovery, and occupied-feedback evidence.
- **External Navigation Service:** receives a destination from Spott and provides route guidance.
- **Push Notification Service:** delivers alerts requested by the Spott backend.

Parking operations require authentication.

## Use-case descriptions

| Use case | Trigger and successful result | Alternatives and conditions |
| --- | --- | --- |
| Register Account | An unauthenticated driver requests an account; Spott creates it. | Duplicate or invalid details leave the account uncreated and show a correction message. |
| Log In | An account holder submits credentials; Spott establishes a session. | Failed authentication leaves the driver signed out. |
| Log Out | An authenticated driver ends their session. | Parking operations subsequently require authentication. |
| Save Car Location | The driver selects and explicitly confirms a parking position; Spott saves the private car location. | Device signals can suggest a position for review, correction, or dismissal. Manual selection remains available without usable location data. |
| View Saved Car Location | The driver requests their parked-car position; Spott displays the saved position. | If none exists, Spott explains that a location must first be saved. |
| Adjust Saved Car Location | The driver corrects an existing saved position; subsequent retrieval shows the correction. | Requires a saved position. Cancelling preserves the existing position. |
| Report Upcoming Departure | The driver explicitly publishes a report at the saved or a manually selected position; Spott sets `POTENTIALLY_LEAVING` and its expiration. | A return-to-car prompt can suggest the action. The report does not guarantee availability. |
| Confirm Departure | The author explicitly confirms leaving before the upcoming report expires; Spott sets `RECENTLY_VACATED`, records the confirmation time, and starts its validity interval. | Requires the author's active `POTENTIALLY_LEAVING` report. Device signals can suggest confirmation. |
| Cancel Upcoming Departure | The author cancels an unexpired upcoming report; Spott sets `CANCELLED`. | Requires the author's active `POTENTIALLY_LEAVING` report. |
| Report Spot Availability | The driver submits an observation for an identified report; Spott records accepted available feedback or closes an active report as `OCCUPIED`. | Occupied feedback requires accepted nearby device-location evidence. Available feedback requires an unexpired `RECENTLY_VACATED` report and preserves `expiresAt`. |
| Search Nearby Parking | The driver requests nearby reports; Spott displays matching reports on a map with their status and age. | Uses current location when permitted and available; manual area selection provides an alternative. Empty results are a valid outcome. |
| Filter Parking Reports | While reviewing search results, the driver supplies distance or freshness criteria; Spott displays the matching subset. | Optional. An unfiltered search can complete independently; an empty filtered result is valid. |
| View Parking Details | The driver selects a report; Spott displays its location, status, timestamp, and available supporting information. | A report may expire or change while being viewed. |
| View Parking Heatmap | The driver requests a heatmap; Spott displays aggregated parking activity for the selected area. | Requires sufficient observations. Sparse data produces an insufficient-data message. |
| Navigate to Parking Spot | The driver chooses a reported destination; Spott opens the external navigation service with that location. | Requires a selected destination. If the service is unavailable, Spott reports the failed handoff. |
| Start Parking Search | The driver selects a fixed center, radius in meters, and filters; Spott activates the search subscription. | Only an explicit driver action changes the area. Notification permission and opt-in are required for push alerts. |
| Receive Nearby Parking Alerts | A qualifying new or updated report matches an active search's fixed circle and filters; Spott requests delivery of an alert. | Requires an unexpired report, an active search, and compatible notification preferences and permissions. Opening an alert reads the report's current state. Delivery may be delayed or fail; the map remains available. |
| Stop Parking Search | The driver stops an active search; Spott deactivates its subscription. | Subsequent matching reports do not generate new alerts for that search. Already dispatched messages may still arrive. |

Filtering is optional while reviewing search results:

| Extension | Base use case | Extension point | Condition |
| --- | --- | --- | --- |
| Filter Parking Reports | Search Nearby Parking | When reviewing search results | The driver chooses filtering criteria. |

## Functional rules

1. **Driver confirmation.** Activity, location, and return-to-car signals can generate suggestions. Saving a parking position and confirming departure both require explicit driver actions. Manual saving and reporting remain available.
2. **Private car location and public reports.** Each account has at most one current saved car location. Saving it does not publish a parking report. When reporting, the driver selects the saved position or a position manually.
3. **Cancellation.** Only the author can cancel an unexpired `POTENTIALLY_LEAVING` report. Closed reports cannot be reopened through cancellation.
4. **Expiration.** Upcoming reports have an initial validity interval. Explicit departure confirmation changes the state to `RECENTLY_VACATED` and starts that state's validity interval. Timestamps and expiration use server time.
5. **Nearby occupied feedback.** An authenticated driver can close an active report as `OCCUPIED` only when the backend accepts recent, sufficiently accurate device-location evidence near the reported position. A manually selected map point is not proximity evidence. Missing permission, old data, insufficient accuracy, or excessive distance causes rejection.
6. **Feedback and concurrent updates.** `AVAILABLE` feedback applies only to unexpired `RECENTLY_VACATED` reports and does not extend `expiresAt`. Transitions atomically check the author where required, current state, expiration, and applicable evidence. Delayed feedback does not reopen closed reports.
7. **Fixed-area active search.** The driver selects a center and radius in meters. Phone movement does not change the center; changing the area requires an explicit driver action. Matching reports must fall inside the saved circle and satisfy its filters.
8. **Alerts.** New alerts require an active search, an unexpired matching report, and compatible preferences and permissions. Stopping a search stops new alert generation. Already dispatched messages may arrive later; opening an alert reads the current report state.
9. **Map and navigation.** The map shows report status, age, filters, and details. Ordinary browsing can use current location or a manually selected area. Navigation receives the selected destination. Reads check expiration even before background processing sets the state to `EXPIRED`.
10. **Aggregated and reference data.** Heatmaps require sufficient relevant observations. Cancelled reports are not evidence of a vacated spot. The confidence indicator is heuristic. Imported zones, rules, and categories retain provenance and verification information. A mapped parking zone does not imply current spot availability.

## Report transitions

| From | To | Condition |
| --- | --- | --- |
| New report | `POTENTIALLY_LEAVING` | The driver explicitly confirms publication. |
| `POTENTIALLY_LEAVING` | `RECENTLY_VACATED` | The author explicitly confirms departure before expiration. |
| `POTENTIALLY_LEAVING` | `CANCELLED` | The author cancels before expiration. |
| Either active state | `OCCUPIED` | Occupied feedback is accepted after proximity validation. |
| Either active state | `EXPIRED` | Server time reaches `expiresAt`. |

The active states are `POTENTIALLY_LEAVING` and `RECENTLY_VACATED`. Reports describe community observations rather than reservations or guaranteed availability.

## Parameters to be determined

| Parameter | Purpose |
| --- | --- |
| `upcomingTtl` | Validity interval of an upcoming-departure report. |
| `vacatedTtl` | Validity interval after departure confirmation. |
| `occupiedProximityMeters` | Maximum accepted distance for occupied feedback. |
| `maxLocationAge` | Maximum age of the position used as evidence. |
| `maxLocationAccuracyMeters` | Maximum accepted position uncertainty. |

The domain model groups these values in `ReportPolicy`. Their numeric values and the permitted search-radius limits remain to be determined during implementation. The driver chooses the search radius independently of the occupied-feedback proximity threshold.

## Data flow

The data-flow diagram covers account and saved-car data, reports and feedback, discovery and alerts, and parking reference-data imports. Repeated IDs identify the same external entity or data store.

| Store | Contents |
| --- | --- |
| `D1` | Private driver data, including the account and saved car location. |
| `D2` | Parking reports and accepted availability feedback. |
| `D3` | Parking reference data with source and verification information. |
| `D4` | Fixed search circles, filters, and associated notification preferences. |

## Rendering diagrams

Generate the UML previews with PlantUML and the data-flow preview with Graphviz, from the repository root:

```sh
java -jar path/to/plantuml.jar -tsvg "docs/uml/*.puml"
dot -Tsvg docs/uml/data-flow.dot -o docs/uml/data-flow.svg
```

The existing previews were generated with PlantUML 1.2026.9 and Graphviz 2.44.1.
