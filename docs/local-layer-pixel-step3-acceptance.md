# Step 3 acceptance — Pixel 3a

Device: Pixel 3a, serial 016AY1RYD7, Android 12. Branch: feature/local-layer.
The developer explicitly authorized clearing this development device's map cache.
No uninstall, connected-test cleanup, or all-app-data clearing was used.

## Installation and fixtures

Installed app/build/outputs/apk/foss/debug/app-foss-debug.apk with
`adb -s 016AY1RYD7 install -r`. Result: Success.
The existing installed Caperrino package survived the update:
1a4b44b9-6942-4426-a9c2-159176bafc7e.

Copied the supplied ZIP to Download/OpenTopo-Step3-Caperrino.zip. Imported it
through Settings → Local maps → Import local map and DocumentsUI.
The new row reports 1,692 tiles, zooms 15,16,17,18, UUID
bb3d6db3-25ff-4172-90d3-b0c18a044c2f.
The workstation samples were not modified or added to Git.

## Observed results

- **Pass — managed import and Show on map:** the new row's Show on map returned
  directly to Caperrino and displayed historical raster at zoom 15, without
  debug staging. The existing package remained listed independently.
- **Pass — offline Settings cache clear:** after Show on map, enabled airplane
  mode and explicitly disabled Wi-Fi. Android reported “enabled” for
  `cmd connectivity airplane-mode` and “Wifi is disabled” for `cmd wifi status`.
  Opened Settings → Clear map cache, checked the wording distinguishing legacy
  cache tiles from retained local maps, and tapped CLEAR CACHE. Returning to
  the map still showed the historical raster; modern base detail disappeared.
- **Pass — GPX above raster:** imported crocetta_caperrino.gpx through the GPX
  picker. Its initial fit was below the package's minimum zoom. Used on-screen
  zoom buttons to reach local zooms and observed the red GPX above the historical
  raster, including zoom 18 (tile diagnostics /18/142761/98813–98815).
- **Pass — opacity UI:** the initial control showed 65%. Set the slider to 0%,
  50%, then 100%; the map behind the dialog updated immediately. At 0% the
  background grid and GPX remained; intermediate and full opacity showed
  visibly different raster strength.
- **Pass — hide without losing selection, original saved cap:** the Pixel's
  original saved maximum was 15. Reached zoom 18 with the layer shown. Turning
  Visible off retained the new UUID, removed raster, and clamped to zoom 15.
  Turning it on again restored raster.
- **Pass — offline process restart:** force-stopped the app and relaunched it,
  without reinstalling or copying the ZIP again. The historical raster rendered
  again at zoom 15. The GPX itself was absent after process restart; persistence
  of imported GPX is outside this local-layer acceptance scope.

- **Pass — rotation and saved state:** forced landscape while still offline; the
  historical raster remained visible. Restored portrait/automatic rotation.
  After a later fresh app launch, the layer dialog showed the same new UUID,
  Visible checked and Opacity 100%.
- **Pass — base and overlay independence:** selected OpenStreetMap, Lonvia
  Hiking, then Lonvia Cycling through the layers menu. The local raster stayed
  attached after every selection.
- **Pass — coverage edge:** panned east at zoom 15. Historical raster ended at
  x=17847; modern OpenStreetMap remained visible on the other side at rows
  y=12352–12353. Missing local tiles exposed the base rather than blanking it.
- **Pass — selection without navigation:** selected the original package, then
  the newly imported package in the map dialog. The tile boundaries, map center
  and zoom stayed in place.
- **Pass — active-package removal:** removed only the new UUID through Settings
  and its name/UUID confirmation. The original UUID remained in the catalog.
  Returning to the map showed OpenStreetMap without stale historical tiles.
  The local-map dialog then showed its remaining radio button unchecked and
  Visible off: no automatic replacement selection. Selecting the original
  package again rendered it at the same viewport.
- **Pass — legacy background ZIP picker:** opened Settings → Import offline tiles,
  selected Download/OpenTopo-Step3-Caperrino.zip through DocumentsUI, and returned
  to Settings. Device logcat recorded:
  `D/SettingsFragment: Imported 1692, skipped 0`.
  The legacy importer remained separate from managed selection. Cleared the
  normal map cache again through Settings after this smoke test so legacy
  historical background tiles would not confuse later hide/show use.

