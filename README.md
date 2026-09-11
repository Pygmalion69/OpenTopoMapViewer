# OpenTopoMapViewer

An Android map viewer for topographic maps, tracks, places and lightweight route planning.

[![GitHub Release](https://img.shields.io/github/release/Pygmalion69/OpenTopoMapViewer.svg?logo=github)](https://github.com/Pygmalion69/OpenTopoMapViewer/releases)

## Features

### Maps and location

- Multiple raster base maps: OpenTopoMap, OpenTopoMap-R, Top-O-Map, OpenStreetMap, OpenHikingMap and Freemap.sk
- Waymarked Trails overlays for hiking and cycling routes
- Follow the device location and quickly return to it
- Location details with coordinates and GPS/NMEA elevation; optional ORS elevation lookup
- Optional map rotation using the compass or GPS bearing
- Fullscreen-on-tap, keep-screen-on and maximum zoom settings

### GPX and KML

- Open GPX files and display tracks, routes and waypoints
- Zoom to imported tracks and inspect distance, elevation profile, track description and waypoint details
- Choose the GPX track color
- Optional KML/KMZ import, display and zoom-to-content support
- Remove imported GPX or KML content directly from the map

### Markers and nearby places

- Add persistent markers by long-pressing the map, then edit, drag or delete them
- Set marker names, descriptions and individual colors
- Optional marker labels with a configurable minimum zoom level
- Import and export markers as GPX waypoints, including bulk selection and deletion
- Browse nearby Wikipedia places and place a selected result on the map

### OpenRouteService integration

When an [OpenRouteService](https://openrouteservice.org/) API key is configured:

- Search for places with debounced autocomplete and add a selected result as a normal editable marker
- Calculate routes using markers as waypoints
- Recalculate routes after waypoints are added, removed or dragged
- Choose profiles for car, truck, several bicycle types, walking, hiking and wheelchair routing
- Display calculated routes as GPX, including route details and elevation data

### Offline use and customization

- Import an OpenTopoMap tile ZIP into the offline cache
- Configure the tile-cache location and maximum cache size
- Light and dark theme support
- English, German, Spanish, French, Italian and Dutch interface translations

See [Offline tile ZIP import for OpenTopoMap Viewer](https://pygmalion.nitri.org/preloading-offline-maps-in-opentopomap-viewer-1714.html) for the required archive layout and import workflow.

Routing, elevation and place search use the [ORS Android Client](https://github.com/Pygmalion69/ors-android-client). See [How to Enable Basic Routing with OpenRouteService (ORS) in OpenTopoMap Viewer](https://pygmalion.nitri.org/how-to-enable-basic-routing-with-openrouteservice-ors-in-opentopomap-viewer-1818.html) for setup instructions.

![Wageningen](screen_wag.png "Wageningen") ![WUR](screen_wur.png "WUR")

![GPX](screen_dopplersteig.png "GPX") ![POI](screen_dopplersteig_poi.png "POI")

![GPX](screen_dopplersteig_gpx_detail.png "GPX")

## Forks and third-party distributions

OpenTopoMap Viewer is open-source software licensed under the Apache License 2.0. The licence permits modification and redistribution, including under a different name.

Please note, however, that third-party forks and rebranded distributions are independent projects. They may contain older versions of OpenTopoMap Viewer, additional modifications, different advertising or analytics components, or different release and signing practices.

This repository is the upstream development project for OpenTopoMap Viewer. Issues, releases and security fixes published here apply to the official OpenTopoMap Viewer builds only.

If you maintain a fork and fix a bug or make a generally useful improvement, contributions back to the upstream project are very welcome.

&nbsp;

<a href="https://play.google.com/store/apps/details?id=org.nitri.opentopo" target="_blank" rel="noopener"><img src="https://pygmalion.nitri.org/wp-content/uploads/2024/06/GetItOnGooglePlay_Badge_Web_color_English.png" alt="" width="270" height="80" class="alignnone size-full wp-image-1648" /></a>

(Has ads.)

&nbsp;

<a href="https://f-droid.org/packages/org.nitri.opentopo">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80"/></a>

(FOSS, no ads.)
