# Local XYZ rendering proof — #104, Step 1

Branch: feature/local-layer. Prototype only; no production import or layer UI.

## Storage and provider

The package remains an indexed ZIP in internal persistent app storage:
files/local-layers/caperrino-proof/tiles.zip. A sibling file named enabled
opts into loading it on map creation in debug builds only. The source identity
is local-caperrino-proof, regardless of the ZIP's OpenTopoMap directory prefix.

Java ZipFile provides indexed exact entry lookup without extracting 1,692 files.
A managed SQLite archive would add conversion/schema/verification work; a tile
directory would add extraction and many filesystem entries. Retaining the ZIP
is the smallest rendering experiment. Each later layer can have its own directory,
identity and read-only provider. Index enumeration and reads run off the UI thread.

The provider chain contains exactly one custom read-only archive module and its
own osmdroid memory cache. It has no SqlTileWriter, database reader, archive
discovery, approximation module, downloader, or normal-cache eviction.
Missing tiles and loading placeholders are transparent. PNG alpha is multiplied
by 0.65. Outside the package zoom range the overlay draws nothing.

MapCache.resetProvider replaces only the base raster. Its revision collector
recreates the hiking/cycling provider independently; neither operation owns the
local provider. Local teardown closes the ZIP and stops its queue when the map
view is destroyed; the next map view opens the same file.

The installed osmdroid version is 6.1.20. The existing TileImporter continues
writing OpenTopoMap tiles into the normal cache without changes. The default
map cap is 17; once the local package opens, the effective cap becomes the
greater of the preference and the package maximum (18 for Caperrino). No saved
maximum preference is changed and no minimum is imposed on the base map.
Disable and recreate the map to restore the preferred cap.

GPX tracks previously inserted at index zero. They now insert after raster
overlays, so GPX loaded before or after the asynchronous ZIP open remains above
the historical raster. Markers, location and route overlays remain above it.
Changing hiking/cycling removes only that helper's own raster.

## Pixel reproduction

Use the SDK platform-tools adb, with USB debugging authorized. Both debug
flavors use org.nitri.opentopo. Installing over an app signed with a different
key requires a separately planned migration; do not uninstall and erase data
just to run this proof.

Build and install one debug APK:

```sh
./gradlew assembleFossDebug --console=plain --no-daemon
ADB="$HOME/Android/Sdk/platform-tools/adb"
"$ADB" install -r app/build/outputs/apk/foss/debug/app-foss-debug.apk
"$ADB" shell am force-stop org.nitri.opentopo
"$ADB" shell run-as org.nitri.opentopo mkdir -p files/local-layers/caperrino-proof
"$ADB" push /home/helfrich/Downloads/Caperrino_15_18.zip /data/local/tmp/caperrino-proof.zip
"$ADB" shell run-as org.nitri.opentopo cp /data/local/tmp/caperrino-proof.zip files/local-layers/caperrino-proof/tiles.zip
"$ADB" shell run-as org.nitri.opentopo touch files/local-layers/caperrino-proof/enabled
"$ADB" shell am start -n org.nitri.opentopo/.MainActivity
"$ADB" logcat -d -s DebugLocalLayer:I
```

No conversion or extraction is needed. Stage only while force-stopped.
Subsequent rotations and launches need no copy. To disable, retain the ZIP:

```sh
"$ADB" shell am force-stop org.nitri.opentopo
"$ADB" shell run-as org.nitri.opentopo rm -f files/local-layers/caperrino-proof/enabled
"$ADB" shell am start -n org.nitri.opentopo/.MainActivity
```

Import crocetta_caperrino.gpx using the existing GPX picker (copy it to Downloads
on the Pixel if necessary) and use the existing zoom-to-track action.
Do not use the background ZIP importer for this proof. Neither sample belongs
in the repository.

## Verification checklist

Record actual results separately from the expected behavior below.

- Log shows 1,692 PNG tiles and zooms 15–18.
- Historical raster appears at 65% opacity; GPX remains above it.
- Use ordinary zoom controls to reach 18 with the default maximum preference.
- Pan beyond coverage; the base is visible through missing local tiles.
- Zoom below 15 and above 18 (if the preferred maximum allows); base alone draws.
- Enable airplane mode; local raster remains visible.
- Clear the normal map cache offline, return to map; local raster remains even
  where the base becomes blank.
- Switch base source and hiking/cycling overlays; local layer remains.
- Rotate and force-stop/reopen; local package is reused without staging.
- Disable using the commands above; preferred zoom cap is restored.

Unit tests cover actual zoom indexing, exact tile reads, missing/out-of-range
tiles, invalid packages, close and reopen. These do not prove visual compositing,
device lifecycle or offline cache-reset behavior.

## Recorded validation — 2026-11-17

