# Managed local XYZ packages — #104, Steps 2–3

Step 2 baseline: feature/local-layer at 5cdeb6a. The Step 2 validation record below is historical; Step 3 adds the display controls described here.
Settings → Local maps opens the installed catalog and its Import local map action.
The old Import offline tiles action still writes to the background cache.
Importing a managed package does not attach it to the map.

## Storage, identity and backup

Production storage is Context.noBackupFilesDir/local-xyz/:

- packages/<UUID>/tiles.zip: retained, indexed ZIP; no tile-tree extraction.
- packages/<UUID>/metadata.properties: version, ID, display name, actual zoom
  levels, count, longitude/latitude envelope and optional attribution.
- staging/<UUID>/: copy and validation in progress, on the same filesystem.
- staging/removed-<UUID>/: retired package awaiting deletion.
- store.lock: cross-process file lock.

Each import generates a new UUID, even for identical input. Names, roots and
coordinates never determine identity. LocalPackageRepository exposes import,
list, lookup, open and remove by ID; open returns a LocalXyzArchive whose sourceId
is that ID. Android document access lives in LocalMapsViewModel, storage and
validation in the repository, and indexed rendering reads in LocalXyzArchive.
All package operations run on Dispatchers.IO. The UI never resolves a URI path.

The ZIP is copied and synced, validated, and metadata written and synced before
a single directory rename publishes both files. Catalog enumeration only reads
packages/, never staging/. A process-wide mutex and store.lock serialize imports,
recovery, reader acquisition and removal. Recovery deletes staging only with both
locks held; a live import cannot be swept. A crash before publication leaves
recoverable staging; a crash after publication leaves the complete package.
Cancellation is checked while copying, between entries, and before publication.
Publication is the commit point: cancelling afterward retains the installed package.

Removal closes registered readers (synchronized against active reads), renames
only that UUID out of packages/, then deletes it. Closed readers return no tiles.
Reader references are weak; the catalog does not keep unused tile indexes alive.
The original document and other IDs are untouched.

noBackupFilesDir excludes these large, reimportable ZIPs AND their catalog metadata
from Android cloud backup and device-transfer backup. Reimport after reinstall or
moving to another device. Normal map-cache clearing does not reference this store.
The debug proof remains at files/local-layers/caperrino-proof/ and is never scanned
as a production package. No historical background-cache tiles are migrated.

## Input contract and limits

Web Mercator XYZ: z/x/y.png, optionally under exactly one common directory
(e.g. OpenTopoMap/15/17846/12351.png). Y starts at the NORTH and increases SOUTH.
TMS has the opposite Y axis; coordinate ranges cannot detect it. The importer
does not guess, convert or flip TMS data. The user must supply XYZ.

Tiles must be readable 256 × 256 PNGs, zoom 0–29, with x and y in [0, 2^z).
Zoom levels are persisted as the actual sorted set, including holes. Bounds use
the outer edges of all tiles, in geographic degrees. They are an envelope;
neither missing zooms nor sparse holes imply coverage. Antimeridian packages
may have a wide envelope.

Optional attribution is supplied explicitly as UTF-8 text in a ZIP-root file
named attribution.txt (outside any common tile root). It is preserved and shown
verbatim; folder names never supply attribution. Display name comes from the
document provider, capped at 200 characters; a localized fallback is used if absent.

Limits are independent of osmdroid cache preferences:

| Resource | Limit |
| --- | --- |
| Copied ZIP bytes | 512 MiB |
| All ZIP entries, including directories and metadata | 50,000 |
| Central directory | 32 MiB |
| Inflated bytes per PNG tile | 2 MiB |
| Inflated bytes per non-tile entry / attribution | 64 KiB |
| Total inflated entry bytes | 2 GiB |
| Entry path length | 1,024 characters |

A preflight checks the ordinary ZIP end record and central-directory entry count
before ZipFile allocates its index. ZIP64, split, encrypted and compression methods
other than stored/deflate are unsupported. Declared sizes and actual stream reads
are bounded. Every entry is read, its actual size and CRC checked. Empty ordinary
directory entries are allowed. Harmless non-tile files are ignored after bounded
validation, except attribution.txt. PNG-looking paths and numeric malformed tile
paths are rejected rather than silently discarded. Thus ancillary image thumbnails
and numeric metadata directory names are unsupported; omit them from the ZIP.

