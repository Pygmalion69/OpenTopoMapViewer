# Clear map cache verification

The cache action calls osmdroid 6.1.20 `SqlTileWriter.purgeCache()` (all sources).
It does not unlink database files or touch preferences, application databases, or
import documents. It checks the shared SQLite connection against the effective
`Configuration.osmdroidTileCache` path; a mismatch reports failure without deletion.

ZIP imports hold the same lock as downloader writes and purging. An import already
running finishes before the purge. Queued imports crossing a purge are rejected.
Old downloader writers are invalidated even if the purge fails; replacement
providers can download again. The application-scoped operation survives leaving
Settings or rotating the device. Map fragments observe its completion and replace
only base/route tile providers, retaining the map view and non-tile overlays.

Focused instrumentation: `org.nitri.opentopo.util.MapCacheTest` covers downloaded
and ZIP-imported rows, all-source deletion, original files and inactive cache
preservation, old-writer rejection, fresh-writer acceptance, cancellation, path
mismatch, inaccessible storage, an in-flight downloader write, and in-memory tile
reset while preserving center, zoom, offline mode and non-tile overlays. Tests use
a temporary cache directory.

Device checks still required unless recorded separately:

- Populate multiple map sources and a hiking/cycling overlay, import a ZIP, then
  cancel the confirmation and confirm offline tiles remain.
- Confirm clearing, including while a large ZIP import/download is active. Rotate
  or navigate back while it runs; confirm repeated activation is disabled.
- With networking disabled, previously visible tiles (including zoom approximations
  and route tiles) disappear. With networking enabled, fresh tiles load afterward.
- Check center, zoom, GPX tracks, markers, Nearby results and settings remain intact.
- Repeat with configured internal, external and runtime fallback storage. Verify
  inactive directories and original ZIP/GPX documents remain unchanged.
- Simulate unavailable/read-only storage and check failure feedback, then recover
  storage and verify normal map downloads resume.

The existing cache-location settings flow is separate from this action. If its
configured path and osmdroid's live static connection disagree, clearing fails
safely; it does not search for or delete a different cache directory.

## Checks run (2026-10-17)

- Play and FOSS debug APK builds passed.
- JVM unit suites: 38 passed in each flavor, no failures or skipped tests.
- Pixel 3a / Android 12: all six focused instrumentation tests passed in each
  flavor. The Play cancellation test initially selected the activity window and
  timed out; explicitly matching the confirmation dialog fixed the test. Both
  suites passed again with that correction.
- XML parsing and explicit coverage checks passed for all seven new strings in
  default English, German (`de`), Spanish (`es`), French (`fr`), Italian (`it`) and
  Dutch (`nl`). Existing Android Cancel resources are reused.
- `git diff --check` passed.

Builds used `--no-configuration-cache`; an initial offline attempt could not
resolve an uncached dependency. Existing Gradle/SDK deprecation and NDK path
warnings remain unrelated to this change.
