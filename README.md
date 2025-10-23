# OpenTopoMapViewer

OpenTopoMap viewer for Android

[![GitHub Release](https://img.shields.io/github/release/Pygmalion69/OpenTopoMapViewer.svg?logo=github)](https://github.com/Pygmalion69/OpenTopoMapViewer/releases)

- Follow location
- Import GPX
- Lonvia hiking routes
- Lonvia cycling routes

Check out this post to learn more about preloading the cache for offline use: [Preloading Offline Maps in OpenTopoMap Viewer](https://pygmalion.nitri.org/preloading-offline-maps-in-opentopomap-viewer-1714.html).

Basic routing with the [ORS Android Client](https://github.com/Pygmalion69/ors-android-client) is an experimental feature: [How to Enable Basic Routing with OpenRouteService (ORS) in OpenTopoMap Viewer](https://pygmalion.nitri.org/how-to-enable-basic-routing-with-openrouteservice-ors-in-opentopomap-viewer-1818.html).

![Wageningen](screen_wag.png "Wageningen") ![WUR](screen_wur.png "WUR")

![GPX](screen_dopplersteig.png "GPX") ![POI](screen_dopplersteig_poi.png "POI")

![GPX](screen_dopplersteig_gpx_detail.png "GPX")

&nbsp;

<a href="https://play.google.com/store/apps/details?id=org.nitri.opentopo" target="_blank" rel="noopener"><img src="https://pygmalion.nitri.org/wp-content/uploads/2024/06/GetItOnGooglePlay_Badge_Web_color_English.png" alt="" width="270" height="80" class="alignnone size-full wp-image-1648" /></a>

(Has ads.)

&nbsp;

<a href="https://f-droid.org/packages/org.nitri.opentopo">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80"/></a>

(FOSS, no ads.)

## UI Verification

To confirm that multi-track GPX files report the combined distance correctly:

1. Copy [`test-data/multi_track_sample.gpx`](test-data/multi_track_sample.gpx) to your device.
2. Open OpenTopoMapViewer and import the sample GPX.
3. Open the GPX details screen.
4. Verify that the total distance displays approximately **2.22 km**, which reflects the sum of both tracks in the sample file.
