# Implementation Plan: Add KML Viewer to OpenTopoMapViewer

## Summary of the requirement

The GitHub issue `wishlist: kml viewer` (issue #4) proposes adding support for displaying [KML](https://en.wikipedia.org/wiki/Keyhole_Markup_Language) files in the **OpenTopoMapViewer**.  The issue suggests leveraging the `osmbonuspack` library, an extension to [osmdroid](https://github.com/osmdroid/osmdroid), because it already supports reading and displaying KML overlays.  At the moment the app can load GPX tracks and cycle/hiking tile overlays, but there is no way to load a KML file.  This plan outlines how to add a KML viewer that allows users to select a KML file from the device and display its placemarks, lines and polygons on the map.

## 1. Dependencies

* **Add OSMBonusPack** – Add the `osmbonuspack` library to the app module’s Gradle dependencies.  The latest release (v6.9.0 at time of writing) upgrades to osmdroid 6.1.13 and contains KML support.  Add the following line to `app/build.gradle` in the `dependencies` block:

  ```gradle
  implementation "org.osmdroid:osmbonuspack:6.9.0"
  ```

  Alternatively, if using JitPack, use `com.github.MKergall:osmbonuspack:6.9.0`.

* **Proguard configuration** – If the app uses code shrinking, add rules to keep the KML classes: `-keep class org.osmdroid.bonuspack.kml.** { *; }`.

## 2. User interface and file picker

* **Menu item** – Add a new menu entry (e.g., “Open KML”) alongside the existing GPX actions.  In `res/menu/menu.xml`, define an `<item>` with a unique ID such as `action_open_kml`.

* **Activity result launcher** – In `BaseMainActivity` (or the activity that currently loads GPX files) register an `ActivityResultLauncher<Intent>` for selecting files.  Reuse the pattern used for GPX selection:

  ```kotlin
  private val kmlPicker = registerForActivityResult(
      ActivityResultContracts.StartActivityForResult()
  ) { result ->
      val uri = result.data?.data ?: return@registerForActivityResult
      handleKmlUri(uri)
  }
  ```

* **Launch the picker** – When the user taps the menu item, create an intent with `ACTION_GET_CONTENT`, add category `CATEGORY_OPENABLE`, and set the MIME types to KML/KMZ (e.g. `"application/vnd.google-earth.kml+xml"` and `"application/vnd.google-earth.kmz"`).  Launch the intent via `kmlPicker.launch(intent)`.

## 3. Parsing the KML

* **Read the file** – Resolve the selected URI to an input stream using `contentResolver.openInputStream(uri)`.  Copy the stream to a temporary file or input to `osmbonuspack` directly.

* **Create and parse a `KmlDocument`** – Use `KmlDocument` from `osmbonuspack`:

  ```kotlin
  val kmlDoc = KmlDocument()
  val inputStream = contentResolver.openInputStream(uri)
  kmlDoc.parseKMLStream(inputStream, null)  // parses the KML into the document
  inputStream?.close()
  ```

  The OSMBonusPack tutorial recommends creating a `KmlDocument`, parsing the content and then building an overlay.  The relevant lines show that after creating a `KmlDocument` you call `parseUrl()` or parse from a stream, and the document exposes `mKmlRoot`, a folder containing the KML hierarchy【110433767800318†L227-L240】.

## 4. Building and displaying the overlay

* **Create overlay** – Build the overlay from the parsed document:

  ```kotlin
  val kmlOverlay = kmlDoc.mKmlRoot.buildOverlay(mapView, /*defaultStyle=*/null, /*styler=*/null, kmlDoc) as FolderOverlay
  ```

  According to the tutorial, this `FolderOverlay` will contain all markers, polylines and polygons defined in the KML【110433767800318†L243-L255】.  You can specify a custom `Style` if you want to override marker icons, line colour or fill colour.

* **Add to map** – Remove any previous KML overlay and add the new one:

  ```kotlin
  // In MapFragment or OverlayHelper
  previousKmlOverlay?.let { mapView.overlays.remove(it) }
  mapView.overlays.add(kmlOverlay)
  previousKmlOverlay = kmlOverlay
  mapView.invalidate()
  ```

* **Zoom to content** – Optionally centre/zoom the map on the KML bounding box.  The tutorial suggests retrieving the bounding box via `kmlDoc.mKmlRoot.getBoundingBox()` and centring on its centre【110433767800318†L269-L277】.

## 5. Integrating with `OverlayHelper` and `MapFragment`

* **Extend `OverlayHelper`** – Add a nullable `FolderOverlay` property (`kmlOverlay: FolderOverlay?`).  Provide a `setKml(kmlDoc: KmlDocument?)` method that clears the current KML overlay and, if a document is provided, creates a new overlay and adds it to the map.  Use similar patterns to `setGpx()` for GPX tracks.

* **Expose in `MapFragment`** – Add a method (e.g. `displayKml(kmlDoc: KmlDocument)`) that delegates to `overlayHelper.setKml(kmlDoc)` and adjusts the map view.  Ensure that lifecycle events (e.g. configuration changes) restore the loaded KML overlay.

## 6. Hide/show overlay

Optionally provide a toggle in the UI to hide or show the loaded KML without clearing it.  This can be a check‑box or menu item that calls `overlay.setEnabled(flag)` and invalidates the map.

## 7. Error handling

* Catch exceptions thrown by `parseKMLStream()` (e.g. malformed KML) and notify the user.
* If the selected file has a `.kmz` extension, unzip it to a temporary directory and pass the KML file inside to `parseKMLFile()`.

## 8. Testing and sample KML file

* **Test file** – A simple KML file named **`sample_kleve_duisburg_wesel.kml`** is provided with this plan.  It defines placemarks for Kleve, Wesel and Duisburg in Germany and a line connecting them.  Use it to verify that markers and polylines appear correctly.  The file is in the `/home/oai/share` directory of this challenge and can be loaded via the new KML picker.

* **Manual test procedure**:
  1. Build the app with the KML viewer implementation.
  2. Install it on a device or emulator.
  3. Copy `sample_kleve_duisburg_wesel.kml` to the device (e.g. via `adb push`).
  4. Launch **OpenTopoMapViewer** and use *Open KML* to select the file.
  5. Observe that markers appear at Kleve, Wesel and Duisburg and the route line connects them.  The map should zoom to the region automatically.

## 9. Documentation

* Update the project README or user guide to describe the new feature: how to load KML files, supported file formats (`.kml`, `.kmz`), any known limitations (large files or unsupported KML features), and sample usage.

* Consider adding a dedicated `docs/kml-viewer-usage.md` with screenshots illustrating the selection dialog and the resulting map overlay.

## 10. Future enhancements

* **Styling** – Implement custom default styles by creating a `Style` object (default marker icon, line colour and width, fill colour) and passing it to `buildOverlay()` as shown in the tutorial【110433767800318†L313-L320】.  For more control, implement a `KmlFeature.Styler` to adjust styling dynamically【110433767800318†L323-L356】.

* **Saving/exporting** – Allow users to save edited overlays back to a KML file using `KmlDocument.saveAsKML()` or export as GeoJSON (see `KmlDocument.saveAsGeoJSON()`)【110433767800318†L343-L349】.

* **Filtering layers** – For complex KML documents with many folders, provide a layer list UI to enable/disable individual folders or placemarks.

This plan uses the standard OSMBonusPack API for reading and displaying KML content.  By following these steps, OpenTopoMapViewer will gain the ability to load KML and KMZ files and display their contents as overlays on top of the map.
