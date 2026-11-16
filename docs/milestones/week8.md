# Week 8 — CRUD and persistence

Spott now runs as a local CRUD prototype for unpublished parking-report drafts. Drafts are stored in Room and survive closing the process. They are not community reports, confirmed available spaces, or the driver's saved car position.

## Grading evidence

| Criterion | Demonstration |
| --- | --- |
| CREATE | New draft → enter location and optional coordinates/notes → Save draft. |
| READ | Home observes stored drafts; selecting a card opens complete details. |
| UPDATE | Edit draft → change fields → Save draft. The existing ID and creation time remain. |
| DELETE | Delete draft → Delete in the confirmation dialog. The row is removed and Home opens. |
| PERSISTENCE | Save a draft, force-stop the app, then relaunch. The draft remains. |

## Architecture and storage

Compose screens send user actions to screen-scoped ViewModels. ViewModels use `ParkingDraftRepository`; `RoomParkingDraftRepository` validates input and calls `DraftDao`. Room generates SQL adapters and observes invalidation, returning changes through Flow. The screens collect StateFlow with lifecycle awareness.

`SpottApplication` owns a lazily created repository and file-backed `spott-drafts.db`. Dependency wiring is manual. No DI framework or backend is required.

Database version 1 has one `parking_report_drafts` table: UUID text primary key, required location, nullable latitude/longitude, notes (empty string means absent), and creation/modification times as epoch milliseconds. The exported schema is in `android/app/schemas/`. Future schema changes require explicit migrations; destructive migration is not enabled.

The shared validator rejects blank locations, unpaired coordinates, nonnumbers, NaN/infinity, latitude outside −90…90, and longitude outside −180…180. Text is trimmed before persistence. Coordinate input uses a decimal dot. Device time is injected into the repository for deterministic tests; update timestamps cannot move backward. These timestamps do not provide server authority or expiration.

## Screens and state

- Home: local-prototype notice, unpublished draft cards sorted by latest modification, notes excerpts, timestamps, New draft action, loading/error/empty states.
- Create/edit: shared scrollable form, field errors, save/cancel/back, persisted values loaded once for editing. `SavedStateHandle` retains unsaved input through recreation and supported process restoration.
- Details: location, notes, optional coordinates, both timestamps, edit/delete actions, deletion confirmation and missing-record behavior.

Writes disable conflicting interactions and navigation until completion. Storage failures retain input and allow retry. Navigation follows successful writes. Cancel does not write. Missing records are not recreated by an edit. Theme colors, dynamic color, system dark mode, 48dp touch targets and width-constrained layouts use the existing Material 3 foundation.

## Tests and manual verification

From `android/`, run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:lintDebug
```

Connected tests need a device/emulator. JVM tests cover validation. Instrumentation tests use real named Room databases and a deterministic clock for CRUD, reactive ordering, rejected writes, deletion, and reopening a database. Compose tests exercise the actual navigation workflow, deletion cancellation, validation and activity recreation. Test databases use unique names and are removed afterward.

For an Android Studio demonstration:

1. Open `android/`, sync Gradle, and run on an API 26+ emulator/device.
2. Tap New draft. Save with an empty location to show validation.
3. Enter “University library”, coordinates `45.75` and `21.23`, and a note. Save.
4. Show the details, timestamps, and unpublished label. Back shows the stored card.
5. Open it, choose Edit draft, change its notes/location, and save. Confirm there is one updated card.
6. Force-stop using Android Settings → Apps → Spott → Force stop, and launch Spott again. Open the same draft and show its edited values. Do not clear storage or uninstall.
7. Choose Delete draft, then Keep draft to demonstrate cancellation. Repeat and confirm Delete; show that the card disappears.
8. Create another draft and rotate while typing. Check the values remain. Check light/dark mode, large text, keyboard scrolling, and TalkBack labels.

See [the guide](week8-guide.md) for the source inventory, execution flows and oral-exam preparation. Final verification evidence is recorded there.

## Relationship to UML and future work

The original diagram sources remain unchanged. Their `ParkingReport` is a published observation with author restrictions, server-time expiration and retained history. A local draft is an implementation concept preceding any future publication; it is not another `ReportStatus`. Deleting a draft therefore does not delete report history. Draft coordinates are typed input, not accepted location evidence.

Week 8 provides UI/persistence responsibilities supporting later report preparation. It does not complete Report Upcoming Departure, Save Car Location, View Parking Details for published reports, authentication, discovery or any backend sequence. Room's draft store is not the final DFD's community reports/feedback store D2.

Postponed: Firebase Authentication, Firestore synchronization, publication and authorized transitions, report expiration, location/activity services, map integrations, navigation handoff, fixed-circle searches, notifications, predictions, heatmaps and background monitoring. A future implementation diagram may add a local draft component without changing the published domain lifecycle.

Suggested next stages (not official grading specifications): Week 10 can add authentication and explicit publication/backend validation through a separate published-report boundary; Week 12 can complete discovery/integration and assess later-iteration device signals/alerts against requirements. Draft CRUD should remain independent.
