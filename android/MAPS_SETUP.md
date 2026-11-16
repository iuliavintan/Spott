# Maps and address search setup

Spott uses MapLibre Native inside Compose, OpenFreeMap map styles/tiles, and Geoapify address autocomplete. Room drafts and the map do not require an API key. Only online address search needs a Geoapify key. No Google Maps or Places key is used.

## Get your Geoapify key

1. Open https://myprojects.geoapify.com/ and sign up or sign in.
2. Open My projects and select Create project.
3. Name it Spott and select OK. Geoapify creates the first API key automatically.
4. Open the project's API keys page and copy the key locally.
5. Open android/local.properties in Android Studio. Keep its existing sdk.dir entry and add:

```properties
GEOAPIFY_API_KEY=your_actual_key
```

6. Sync Gradle, rebuild, and run the app. Search places will now request real Geoapify suggestions.

local.properties is gitignored. Do not put the key in Kotlin, the version catalog, screenshots or commits. The old MAPS_API_KEY property is unused and can be removed. No key was found locally during migration, and the coding agent has no authenticated Geoapify account from which to create/import one.

## Plans and key restrictions

Geoapify's free plan currently has 3,000 credits/day and a 5 requests/second limit; check your dashboard and current pricing before relying on those limits. Search is debounced and only the selected result's returned coordinates are saved, with no separate details API request. Monitor the project's Statistics page.

Use a dedicated development key. Geoapify's referrer/origin restrictions are intended for browsers, and fixed-IP restrictions for servers; they are not equivalent to Google's Android package/SHA-1 restriction. Native mobile requests generally have neither a browser referrer nor a stable outgoing IP. Do not configure a website-only restriction and expect native search to work. For public distribution, put geocoding behind an authenticated/rate-limited backend if the key must be kept secret. A key compiled into an APK can be extracted. Changing CORS does not secure a native client key.

Official guidance: [Projects](https://myprojects.geoapify.com/help/projects/), [API keys](https://myprojects.geoapify.com/help/api-keys/), [pricing](https://www.geoapify.com/pricing/).

## Map configuration and attribution

OpenFreeMap public styles require no API key, registration or Google Cloud billing:

- Light: https://tiles.openfreemap.org/styles/liberty
- Dark: https://tiles.openfreemap.org/styles/dark

The native attribution control remains enabled and is positioned above the bottom sheet. Keep the OpenFreeMap/OpenMapTiles/OpenStreetMap credits provided by the style. Search displays a clickable Geoapify attribution link. The optional device-location button still uses Android foreground permission and the fused location provider; it is independent of both map and search keys.

MapLibre's annotation API is isolated in NativeParkingMap.kt. Long press chooses a point, dragging the temporary pin adjusts it, marker clicks select Room drafts, and explicit camera requests animate the map. Android lifecycle callbacks release native resources when a map screen is disposed. Android Geocoder still supplies optional dropped-pin addresses; manual entry works if it is unavailable.

## Verify

1. With no key, launch and verify the real map loads near Timisoara. Pan, zoom and select points.
2. Add the Geoapify key and rebuild. Search a street/address in Timisoara and another city. Results are softly biased around the viewport, not restricted to it. Coverage/ranking is different from Google Places.
3. Select a suggestion, save a draft, edit its location and delete it. Check markers follow Room changes; text-only drafts remain in Saved drafts.
4. Force-stop/reopen with another saved draft. Check its location persists.
5. Try denied location permission, offline search, quota/key errors and dark mode. None should remove saved drafts.

A map load failure usually means connectivity or tile/style availability; it is unrelated to Geoapify credentials. Search failure may mean an absent/incorrect key, restrictions, quota or network errors. App messages deliberately omit raw exception URLs because URLs contain the API key.
