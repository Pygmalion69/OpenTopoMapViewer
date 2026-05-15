# Test Plan for OpenTopoMapViewer

This document proposes a set of **unit tests** and **instrumentation tests** for the OpenTopoMapViewer Android project.  The goal is to improve confidence in core business logic (networking, database access, utilities) and to exercise important UI workflows (map interactions, GPX handling, and settings).  Each proposed test is accompanied by a rationale, suggested tools/libraries, and references to the code base for context.  The coding agent should create the corresponding test files under the appropriate packages using JUnit 4 and AndroidX test infrastructure.

## Prerequisites

1. **Testing dependencies:** Ensure the following libraries are available in the project’s `build.gradle` (they are typically already configured but add if missing):
   - JUnit 4 (`testImplementation` for unit tests).
   - AndroidX Test (`androidx.test.ext:junit`, `androidx.test.espresso:espresso-core`) for instrumentation tests.
   - [Mockito](https://site.mockito.org/) or [MockK](https://mockk.io/) for mocking collaborators (e.g., `OrsClient`).
   - `kotlinx-coroutines-test` for coroutine-based tests.
   - Room testing libraries (`androidx.room:room-testing`) for DAO tests.
2. **Test structure:** Place unit tests under `app/src/test/java/org/nitri/opentopo` and instrumentation tests under `app/src/androidTest/java/org/nitri/opentopo` using the same package structure as the production code.
3. **Helper functions:** For LiveData assertions, implement or import a `getOrAwaitValue()` extension to retrieve values from `LiveData` in tests.

## Unit Tests

### 1. `Directions.getRouteGpx()`

- **Goal:** Verify that the `Directions` class correctly calls the `OrsClient`, processes the result, and invokes the appropriate callback.
- **Approach:** Use Mockito/MockK to create a fake `OrsClient` that returns a GPX string or throws an exception.  Use `kotlinx-coroutines-test` to run coroutines on a test dispatcher.
- **Tests:**

| Test case | Scenario | Expected behaviour |
|---|---|---|
| `testGetRouteGpx_successInvokesOnSuccess` | Fake `OrsClient.getRouteGpx()` returns a non‑blank string (e.g., `<gpx></gpx>`). | `Directions.RouteGpxResult.onSuccess()` is called with the returned string; `onError()` is **not** invoked. |
| `testGetRouteGpx_emptyResponseInvokesOnError` | Fake `OrsClient.getRouteGpx()` returns an empty or blank string. | `Directions.RouteGpxResult.onError()` is called with "Empty response body". |
| `testGetRouteGpx_exceptionInvokesOnError` | Fake `OrsClient.getRouteGpx()` throws an exception. | `Directions.RouteGpxResult.onError()` is called with a message containing `"Failed to fetch GPX"`. |

*Relevant code:* `Directions.getRouteGpx()` launches a coroutine on `Dispatchers.IO` and delegates to `RouteHelper` and `OrsClient`; it calls `result.onSuccess()` or `result.onError()` accordingly【477220888047471†L9-L35】.

### 2. `Utils.area()` and waypoint helpers

- **Goal:** Validate bounding‐box calculations and waypoint helpers.
- **Approach:** Create dummy `Gpx` objects with tracks, routes, and waypoints using the GPX parser domain classes.  Assert that the returned `BoundingBox` values match the expected min/max latitude/longitude.  For the list helpers, verify that unique types are sorted and that filtering by type works.
- **Tests:**

| Test case | Scenario | Expected behaviour |
|---|---|---|
| `testArea_usesTrackPointsWhenPresent` | A `Gpx` containing track segments with several `TrackPoint`s. | `Utils.area(gpx)` returns a `BoundingBox` whose north/east/south/west equal the max/min latitudes and longitudes of all track points【280826560027977†L42-L55】. |
| `testArea_fallsBackToRoutePoints` | A `Gpx` with no tracks but with routes/routePoints. | `Utils.area(gpx)` uses the route points to compute bounds【280826560027977†L48-L55】. |
| `testArea_usesWayPointsWhenNoTracksOrRoutes` | A `Gpx` with only waypoints. | Bounding box uses waypoint coordinates. |
| `testGetWayPointTypes_returnsSortedDistinctTypes` | Supply a `Gpx` with waypoints of types `"summit"`, `"cafe"`, and an empty type. | `Utils.getWayPointTypes(gpx, defaultType)` returns a sorted list of unique types, with the default type in place of empty ones【280826560027977†L115-L125】. |
| `testGetWayPointsByType_filtersCorrectly` | Provide a `Gpx` with various waypoint types. | `Utils.getWayPointsByType(gpx, type)` returns only waypoints whose `type` matches the parameter or empty when both are blank【280826560027977†L135-L145】. |
| `testConvertRouteToTrack_createsSyntheticTrackWhenNoTrackExists` | A `Gpx` with only a route of several points. | The returned `Gpx` contains a `Track` with the same number of `TrackPoint`s as the original route, and a `Route` inside. |
| `testConvertRouteToTrack_returnsOriginalWhenTrackPresent` | A `Gpx` that already has at least one track. | The returned object is the same instance (no conversion)【280826560027977†L155-L156】. |

### 3. `Utils.elevationFromNmea()`

- **Goal:** Ensure NMEA strings are parsed correctly for elevation.
- **Tests:**

| Test case | Scenario | Expected behaviour |
|---|---|---|
| `testElevationFromNmea_validSentenceReturnsElevation` | NMEA sentence `$GPGGA, …,100.0,M,…`. | Returns `100.0` as a `Double`【280826560027977†L248-L265】. |
| `testElevationFromNmea_invalidSentenceReturnsNoElevationValue` | NMEA sentence with wrong prefix or missing data. | Returns `Utils.NO_ELEVATION_VALUE` (converted to double). |

### 4. `Utils.getBitmapFromDrawable()`

- **Goal:** Test conversion of vector drawables to bitmap.
- **Approach:** Use an Android instrumentation test (because it depends on a `Context`) with `ContextThemeWrapper` and a known vector drawable.  Check that the resulting `Bitmap` dimensions match the drawable’s intrinsic size and that passing `null` context throws `IllegalArgumentException`【280826560027977†L274-L284】.

### 5. `Utils.getOsmdroidBasePath()`

- **Goal:** Verify correct path selection between external and internal storage.
- **Approach:** In a unit test (using Robolectric or instrumentation), call `getOsmdroidBasePath(context, false)` and assert that the returned path equals `context.cacheDir/osmdroid`; call `getOsmdroidBasePath(context, true)` and assert it points to `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)/osmdroid` on API 26+, else `Environment.getExternalStorageDirectory()/Download/osmdroid`【280826560027977†L299-L312】.

### 6. `Utils.clearOsmdroidSqliteCache()`

- **Goal:** Ensure leftover OSMDroid SQLite files are removed and preferences are persisted.
- **Approach:** Set up a temporary directory to mimic the OSMDroid tile cache with dummy `cache.db` files; call `clearOsmdroidSqliteCache(context)`; assert that files no longer exist and that preferences contain the expected keys【280826560027977†L319-L345】.

### 7. `MarkerDao` and `OverlayDatabase`

- **Goal:** Verify CRUD operations on `MarkerModel` via Room.
- **Approach:** Use Room’s in-memory database (`Room.inMemoryDatabaseBuilder`) in unit tests.
- **Tests:**

| Test case | Scenario | Expected behaviour |
|---|---|---|
| `testInsertAndGetMarkers_returnsInsertedMarker` | Insert a `MarkerModel` via `MarkerDao.insertMarker()`. | `getAllMarkersNow()` returns a list containing the inserted marker. |
| `testUpdateMarker_updatesFields` | Insert a marker, modify a field (e.g., name), call `updateMarker()`. | The updated marker is retrieved with the new value. |
| `testDeleteMarkerById_removesCorrectMarker` | Insert two markers then delete one by ID. | Remaining list contains only the non‑deleted marker. |
| `testDeleteMarkersByIds_removesMultipleMarkers` | Insert several markers and delete two by IDs. | The remaining markers list excludes the deleted ones. |
| `testLiveData_getAllMarkers_emitsValues` | Observe `LiveData` returned by `getAllMarkers()` in a test observer, insert a marker, and assert that the emitted list changes. |

### 8. `MarkerViewModel`

- **Goal:** Ensure ViewModel methods interact correctly with the DAO and reflect route waypoint state.
- **Tests:**

| Test case | Scenario | Expected behaviour |
|---|---|---|
| `testHasRoutePoints_returnsTrueWhenMarkerIsWaypoint` | Insert a marker with `routeWaypoint = true` into the database. | `MarkerViewModel.hasRoutePoints()` returns `true`【351052727112042†L43-L45】. |
| `testAddMarker_invokesDaoInsert` | Mock `MarkerDao` and verify that `insertMarker()` is called when `MarkerViewModel.addMarker()` is invoked. |
| `testRemoveMarker_invokesDaoDelete` | Mock `MarkerDao` and verify `deleteMarkerById()` call when removing a marker. |

### 9. `MapFragment` helpers

- **Goal:** Test small pure functions in `MapFragment` that do not require UI.
- **Tests:**

| Test case | Scenario | Expected behaviour |
|---|---|---|
| `testReadMaxZoomLevel_returnsDefaultWhenPreferenceEmpty` | Use a `SharedPreferences` without the key `PREF_MAX_ZOOM_LEVEL`. | `MapFragment.readMaxZoomLevel()` returns the constant `DEFAULT_MAX_ZOOM`【866924116105185†L1301-L1306】. |
| `testReadOpenTopoMapSource_returnsPreferenceValue` | Store a custom string (e.g., `"opentopomap"`) in `SharedPreferences` under `PREF_OPEN_TOPO_MAP_SOURCE`. | `readOpenTopoMapSource()` returns that value【866924116105185†L1308-L1311】. |

Because most of `MapFragment` is tightly coupled to Android UI and OSMDroid, extensive logic should be exercised via instrumentation tests (see below).

## Instrumentation (UI) Tests

The following tests should be placed in `androidTest` and use `ActivityScenario` or `FragmentScenario` with Espresso.  These tests run on an emulator or device.

### 1. Map long press adds a marker

- **Goal:** Verify that a long press on the map adds a new marker and updates the `MarkerViewModel`.
- **Steps:**
  1. Launch `MainActivity` (FOSS or Play flavor) with `ActivityScenario`.
  2. Wait for the map to initialise (e.g., using `IdlingResource` or a short `Thread.sleep` if necessary).
  3. Perform a long press on the `MapView` at a given location using Espresso’s `GeneralLocation.CENTER` or coordinates.
  4. Observe the `MarkerViewModel` (obtain via `ViewModelProvider` on the activity) or check UI: there should be one more marker than before.  Alternatively, verify that a marker icon appears on the map.
- **Expected:** A new `MarkerModel` is inserted with `seq` incremented and latitude/longitude equal to the pressed point【866924116105185†L244-L262】.

### 2. Toggle follow mode via menu

- **Goal:** Ensure that selecting the "Follow" action toggles follow mode and the corresponding menu items update.
- **Steps:**
  1. Launch `MainActivity` and ensure location permissions are granted (use `GrantPermissionRule`).
  2. Open the overflow menu (action bar) and click `R.id.action_follow`.
  3. Verify that the follow flag is set: the "Follow" menu item is hidden and "No Follow" is visible【866924116105185†L874-L883】.
  4. Click `R.id.action_no_follow` and verify that the original state is restored.
- **Expected:** Menu item visibility toggles appropriately and `MarkerFragment` disables/enables follow accordingly.【866924116105185†L874-L883】

### 3. GPX file import displays track and waypoints

- **Goal:** Test that selecting a GPX file through the UI triggers parsing, loads overlays, and zooms to the GPX bounds.
- **Steps:**
  1. Place a small test GPX file in `device/emulated/0/Download` or use `DocumentsContract` with `ActivityScenario` to simulate file selection (Espresso-Intents may be required).
  2. Launch `MainActivity` and click the GPX action in the menu (`R.id.action_gpx`).  Use `Intents.init()` to stub the file‑picker result so that `parseGpx()` is invoked.
  3. Wait for parsing; then assert that the map has overlays present (`OverlayHelper.hasGpx()` returns true【26256360127586†L301-L303】) or that the GPX detail menu items (`R.id.action_gpx_zoom`, `R.id.action_gpx_remove`) become visible【866924116105185†L883-L887】.
- **Expected:** The GPX track and waypoint overlays are drawn on the map and the GPX menu items appear.

### 4. Switching base map and overlay via layers menu

- **Goal:** Ensure that selecting different base maps or overlays changes the tile source and copyright notice.
- **Steps:**
  1. Launch `MainActivity` and open the layers popup (`R.id.action_layers`).
  2. Click each base map option (OpenTopoMap, OpenStreetMap, OpenHikingMap, Freemap.sk) and check that `MapView.tileProvider.tileSource.name` matches the expected string (`"OpenTopoMap"`, `"MAPNIK"`, etc.)【866924116105185†L522-L545】.
  3. Select hiking or cycling overlays and verify that the overlay is added to the first position in the overlay list (use reflection or `MapView.getOverlays()`), and that the copyright notice includes the overlay’s text【26256360127586†L305-L342】.
- **Expected:** Tile source and overlay update correctly, and the copyright view shows the combined copyright strings【866924116105185†L599-L621】.

### 5. Fullscreen toggle on map tap

- **Goal:** Verify that tapping the map toggles fullscreen (when the preference `PREF_FULLSCREEN_ON_MAP_TAP` is true).
- **Steps:**
  1. In `SharedPreferences`, set `PREF_FULLSCREEN_ON_MAP_TAP = true` before launching `MainActivity`.
  2. Tap anywhere on the map using Espresso.
  3. Assert that the action bar is hidden and the system bars are hidden (can use `ActivityScenario` and check `WindowInsetsControllerCompat`).
  4. Tap again and assert that the bars reappear.
- **Expected:** The `isFullscreen` flag toggles and the UI responds as in `BaseMainActivity.applyFullscreen()`【276620311802621†L214-L258】.

## Implementation Notes

1. **Handling asynchronous work:** Many classes rely on coroutines and LiveData.  Use `runBlockingTest` or the new `runTest` from `kotlinx-coroutines-test` to control coroutine execution.  Replace `Dispatchers.IO` with `StandardTestDispatcher` where necessary.
2. **Mocking Room:** For ViewModel tests, provide a mocked `MarkerDao` or use an in‑memory Room database to test actual persistence.
3. **Instrumentation environment:** Some utilities (e.g., `getBitmapFromDrawable`, `getOsmdroidBasePath`) require an Android context; implement their tests in `androidTest` using `ApplicationProvider.getApplicationContext()`.
4. **Test data:** Create small GPX samples within test resources.  For instrumentation tests, copy the sample GPX into the device’s storage or stub `ActivityResult` to return a URI pointing to a test asset.
5. **Ensure deterministic tests:** When dealing with UI and asynchronous operations, consider using `IdlingResource` or explicit `CountDownLatch` to wait for operations to complete instead of arbitrary sleeps.

With these tests in place, the project will have better coverage across critical components: network routing, database persistence, GPX parsing and utilities, and key UI flows (marker creation, map settings, GPX import).  The coding agent should now implement each of the above test cases following the guidelines.