After the workstation reboot, reran:

```sh
./gradlew testFossDebugUnitTest assembleFossDebug testPlayDebugUnitTest assemblePlayDebug --console=plain --no-daemon
git diff --check
```

Both flavor unit test suites and debug APK builds passed (Gradle: BUILD SUCCESSFUL,
58 seconds). Existing deprecation warnings remain. No release builds or
instrumentation suite runs are claimed.

Installed the FOSS debug APK on the connected Pixel 3a (016AY1RYD7) without
uninstalling. Staged the sample once using the commands above. Logcat confirmed
1,692 tiles, zooms 15–18. Imported the sample GPX through the existing picker.
Observed base-only rendering at zooms 13–14, historical raster at 15–18, and
GPX drawn above the historical raster. Reached 18 with ordinary double-tap map
gestures; on-screen osmdroid tile diagnostics showed /18/... tiles.

The package was reopened after force-stop without another copy. The existing
startup behavior returned the viewport to the current location outside Caperrino;
that observation alone does not establish offline raster rendering. Airplane mode
was briefly enabled for that reopen attempt and then disabled again.

Follow-up device run on the same Pixel 3a:

- Reopened crocetta_caperrino.gpx through the picker and zoomed from its initial
  track view to 15, where the historical raster and GPX were visible.
- Enabled airplane mode and explicitly disabled Wi-Fi. Android reported
  "enabled" for airplane mode and "Wifi is disabled"; the status bar showed
  airplane mode and no Wi-Fi icon. Historical raster and GPX remained visible.
- Used Settings → Clear map cache → CLEAR CACHE, then returned to the map while
  still offline. Historical raster and GPX remained; modern base detail vanished.
  This exercised the normal cache revision/provider reset, not manual DB deletion.
- Panned east across the package edge at zoom 15. Historical tiles ended before
  x=17847 (observed rows y=12351–12352); the base's empty grid showed through.
- Disabled airplane mode, enabled Wi-Fi, and confirmed Wi-Fi reconnected.
  Switched the base to OpenStreetMap. Downloaded OSM tiles were visible beyond
  that same edge and through the partial-opacity historical raster inside it.
- Selected Lonvia Hiking, then Lonvia Cycling. The historical raster remained
  attached after each selection.
- Rotated to landscape. The staged historical raster rendered again without
  copying; the same boundary exposed OSM on the right and historical raster
  on the left, with a GPX segment visible near the bottom-left.

- Restored the original rotation settings (accelerometer rotation enabled,
  user rotation 0), force-stopped the app, removed only the enabled marker and
  relaunched. Historical raster disappeared; OSM remained visible in the same
  area. A double-tap zoom attempt stayed at zoom 15 (on-screen /15/... tile
  diagnostics and unchanged scale). Settings still displayed maximum zoom 15.
- Confirmed the staged ZIP remains on disk at 16,233,799 bytes after cache
  clearing, rotation and disabling. No second package copy was performed.

The requested follow-up device checks passed. The saved maximum zoom on this
Pixel was 15, not the application's default 17. Enabled-layer zoom-18 rendering
exceeded the saved cap without changing that preference; disabling restored 15.
A separate clean-preference run using default 17 was not performed. No release
build or automated instrumentation run is claimed.

Final device state: connectivity restored, original rotation settings restored,
OpenStreetMap base selected with Lonvia Cycling, debug local layer disabled,
staged ZIP retained. Clearing the normal cache intentionally removed the previous
background cache, including previously imported background tiles.

Some unbounded terminal tool calls timed out during the final checks. Retried
the device commands individually with an eight-second timeout and verified their
results from the UI; no device-check result is inferred from a timed-out call.

Changes for this prototype: MapFragment.kt, OverlayHelper.kt, DebugLocalLayer.kt,
LocalXyzArchive.kt, LocalXyzArchiveTest.kt and this document. Pre-existing untracked
screenshots, artifacts, build output, flavor directories and other files were left
alone. The commit includes only these six prototype files; no sample ZIP or GPX
is included.

## Step 2 implementation

Managed package import, storage, validation and removal are documented in
[local-map-packages.md](local-map-packages.md). The debug fixture above remains
separate from the production catalog; Step 2 does not add map layer controls.

## Step 2 findings (recorded at the Step 1 baseline)

Keep package identity distinct from tile-source prefixes. Production work needs
atomic staging/validation and replacement, explicit ownership of layer providers,
stable raster ordering, per-layer metadata and zoom policy. The debug marker
requires map recreation after toggling and is deliberately not package management.
ZIP bounds, tile dimensions, hostile/oversized entry limits, full package validation,
backup policy and migration are deferred; use only the prepared trusted sample.
No WMS, GeoTIFF, layer picker, opacity controls or new user-facing strings are added.