Reject absolute paths, backslashes, drive/URI colons, control characters, empty,
dot or parent components, duplicate names or XYZ coordinates (including alternate
numeric spellings), mixed roots, bad coordinates, and archives without tiles.
PNG validation checks signature, chunk CRCs, critical chunks, IHDR, complete
zlib data, expected scanline lengths/filters (including Adam7), then Android
BitmapFactory decoding. Decoded bitmaps are immediately recycled. ZIP and PNG
truncation/corruption fail the entire import. One entry/tile is held at a time;
no decoded tiles accumulate. Write/space failures clean staging without publication.

## UI and lifecycle

The ViewModel owns one import job across rotation; busy state prevents resubmission.
Copy progress reports KiB, validation reports entries. Cancel cleans staging before
re-enabling Import local map. Returning from the picker without a document is safe.
Document-open errors explain pending downloads/unavailable documents and offer retry
through the same action. Validation and storage errors are localized; exceptions
and stack traces stay in logs. The picker’s temporary read grant survives configuration changes; the importer
neither acquires nor releases persistent grants owned by other features. Leaving the management activity cancels its ViewModel job. Process
death interrupts rather than automatically retries an import; the next catalog
load recovers abandoned staging.

Packages list their name, full ID (to distinguish identical names), actual zooms,
tile count and supplied attribution. Removal confirms name and ID.
Step 3 adds visibility, selection, opacity and Show on map (see below).

All 16 new string keys are present in values plus values-de, -es, -fr, -it and -nl.
Cancel uses Android's localized system string. Existing translations are unchanged.

## Validation record

On 2026-11-17, final JVM/build validation passed:

```sh
LOCAL_XYZ_SAMPLE=/home/helfrich/Downloads/Caperrino_15_18.zip ./gradlew testFossDebugUnitTest assembleFossDebug testPlayDebugUnitTest assemblePlayDebug --console=plain --no-daemon
git diff --check
```

Final build: BUILD SUCCESSFUL in 26s. Both flavor suites passed. The optional sample
test actually ran with the environment variable above and verified 1,692 tiles,
actual zooms 15,16,17,18 and reopening via the local reader. No sample is tracked.

LocalPackageRepositoryTest covers two same-name/same-root overlapping packages
with different PNG contents, independent reads by ID, source deletion, a new
repository instance, metadata/attribution, live-reader removal, unsafe/mixed/bad
paths, duplicate coordinates, bad dimensions, bad/truncated PNGs, empty/invalid
ZIPs, archive/entry/tile/metadata/total-read limits, document-open failure,
cancellation in both phases, metadata-write failure, abandoned staging recovery,
and recovery waiting for a live import. LocalMapTranslationsTest checks all 16
keys and formatting arguments across every maintained strings.xml locale.
LocalXyzArchiveTest covers generic roots, exact reads, missing tiles and close/reopen.

Pixel 3a (016AY1RYD7), Android 12, FOSS debug observations:

- Installed over the existing APK with adb install -r; no initial uninstall.
- Opened Settings → Local maps → Import local map. Copied the workstation sample
  to the uniquely named device file Download/OpenTopo-Step2-Caperrino.zip,
  scanned it and selected it through DocumentsUI.
- Observed validating progress (1458 / 1747 entries), then a successful catalog
  row: 1692 tiles, zooms 15,16,17,18, ID
  d83f06cf-5cb6-441b-9a8a-38bfdbd13b53. Import did not add a map overlay.
- Rotated to landscape and observed the same one-row catalog. This establishes
  completed-catalog rotation, NOT a proven in-flight import rotation.
- Moved only the new device source to .zip.unavailable, force-stopped and
  relaunched the app. The targeted reader instrumentation below successfully
  listed/looked up that ID and decoded its managed tile at 15/17846/12351.
- Targeted LocalPackageDeviceTest ran two tests, zero failures/errors/skips.
  Its second test imported Android-generated red and blue PNG packages with
  identical name/root/coordinate, verified pixels through each ID, removed one
  with readers open, and verified the blue package remained readable.
- After reinstall (see incident below), manually selected the sample and tapped
  Cancel during import. Observed “Import cancelled” and “No local maps installed.”
