# OpenTopoMap Viewer — Marker Management and GPX Import/Export Implementation Plan

Target issue: `Wishlist: marker export #39`

## Goal

Implement a small but complete marker management feature for OpenTopoMap Viewer using the existing XML/View-based UI. The feature should allow users to view, edit, select, delete, export, and import saved map markers.

The first implementation should deliberately avoid a full UI rewrite and should not introduce Jetpack Compose. The app currently uses legacy XML/View UI, so this feature should follow the same approach for consistency and lower implementation risk.

## User-facing behavior

### Entry point

Add a `Markers` entry to the existing app menu.

Recommended behavior:

- Show the menu item only when at least one marker exists.
- Alternatively, show it disabled when no markers exist.
- Opening it displays a marker list screen.

The marker list screen should also expose an `Import GPX` action, because import is useful even when no markers currently exist. If the menu item is hidden when there are no markers, make sure import is still reachable elsewhere, for example from settings or the main menu.

### Marker list screen

Create a new XML/View-based screen, for example:

- `MarkerListActivity`, or
- `MarkerListFragment` hosted by the existing main activity, depending on the current app architecture.

Use a `RecyclerView`.

Each marker row should show at least:

- Marker label, or a generated fallback name.
- Latitude and longitude, shortened for readability.
- Optional note/description if the marker model already supports it.

Fallback label examples:

- `Marker 1`
- `Marker 2026-04-26 14:08`
- `51.83860, 6.61550`

Keep the first version simple. Sorting, filtering, search, categories, icons, and folders are out of scope.

### Opening/editing a marker

Tapping a marker row in normal mode should open the same view/edit UI used when selecting a marker on the map.

This is important: do not create a second, inconsistent marker editor if the app already has one.

Expected behavior:

- User taps marker in list.
- Existing marker detail/edit dialog or screen opens.
- User can view/edit the marker exactly as on the map.
- After editing, the list refreshes.

If the existing map marker UI is tightly coupled to the map screen, extract the reusable marker-edit logic into a shared dialog/helper rather than duplicating behavior.

### Selection mode

The list should support selecting markers.

Recommended interaction:

- Normal tap: open marker view/edit UI.
- Long press: enter selection mode and select the long-pressed item.
- In selection mode, tapping rows toggles selection.
- Show checkboxes or selected-state indicators while selection mode is active.

Required selection actions:

- Select all.
- Clear selection or leave selection mode.
- Delete selected markers.
- Export selected markers.

Export and delete actions must require at least one selected marker.

### Delete selected markers

When the user chooses delete:

- Show a confirmation dialog.
- Include the number of selected markers.
- Delete only after confirmation.
- Refresh the list and map markers afterward.

Example confirmation text:

```text
Delete 3 selected markers?

This cannot be undone.
```

### Export selected markers

When the user chooses export:

- Require at least one selected marker.
- Launch Android Storage Access Framework using `ACTION_CREATE_DOCUMENT`.
- Suggest filename `opentopomap-markers.gpx`.
- Write selected markers as GPX 1.1 waypoints.

Do not request broad storage permissions.

### Import markers

Add an `Import GPX` action, preferably in the marker list toolbar/overflow menu.

When selected:

- Launch Android Storage Access Framework using `ACTION_OPEN_DOCUMENT`.
- Let the user select a GPX file.
- Parse GPX `<wpt>` elements.
- Add imported waypoints as markers.
- Do not delete or overwrite existing markers.
- Refresh the list and map markers afterward.

For the first version, duplicate detection and merge behavior are out of scope. It is acceptable to import duplicate markers.

## GPX format

### Export format

Export GPX 1.1.

Each marker should be written as one `<wpt>` element.

Minimum data:

```xml
<wpt lat="51.83860" lon="6.61550">
  <name>Marker label</name>
</wpt>
```

With optional description:

```xml
<wpt lat="51.83860" lon="6.61550">
  <name>Marker label</name>
  <desc>Optional marker note</desc>
</wpt>
```

Complete example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1"
     creator="OpenTopoMap Viewer"
     xmlns="http://www.topografix.com/GPX/1/1">
  <wpt lat="51.83860" lon="6.61550">
    <name>Parking place</name>
    <desc>Optional marker note</desc>
  </wpt>
  <wpt lat="51.84520" lon="6.62110">
    <name>Viewpoint</name>
  </wpt>
