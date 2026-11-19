# Startup viewport investigation

## Confirmed sequence

The supplied `diff_nearby_complete.txt` was reviewed as context; the fix is based on the current checkout.

1. With empty preferences, `initializeMapState` correctly selects Kleve (51.78962, 6.14120), zoom 15.
2. In the bundled osmdroid 6.1.20, `MapController.setCenter` queues the request when `isLayoutOccurred` is false. `onFirstLayout` replays it. Before that, `mapCenter` still reports (0, 0).
3. `mapViewInitialized` only meant overlays were initialized. Both preference saving and instance-state saving could read that uninitialized center during startup pause/recreation. Once saved, (0, 0) correctly passes coordinate validation and takes precedence over the fallback and location.
4. `onPause` separately cleared `awaitingFirstFix`. Thus a permission/lifecycle interruption could abandon first-fix centering even when a later regular location update arrived. Startup centering depended on a separate overlay `runOnFirstFix` callback, not the fragment's regular GPS/network listener.
5. The activity originally created the map only after permission was already granted or the permission result arrived.

Before changing production behavior, an instrumentation test on a Pixel 3a / Android 12 confirmed (0, 0) before layout and Kleve after layout using the old controller sequence. Its initial post-layout tolerance was too tight for osmdroid projection rounding; the regression uses a 0.0001-degree tolerance. This reproduces the causal pre-layout window deterministically, not a spontaneous full-app intermittent failure.

## Changes

- Create the map independently of permission. On a permission result, restart subscriptions if resumed; otherwise `onResume` registers them.
- Seed osmdroid's expected center synchronously, so its initial projection has the selected viewport without waiting for controller replay.
- Save the selected viewport before layout, and save nothing before viewport initialization. After layout, save the validated displayed viewport. Coordinate (0, 0) remains valid.
- Persist pending first-fix intent with preferences and instance state; preserve it across pause/resume and view recreation.
- Consume a valid regular `onLocationChanged` update once when first-fix centering is pending. Invalid coordinates do not consume it.
- Cancel pending centering on deliberate map touch/gestures or explicit navigation. Programmatic zoom notifications do not masquerade as user interaction.
- Explicit coordinate navigation no longer waits an arbitrary 500 ms; before layout it sets the expected center directly.

Existing saved (0, 0) coordinates are intentionally not migrated: they cannot be distinguished from a deliberate legitimate viewport.

## Regression coverage

`MapStartupLayoutTest` exercises the actual osmdroid map before/after first layout, both the deferred old controller behavior and the immediate expected-center behavior.

`MapStartupLifecycleTest` exercises the fragment with controlled unavailable/delayed location, pause/resume, one-shot regular updates, invalid and (0, 0) fixes, interaction cancellation, explicit destinations, instance-state restoration, actual activity recreation, and persistence before layout/before initialization. These tests deliberately isolate location delivery from real GPS timing.

Both debug APKs compile. Both JVM suites pass (42 tests per variant). Both device suites pass on Pixel 3a / Android 12 (two layout tests and four lifecycle tests per variant). Run instrumentation classes separately and inspect the XML test counts; the initial combined class filter only executed the layout class in this environment.

Commands (replace `Foss` with `Play` for the other distribution):

```sh
./gradlew :app:assembleFossDebug :app:testFossDebugUnitTest --offline
./gradlew :app:connectedFossDebugAndroidTest --offline --no-configuration-cache \
  -Pandroid.testInstrumentationRunnerArguments.class=org.nitri.opentopo.MapStartupLayoutTest
./gradlew :app:connectedFossDebugAndroidTest --offline --no-configuration-cache \
  -Pandroid.testInstrumentationRunnerArguments.class=org.nitri.opentopo.MapStartupLifecycleTest
```

## Device checks

With clean application data and device location services disabled, FOSS passed actual permission denial and foreground permission grant. After background/resume and landscape rotation, preferences contained latitude 51.789623, longitude 6.1411858, zoom 15, and pending_first_fix=true (projection rounding around Kleve). Permission states were checked explicitly. The map view was present in the UI hierarchy. Device location and rotation settings were restored after the checks.

Play also retained Kleve and pending first-fix intent through background/resume and landscape rotation with permission observed as granted and location services disabled. The attempted Play denial did not produce a denied permission state; a repeat did not expose the expected permission button. Those UI attempts are not counted as a denial pass.

Remaining device validation: repeat the clean Play permission-denial flow interactively, a naturally delayed GPS/network fix delivered by LocationManager (as opposed to controlled callback injection), approximate-only permission on newer Android versions, and other vendor lifecycle behavior. The deterministic reproduction does not establish the frequency of the original intermittent full-app symptom.
