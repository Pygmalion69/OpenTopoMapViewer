# Focus crash investigation

The supplied framework stack indicates a view that is no longer a descendant when
ViewRootImpl converts its rectangle. It does not identify the outgoing screen,
Android version, or view. Stale input focus is a hypothesis, not a reproduced cause.

## Targeted changes

- Place search, marker editor, cache settings and API-key dialogs contain Compose
  text inputs hosted in Android dialogs. Their dialog subclasses clear Compose
  focus and the local host View focus before `dismiss()` or `hide()` removes/hides
  the window. Lifecycle pause and view teardown cover rotation and leaving the
  screen. Save/selection/delete callbacks clear focus before delivering results.
- API-key saving now dismisses the dialog before replacing SettingsFragment.
  SettingsFragment also dismisses its owned input dialog on view destruction and
  disposes that dialog's composition on dismissal.
- Place search contains the only explicit `requestFocus()` in application source.
  Its composition coroutine waits for RESUMED and window focus, verifies that the
  host belongs to the currently showing dialog, and is cancelled on pause or
  disposal. It requests initial focus once, preserving the existing rotation rule.
- Map/detail/nearby replacement and settings replacement clear only the outgoing
  container. Play ad replacement/destruction clears only the outgoing ad hierarchy;
  initialization uses the activity lifecycle scope and rejects loads after finish
  or destruction.
- Transition breadcrumbs record root/focused view class, resource ID (or numeric
  fallback), and attachment. They never include field values, labels or view
  `toString()`. Play uses existing Crashlytics; FOSS uses Android Log only.

## Audit scope and uncertainty

No posted/delayed View focus requests were found. Reviewed fragment navigation,
dialog buttons, Compose callbacks and ad hierarchy updates execute on the main
thread; no concrete off-main hierarchy mutation was identified. The ad SDK's
background initialization already marshals view creation back to the UI thread.

Place-search results and marker lists have no editable rows; search updates leave
the input mounted. Settings preferences change visibility during initial creation.
AndroidView interop hosts the settings fragment container and non-editable HTML
TextViews. No speculative per-row focus clearing was added. Hardware-keyboard
focus during list updates still needs device testing.

## Validation

Passed `compileFossDebugKotlin`, `compilePlayDebugKotlin`, and
`testFossDebugUnitTest`, plus `git diff --check`. No device was connected, so the
following runtime checks remain unverified:

1. Open each input dialog, type with the keyboard visible, then save/cancel; also
   use Back and outside-tap dismissal where supported. Reopen and type again.
2. Save an API key while focused; verify dialog closure and preference replacement.
   Navigate away from settings and from map/detail/nearby screens with keyboard
   or hardware-keyboard focus active.
3. Change search results while typing; select a result. Edit/delete a marker and
   confirm deletion. Use hardware-keyboard focus when updating/removing list rows.
4. Rotate each input dialog while focused; confirm restored content where supported,
   no leaked window, and normal keyboard behavior after rotation and reopening.
5. Repeat on both flavors; check focus breadcrumbs in Logcat (FOSS) or Crashlytics
   (Play), with no user-entered text in those breadcrumbs.

These checks do not establish that the reported production crash is fixed. New
breadcrumbs should help correlate any recurrence with a specific hierarchy.