</gpx>
```

### Import format

Parse GPX `<wpt>` elements only.

Mapping:

- `wpt/@lat` -> marker latitude.
- `wpt/@lon` -> marker longitude.
- `wpt/name` -> marker label.
- `wpt/desc` -> marker note/description if supported.

Ignore GPX routes, tracks, and track points in this feature. The app may already support GPX tracks elsewhere, but marker import should only import waypoints.

### XML safety

When exporting, escape XML text content correctly.

When importing:

- Reject waypoints without valid latitude/longitude.
- Ignore malformed waypoint entries rather than crashing the app.
- Show a useful error if the selected file is not readable or contains no valid waypoints.

## Storage Access Framework

### Export intent

Use `ACTION_CREATE_DOCUMENT`.

Java-style example:

```java
Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
intent.addCategory(Intent.CATEGORY_OPENABLE);
intent.setType("application/gpx+xml");
intent.putExtra(Intent.EXTRA_TITLE, "opentopomap-markers.gpx");
startActivityForResult(intent, REQUEST_EXPORT_MARKERS);
```

If the project already uses Activity Result APIs, prefer the existing project style. Do not introduce a new style unnecessarily.

### Import intent

Use `ACTION_OPEN_DOCUMENT`.

```java
Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
intent.addCategory(Intent.CATEGORY_OPENABLE);
intent.setType("*/*");
startActivityForResult(intent, REQUEST_IMPORT_MARKERS);
```

The MIME type can be `*/*` because GPX files are often exposed by file managers as generic XML, octet-stream, or unknown MIME types.

## Data model

Inspect the current marker storage first.

If the app already stores marker labels/descriptions, reuse that model.

If the app currently stores only coordinates, extend the marker data model minimally:

```text
id: long
latitude: double, or existing coordinate representation
longitude: double, or existing coordinate representation
label: nullable String
note/description: nullable String, only if the app already has a natural place for it
createdAt: long, optional but useful
updatedAt: long, optional
```

Do not perform a large database refactor for this feature.

If the app stores coordinates as integer microdegrees or another internal representation, preserve the existing approach and convert only at GPX import/export boundaries.

## Suggested classes/components

Names can be adjusted to match the existing project conventions.

### UI

- `MarkerListActivity` or `MarkerListFragment`
- `MarkerAdapter`
- `MarkerViewHolder`
- `activity_marker_list.xml` or `fragment_marker_list.xml`
- `item_marker.xml`
- Optional menu XML: `menu_marker_list.xml`

### Data/service layer

- `MarkerRepository` or reuse existing marker storage class.
- `GpxMarkerExporter`
- `GpxMarkerImporter`

Keep GPX import/export logic out of the Activity/Fragment. The UI should call a small service/helper class.

## UI layout suggestion

### Marker list screen

Use a toolbar plus RecyclerView.

Possible layout structure:

```xml
<LinearLayout orientation="vertical">

    <androidx.appcompat.widget.Toolbar />

    <TextView
        android:id="@+id/emptyView"
        android:visibility="gone" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/markerRecyclerView" />

</LinearLayout>
```

### Row layout

Each row can contain:

- Optional checkbox, visible only in selection mode.
- Primary label text.
- Secondary coordinate text.

Example structure:

```xml
<LinearLayout orientation="horizontal">

    <CheckBox
        android:id="@+id/markerSelectedCheckBox"
        android:visibility="gone" />

    <LinearLayout orientation="vertical">
        <TextView android:id="@+id/markerLabelText" />
        <TextView android:id="@+id/markerCoordinatesText" />
    </LinearLayout>

</LinearLayout>
```

## Selection implementation

Keep selected marker IDs in memory inside the list screen.

Recommended approach:

```text
Set<Long> selectedMarkerIds
boolean selectionMode
```

Behavior:

- Long press enters selection mode and selects the item.
- In selection mode, row click toggles selection.
- If selection becomes empty, either remain in selection mode or exit selection mode. Pick whichever is more consistent with the app.
- Toolbar title can show `3 selected`.

Make sure selection survives simple adapter refreshes. It does not need to survive process death in the first version.

## Map/list synchronization

After marker changes:

- Marker edited: refresh list item and map marker.
- Marker deleted: remove from storage and map.
- Marker imported: add to storage and map.

If the map screen and list screen are separate activities, refresh the map markers in `onResume()` or through the existing marker reload mechanism.

## Error handling

Show user-friendly messages for:

- No markers selected for export.
- Export failed.
- Import failed.
- Selected file contains no valid GPX waypoints.
- Some waypoints were skipped because they were invalid.

Examples:

```text
No markers selected.
```

```text
Export failed: could not write the GPX file.
```

```text
Imported 5 markers.
```

```text
Imported 5 markers. Skipped 2 invalid waypoints.
```

Avoid crashing on malformed GPX.

## Acceptance criteria

### Marker management

- A marker list screen exists and is reachable from the app menu once markers exist.
- The list shows all saved markers.
- Tapping a marker opens the existing marker view/edit UI or equivalent shared marker editor.
- The list supports selection mode.
- The user can select one marker, multiple markers, or all markers.
- The user can delete selected markers after confirmation.
- The map and list reflect deleted or edited markers.

### Export

- The user can export selected markers.
- Export uses Android Storage Access Framework.
- No broad external storage permission is required.
- Exported file is valid GPX 1.1.
- Each selected marker is exported as a GPX waypoint.
- Marker labels are exported as `<name>` when available.
- Marker notes/descriptions are exported as `<desc>` when available.

### Import

- The user can select a GPX file for import.
- GPX waypoints are imported as markers.
- `<name>` is used as marker label.
- `<desc>` is used as marker note/description if supported.
- Existing markers are preserved.
- Imported markers appear in the marker list and on the map.

### UI consistency

- The feature is implemented with the existing XML/View approach.
- Jetpack Compose is not introduced for this feature.
- Existing marker edit behavior is reused where practical.

## Out of scope for the first implementation

- Jetpack Compose migration.
- KML export/import.
- GeoJSON export/import.
- Duplicate detection or merge behavior.
- Marker categories/folders.
- Marker icons/colors.
- Search/filter/sort UI.
- Cloud backup/sync.
- Exporting tracks/routes.
- Importing GPX tracks/routes as markers.

## Suggested implementation steps for Codex

1. Inspect existing marker model and storage.
2. Inspect existing marker creation/view/edit UI on the map.
3. Add or extend marker fields only if needed for label and optional description.
4. Create GPX marker exporter helper.
5. Create GPX marker importer helper.
6. Add marker list XML layout and row layout.
7. Implement marker list Activity/Fragment using RecyclerView.
8. Reuse existing marker edit UI when a list item is tapped.
9. Add selection mode to the marker list.
10. Add delete selected markers with confirmation dialog.
11. Add export selected markers using `ACTION_CREATE_DOCUMENT`.
12. Add import GPX using `ACTION_OPEN_DOCUMENT`.
13. Add or update menu entry for `Markers`.
14. Ensure map markers refresh after import/delete/edit.
15. Add tests where practical for GPX import/export.
16. Manually test export/import with at least one external GPX-capable app or file viewer.

## Test cases

### Export tests

- Export one marker with label.
- Export multiple selected markers.
- Export marker without label and verify fallback name or valid waypoint.
- Export marker with XML-sensitive characters in label, for example `A & B <test>`.
- Verify generated GPX is valid XML.

### Import tests

- Import GPX with one waypoint.
- Import GPX with multiple waypoints.
- Import GPX waypoint with `<name>`.
- Import GPX waypoint with `<desc>`.
- Import GPX with malformed waypoint coordinates and verify no crash.
- Import GPX with no waypoints and show a useful message.

### UI tests/manual checks

- Menu item appears when markers exist.
- Marker list opens.
- Tapping marker opens marker edit UI.
- Long press enters selection mode.
- Select all selects all markers.
- Export is disabled or blocked when no marker is selected.
- Delete asks for confirmation.
- Deleted markers disappear from list and map.
- Imported markers appear in list and map.

## Suggested issue comment

```text
I think we should implement this as a small marker management feature rather than only a direct export button.

The proposed first version:

- Add a Markers screen using the existing XML/View UI, not Jetpack Compose.
- Show the screen from the menu once markers exist.
- List all saved markers.
- Tapping a marker opens the same view/edit UI as selecting it on the map.
- Support selection mode in the list.
- Allow selecting one, multiple, or all markers.
- Allow deleting selected markers after confirmation.
- Allow exporting selected markers as GPX 1.1 waypoints.
- Add GPX waypoint import from the same screen.

GPX mapping:

- marker latitude/longitude -> GPX waypoint lat/lon
- marker label -> <name>
- marker note/description -> <desc>, if supported

For the first implementation I would keep KML/GeoJSON, duplicate handling, categories, filtering, and a Compose migration out of scope.
```
