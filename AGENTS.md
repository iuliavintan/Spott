# Spott agent guidance

Spott is a native Android parking assistant built with Kotlin, Jetpack Compose, and Material 3. The Android project is in `android/`; parking features and Firebase services are planned. Read [README.md](README.md) for the project overview and [docs/uml/README.md](docs/uml/README.md) for functional requirements.

## Skills

Read the relevant `SKILL.md` before applying a skill. Choose skills according to the task; a documentation edit does not require the UI, TDD, or code-review workflows.

| Skill | When to use it for Spott |
| --- | --- |
| [UI/UX Pro Max](.agents/skills/ui-ux-pro-max/SKILL.md) | Design the overall visual experience: map and report hierarchy, saved-car screens, departure actions, colors, typography, spacing, feedback, and accessibility. Use it for visual direction and UX decisions. |
| [Mobile Android Design](.agents/skills/mobile-android-design/SKILL.md) | Translate designs into native Jetpack Compose and Material 3 components, navigation, state handling, adaptive layouts, dark mode, accessibility semantics, and Android touch targets. |
| [Matt Pocock: code-review](.agents/skills/code-review/SKILL.md) | Review a branch, PR, or working changes against repository standards and the requested behavior. Check Kotlin/Compose correctness, lifecycle handling, parking rules, missing tests, and unintended scope changes. |
| [Matt Pocock: tdd](.agents/skills/tdd/SKILL.md) | Develop or fix application logic test-first: parking suggestions, driver confirmation, report transitions, expiration, location-evidence checks, and fixed-area search matching. |
| [Spott README](.agents/skills/spott-readme/SKILL.md) | Write concise English READMEs focused on the project and its current requirements. |
| [UML Use Case Diagram](.agents/skills/uml-use-case-diagram/SKILL.md) | Generate, review, or update PlantUML use-case diagrams from the project's functional requirements. |

## UI design and implementation

- Use UI/UX Pro Max for the visual direction, then Mobile Android Design for native implementation. Reuse the existing theme in `android/app/src/main/java/com/example/spott/ui/theme/` when making UI changes.
- For UI/UX Pro Max, run the installed `.agents/skills/ui-ux-pro-max/scripts/search.py` with a working Python 3 interpreter. Resolve paths from this repository rather than using the upstream `CLAUDE_PLUGIN_ROOT` placeholder.
- Use `--design-system` for a new visual direction. Query `--stack jetpack-compose` separately for implementation guidance.
- Implement with Kotlin, Compose, and Material 3. Apply Android units, components, and minimum 48dp interactive touch targets when adapting general design guidance.
- Show parking-report status and age clearly. Keep upcoming departures visually distinct from recently vacated reports, and provide clear loading, empty, permission-denied, and error states.
- Hoist state where appropriate, keep composables focused, and use accessible labels and previews for relevant screen configurations.

## Application logic and TDD

- Use the current user request as the scope of the change and the functional documentation as the source of parking rules. Preserve the MVP and later-iteration distinction.
- Device signals suggest parking or departure actions; driver confirmation is required. Keep the saved car position private until the driver publishes a report.
- When changing related behavior, cover report ownership, cancellation, server-time expiration, accepted location evidence, and fixed search circles. Use the documented policy parameters rather than inventing production thresholds.
- Agree on public interfaces and test boundaries before starting TDD, as the skill requires. Reuse boundaries already approved in the conversation or an accepted plan.
- Test behavior through public interfaces, one failing test and one minimal implementation at a time. Use deterministic clocks and location/activity inputs for logic tests; avoid real GPS, timing sleeps, and network dependencies in local unit tests.
- Put JVM logic tests in `android/app/src/test/`. Use `android/app/src/androidTest/` for tests that need Android APIs, Compose UI, or a device/emulator.

## Code review and verification

- Establish the review baseline and whether the scope includes committed, staged, or unstaged changes. Include the requested changes in the diff being reviewed.
- For the Standards axis, use this file and the surrounding Kotlin/Compose conventions. For the Spec axis, use the user's request, an accepted plan, or a supplied issue/spec, with the functional documentation providing domain constraints. No external issue tracker is configured for this project; local reviews can use these sources directly.
- Keep standards findings separate from requirements findings, following the code-review skill. Cite concrete files and behavior, and distinguish confirmed defects from judgment calls.
- Run checks appropriate to the change. From `android/`, the Windows Gradle commands are:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

The last command requires a connected device or emulator. Report which checks ran and any material limitations; documentation-only changes do not require an Android build.

## Installed sources

The four imported skills retain their upstream instructions and supporting files. Spott-specific usage guidance belongs in this file.

| Skill | Upstream snapshot |
| --- | --- |
| UI/UX Pro Max | [nextlevelbuilder/ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/tree/e59ff376a41f7ca47b7541ad5c116495cbade3ee/.claude/skills/ui-ux-pro-max) |
| Mobile Android Design | [wshobson/agents](https://github.com/wshobson/agents/tree/9c61ab243def98f72da12abd41cfcf5bcd91af9d/plugins/ui-design/skills/mobile-android-design) |
| Matt Pocock: code-review | [mattpocock/skills](https://github.com/mattpocock/skills/tree/5863806dd4e15c63174bee4e7515a4256792ecfa/skills/engineering/code-review) |
| Matt Pocock: tdd | [mattpocock/skills](https://github.com/mattpocock/skills/tree/5863806dd4e15c63174bee4e7515a4256792ecfa/skills/engineering/tdd) |