- Original rotation settings were restored (accelerometer_rotation=1,
  user_rotation=0). The new device source was moved back to its original
  OpenTopo-Step2-Caperrino.zip test filename. Workstation sources were untouched.

Instrumentation command actually run:

```sh
./gradlew connectedFossDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.nitri.opentopo.util.LocalPackageDeviceTest -Pandroid.testInstrumentationRunnerArguments.localPackageId=d83f06cf-5cb6-441b-9a8a-38bfdbd13b53 --console=plain --no-daemon
```

**Device-test incident:** the Gradle connected-test cleanup uninstalled the app
after the two successful tests. This was an agent workflow mistake. Reinstalled
the FOSS APK; the managed sample ZIP was confirmed absent afterward. Prior private
data may also have been erased; recovery of it is not claimed. This uninstall is
not evidence about restart or cache clearing. Do not repeat connected-test cleanup
on a device containing user data. Install app/test APKs without uninstalling and
run am instrument directly, or use a disposable emulator.

The last tiny production change (reject an unexpectedly occupied UUID destination
instead of allowing rename replacement) passed the final JVM/build run after the
instrumentation run. Instrumentation was not rerun for that guard.

Remaining device checks: in-flight rotation, removal confirmation UI, pending
Downloads/open-error UI, actual process death during import, real ENOSPC,
Clear map cache with subsequent managed reader verification, and the legacy
background import smoke test. These are NOT claimed as device-tested. Cache
isolation is established by storage layout/code inspection and JVM store tests,
not by this Step 2 device run. UI state inspection intermittently failed inside
Android's uiautomator dumper; later the foreground app changed independently of
the test actions, so further device interaction stopped. No release builds,
full instrumentation suite, Step 3 rendering or map-control tests were run.

## Step 3 user flow and rendering

Settings → Local maps → Import local map installs a package without changing the
map. Each installed row includes **Show on map**. This selects its UUID, shows it,
returns directly to the map, and suspends follow using the same behavior as
explicit GPX navigation. The viewport fits the coverage at a zoom actually present
in the ZIP. If no tile at that zoom intersects the resulting viewport, it centers
a known tile. A hole at the center does not cause recentering when edge tiles are visible.

On the map, open the existing layers menu → **Local map**. Choose one package
(name and full UUID distinguish identical names), toggle **Visible**, or move the
**Opacity** slider from 0% to 100%. Initial opacity is 65%. Selection here keeps
the viewport in place, except when switching to a lower maximum requires a zoom
clamp. Hiding retains the UUID. Importing another package never selects it.

UUID, visibility and opacity persist in preferences across rotation and restart.
A missing/unopenable active package is detached and its selection cleared, with
localized feedback. Removing the selection never chooses a replacement. A
separately installed package remains selectable.

The controller opens through LocalPackageRepository.open on IO, with a generation
guard and cancellation cleanup before attachment. Each attached ZIP has one
local-only module and memory cache, no MapCache, downloader or base identity.
Changing opacity redraws that same provider without reopening. Switching, hiding,
pausing and destroying the view close the reader and release the provider. Resume
opens a fresh reader. Pending dialogs are cancelled on pause.

Raster order is base → local → hiking/cycling → GPX/routes/markers/location.
PNG alpha is multiplied by the slider value. Absent tiles, absent zoom levels and
outside coverage draw transparently. Base-source/provider resets and the normal
cache revision replace only their own providers; the local overlay remains attached.
The package attribution joins the existing map attribution while attached, including
at 0% opacity, and disappears on hide/removal.

The effective maximum is max(configured maximum, active package maximum), capped
at renderer zoom 29. Hide/removal/open failure restores and clamps to the configured
maximum. Pausing releases resources without clamping the viewport being saved for
rotation. No maximum preference is rewritten.

Clear map cache now explains that it removes background-cache tiles, including
legacy **Import offline tiles** data, and keeps managed local maps. To move an old
historical background import to the independent layer: import its original ZIP
as a local map, then clear the normal cache if desired. This also removes ordinary
cached background tiles. The legacy importer remains available and unchanged.

The hardcoded enabled-marker/debug staging activation is retired. The old
DebugLocalLayer.kt implementation file now contains LocalMapLayer with no debug
activation path. The historic proof instructions describe Step 1 only; production
display uses the managed catalog. Native WMS, GeoTIFF and multiple simultaneous
local rasters remain outside this change.