## Acceptance checklist

| Brief check | Pixel result |
| --- | --- |
| 1. Managed import, Show on map, GPX above raster, zoom 18 and cap restoration | PASS: manual UI plus cap-17 instrumentation |
| 2. Opacity 0/intermediate/100, coverage edge, base and hiking/cycling switches | PASS: manual UI; instrumented pixel comparisons also passed |
| 3. Airplane mode + Wi-Fi off, Settings cache clear, rotation and restart | PASS: manual physical-device checks |
| 4. Rapid overlapping-package switches, active removal, remaining package usable | PASS: rapid fixture instrumentation plus actual catalog/removal UI |
| 5. Legacy ZIP import, Play/FOSS builds and unit tests, translation coverage | PASS: picker/logcat, both builds, 65 tests per flavor |

The app installed on the Pixel was FOSS debug. Play debug was built and unit-tested,
but was not installed or instrumented on this device. No release build is claimed.
UI hierarchy dumping intermittently failed with exit 137; Android CLI layout
inspection was used to recover. These tool failures were not counted as successful
checks; subsequent UI observations and test output establish the results above.

## Builds, translations and direct instrumentation

Reran with Android Studio's Gradle tool:
`testFossDebugUnitTest assembleFossDebug testPlayDebugUnitTest assemblePlayDebug assembleFossDebugAndroidTest --rerun-tasks --console=plain`.

Build finished successfully. Both flavor unit suites reported **65 passed,
0 skipped, 0 failed**. The translation regression checks all 21 local-map keys
plus the changed cache-clear message across all six maintained locales.

Installed the test APK separately with:
`adb -s 016AY1RYD7 install -r app/build/outputs/apk/androidTest/foss/debug/app-foss-debug-androidTest.apk`.

With the saved maximum explicitly changed from 15 to **17**, ran:
`adb -s 016AY1RYD7 shell am instrument -w -e class org.nitri.opentopo.overlay.LocalMapLayerDeviceTest -e localPackageId bb3d6db3-25ff-4172-90d3-b0c18a044c2f org.nitri.opentopo.test/androidx.test.runner.AndroidJUnitRunner`.

Result: **OK (2 tests), 5.98 seconds**. Coverage includes repeated rapid switching
between isolated same-name overlapping fixtures, reader closure, opacity changes
without ZIP reopen, provider/base-source reset retention, removal/unavailable
handling, attribution, the actual 1,692-tile sample at zoom 18, pixel differences
at 0/65/100%, activity recreation, and hide/clamp back to 17. No
`clearMapCache` instrumentation flag was used; cache clearing on this Pixel
was performed manually through Settings first. The test cleaned only the
temporary fixture store/preferences it created. It did not uninstall or clear
the app's data.

The cap-17 run is an explicit preference test, not a claim that this Pixel's
original/default maximum was 17. Original maximum 15 was restored and verified in Settings.

## Commands and data boundaries

All adb actions target serial 016AY1RYD7. Original connectivity was airplane
mode off and Wi-Fi on; original rotation preferences were
accelerometer_rotation=1 and user_rotation=0. Restored the original connectivity
and automatic portrait rotation. Clearing the normal map cache intentionally
removed ordinary cached/offline background tiles. Managed packages and other
app data were kept.

## Final state and repository changes

- Original managed package 1a4b44b9-6942-4426-a9c2-159176bafc7e remains selected
  and visible at **65%** opacity, verified in the map dialog.
- Newly imported test UUID bb3d6db3-25ff-4172-90d3-b0c18a044c2f was removed
  through its confirmation; its source ZIP remains in Downloads.
- Original saved maximum **15**, OpenTopoMap base, and **None** hiking/cycling
  overlay restored. Final map inspection showed modern background through the
  partial-opacity local raster; the layer survived the second Settings cache clear.
- Airplane mode off, Wi-Fi on/connected, automatic rotation restored.
- App and test APKs remain installed. No uninstall or app-data wipe occurred.
- This follow-up changes only this validation record and its link/summary in
  local-map-packages.md. Existing implementation changes and unrelated untracked
  artifacts were left untouched. No samples were added to Git; nothing was staged
  or committed.