## Step 3 validation record

Initial run passed both flavor unit suites and debug builds:
`LOCAL_XYZ_SAMPLE=/home/helfrich/Downloads/Caperrino_15_18.zip ./gradlew testFossDebugUnitTest assembleFossDebug testPlayDebugUnitTest assemblePlayDebug --console=plain --no-daemon`
(47 seconds). This includes the optional supplied ZIP repository test, sparse
viewport regressions and all 21 local-map translation keys across values, de, es,
fr, it and nl. The changed cache-clear text is translated in all six locales.

LocalMapLayerDeviceTest adds isolated-store/prefs coverage for rapid overlapping
same-name selection, opacity without reopen, hide/zoom clamp, removal/unavailable
selection, attribution, route-overlay attachment, reader closure and teardown.
Final verification through Android Studio's Gradle tool passed:
`testFossDebugUnitTest assembleFossDebug testPlayDebugUnitTest assemblePlayDebug assembleFossDebugAndroidTest --console=plain`.
Both flavor suites reported **65 passed, 0 skipped, 0 failed**. The instrumentation
APK compiled. The IDE Gradle tool rejects --no-daemon, so that option was used
only in the initial wrapper run. `git diff --check` passed.

Device validation used a fresh disposable **OpenTopoStep3 API 34** AVD
(`emulator-5580`, stored under /tmp/opentopo-step3-avds), using the installed
system image without copying the existing AVD's user data. The connected Pixel
3a was not changed. Initial IDE terminal calls timed out at the tool transport
(300 seconds); explicit-directory/background retries eventually worked.

Manual UI observations:
- Settings → Local maps → Import local map imported Caperrino_15_18.zip via
  DocumentsUI: 1,692 tiles, zooms 15,16,17,18, UUID
  288f9079-3dab-4c14-b4ae-75ababe821bf.
- Show on map returned directly to Caperrino and displayed the historical raster
  at zoom 15, without debug staging.
- The map layer dialog showed that UUID selected, Visible checked and Opacity 65%.
- Imported crocetta_caperrino.gpx through the GPX picker. Its initial fit uses zoom
  14, where the raster correctly does not draw. GPX-above-historical compositing
  at a shared zoom was not established by that screenshot.
- Fresh preferences displayed maximum 15. Explicitly changed it to **17** for
  the requested 18-versus-17 maximum test; do not describe this as a default-17 run.

Installed app and test APKs with `adb -s emulator-5580 install -r`, then invoked:
`adb -s emulator-5580 shell am instrument -w -e class org.nitri.opentopo.overlay.LocalMapLayerDeviceTest -e localPackageId 288f9079-3dab-4c14-b4ae-75ababe821bf org.nitri.opentopo.test/androidx.test.runner.AndroidJUnitRunner`.
**Two tests passed** (6.198 seconds). This establishes rapid same-name switching,
reader cleanup on hide/switch/removal/teardown, no ZIP reopen for opacity changes,
provider/base-source reset retention, package attribution, maximum extension to
18 and restoration/clamping to 17, actual sample tile requests at 18, pixel
differences between 0/65/100%, and selection/visibility/opacity after activity
recreation. Recreation is not a physical rotation or a process-death test.

The optional sample test writes map bitmaps to the disposable app's external
files directory. The additional `clearMapCache=true` runner argument is explicitly
emulator-only: it smoke-tests the legacy importer with a generated tile and
confirms Settings → Clear map cache removes that tile before testing managed
rendering again. Offline run: Android reported airplane mode **enabled** and **Wifi is disabled**
after `cmd connectivity airplane-mode enable` and `svc wifi disable`, both
targeting emulator-5580. Reinstalled the final test APK, then ran the same
instrumentation command with `-e clearMapCache true`. **Two tests passed**
(9.062 seconds). The generated legacy tile was verified in the normal cache,
removed through the Settings confirmation, and verified absent afterward.
The managed package still contained 1,692 tiles and rendered at zoom 18; pixel
comparisons at 0/65/100% passed again. Pulled and visually inspected the three
map bitmaps in /tmp/opentopo-step3-pixels. At 0% only the empty background grid
shows; 65% and 100% show the historical raster at different opacity.

Still offline, force-stopped and relaunched the app: the managed raster rendered
again (zoom 17 after normal startup clamping). Forced landscape with
`wm user-rotation lock 1`: the raster remained visible. The layer dialog then
showed the same full UUID selected, Visible checked, and Opacity 100%, confirming
state after restart and landscape configuration change. Screenshots are under
/tmp/opentopo-step3-*.png, outside Git. The disposable emulator was stopped;
its AVD remains under /tmp for inspection. No user-device settings or data changed.

Remaining visual/manual checks: the supplied GPX above the historical raster at
a common zoom; panning across a coverage edge with cached modern background;
manual base plus both hiking/cycling switches; and the legacy document-picker UI.
Provider switching and legacy cache writes were instrumented as described above,
but those are distinct from these manual checks. No Play instrumentation or
release builds were run. No Step 1 or Step 2 device results above should be read
as Step 3 results.

Step 3 changes: LocalMapsActivity.kt, MapFragment.kt, overlay/DebugLocalLayer.kt
(now the managed controller), OverlayHelper.kt, LocalXyzArchive.kt,
util/LocalMapPreferences.kt, util/LocalMapViewport.kt, menu_tile_sources.xml,
all six strings.xml files, LocalMapTranslationsTest.kt, LocalMapViewportTest.kt,
androidTest/overlay/LocalMapLayerDeviceTest.kt and both local-map documents.
Pre-existing untracked artifacts remain untouched. No sample files were added,
and no changes were staged or committed.

## Pixel 3a follow-up acceptance

The subsequent physical-device run is recorded in
[Step 3 acceptance — Pixel 3a](local-layer-pixel-step3-acceptance.md).
All five acceptance groups passed using the FOSS debug app installed in place.
That run includes the normal import/Show on map/offline Settings cache-clear
flow, GPX over raster, opacity controls, coverage-edge transparency, base and
both route-overlay switches, rotation/restart, active-package removal, and the
legacy document picker. Direct instrumentation passed two tests on the Pixel;
fresh Play and FOSS debug builds and both 65-test unit suites also passed.
These physical-device results complete the manual checks left pending in the
historical emulator record above. No uninstall or all-app-data clear was used.

## Changed files (Step 2 historical inventory)

Modified:

- app/src/main/AndroidManifest.xml
- app/src/main/java/org/nitri/opentopo/SettingsActivity.kt
- app/src/main/java/org/nitri/opentopo/util/LocalXyzArchive.kt
- app/src/main/res/xml/preferences.xml
- app/src/main/res/values/strings.xml
- app/src/main/res/values-de/strings.xml
- app/src/main/res/values-es/strings.xml
- app/src/main/res/values-fr/strings.xml
- app/src/main/res/values-it/strings.xml
- app/src/main/res/values-nl/strings.xml
- app/src/test/java/org/nitri/opentopo/util/LocalXyzArchiveTest.kt
- docs/local-layer-rendering-proof.md

New (untracked until explicitly committed):

- app/src/main/java/org/nitri/opentopo/LocalMapsActivity.kt
- app/src/main/java/org/nitri/opentopo/util/LocalPackageRepository.kt
- app/src/main/java/org/nitri/opentopo/util/LocalPackageValidation.kt
- app/src/main/java/org/nitri/opentopo/util/XyzZipIndex.kt
- app/src/main/java/org/nitri/opentopo/util/ZipEnvelope.kt
- app/src/test/java/org/nitri/opentopo/util/LocalPackageRepositoryTest.kt
- app/src/test/java/org/nitri/opentopo/util/LocalMapTranslationsTest.kt
- app/src/androidTest/java/org/nitri/opentopo/util/LocalPackageDeviceTest.kt
- docs/local-map-packages.md

Pre-existing untracked files/directories were left untouched: .artifacts/, .kotlin/,
the Screenshot_*.png and device-* images/video, app/foss/, app/play/, app/release/,
app/src/play/google-services.json.b64, build_output.txt, cache_settings.png,
diff_nearby.txt, diff_nearby_complete.txt, docs/settings_activity_compose_migration_clean.md,
docs/startup-viewport-investigation.md, keystore, location_detail.png and
location_detail.xcf. Build/test outputs remain ignored. No files were staged or committed.
