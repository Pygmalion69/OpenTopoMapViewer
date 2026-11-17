# Migrating `SettingsActivity` to Jetpack Compose

This note analyses how `settings_activity.xml` is used in the current code‐base and
describes a safe migration strategy to replace that XML layout with a Compose
implementation.  The goal is to drop the legacy View hierarchy, adopt
edge‑to‑edge behaviour across Android versions and preserve existing
functionality, particularly the toolbar appearance and `PreferenceFragmentCompat`
integration.

## 1 Current layout and its behaviour

### 1.1 `settings_activity.xml`

The activity uses a simple vertical `LinearLayout`:

- The root view has `android:fitsSystemWindows="true"` and fills the
  screen.  It therefore delegates inset handling to the system: on older
  versions the system will insert padding at the top (for the status bar) and
  bottom (for navigation bars).  This root container prevents content from
  drawing under the system bars.
- A plain `Toolbar` is placed at the top.  The activity calls
  `setSupportActionBar(toolbar)` and enables the home‑as‑up arrow.  The
  toolbar’s title text and navigation icon are tinted white in Java and the
  action bar size is taken from the theme.  There is no explicit title in
  XML; the action bar API handles it.
- A `FrameLayout` with id `settings` fills the remainder of the screen.  A
  `SettingsFragment` (which extends `PreferenceFragmentCompat`) is
  dynamically inserted into this container in `onCreate`.

Because `fitsSystemWindows` is true, the root `LinearLayout` receives
system insets and pads itself accordingly.  This means the toolbar begins
below the status bar on all API levels.  Navigation bar space is also
reserved automatically.  There is no variant for this layout; thus the
behaviour is uniform across all devices.

### 1.2 Edge‑to‑edge in the rest of the app

Other screens in this app handle system bars in a more modern way:

- `MarkerListActivity` calls
  `WindowCompat.setDecorFitsSystemWindows(window, false)` and then
  applies the insets manually in code.  It adjusts the toolbar height and
  padding based on `WindowInsets` for the status bar and adds bottom
  padding to the `RecyclerView`.  This enables full
  edge‑to‑edge drawing while still keeping content visible.
- `activity_main.xml` has two variants.  The default layout uses
  `fitsSystemWindows="true"`; the `layout‑v35` variant (used on
  higher API levels) sets `fitsSystemWindows="false"` and instead
  hard‑codes a top margin equal to the action bar size
 .  The activity then applies window insets in code.

These patterns show that the app already differentiates between legacy
inset handling (automated via `fitsSystemWindows`) and the modern
edge‑to‑edge approach (manual inset handling with `WindowInsets`).

### 1.3 Implications for Compose

Jetpack Compose does not honour `fitsSystemWindows` automatically, and
there is no XML.  Instead, you explicitly call
`WindowCompat.setDecorFitsSystemWindows(window, false)` and use
`WindowInsets` or `Modifier.systemBarsPadding()` to handle insets.

On API 29 and higher, Google recommends drawing behind the system bars and
handling insets yourself for a consistent gesture‑navigation experience.
Older devices can continue using the legacy `fitsSystemWindows` behaviour
if needed, but Compose can emulate it by calling
`WindowCompat.setDecorFitsSystemWindows(window, true)` or by adding
appropriate padding.

## 2 Migration strategy

### 2.1 Prepare the project

1. **Ensure Compose dependencies are present.**  The project already
   includes Compose for dialogs, so the BOM and Material 3 dependencies
   should be in place.  Confirm that `app/build.gradle` declares:
   ```groovy
   buildFeatures {
       compose true
   }
   composeOptions {
       kotlinCompilerExtensionVersion = "<latest>"
   }
   dependencies {
       implementation(platform("androidx.compose:compose-bom:<latest>"))
       implementation("androidx.compose.material3:material3")
       // … other Compose libs …
   }
   ```
2. **Delete `settings_activity.xml`.**  Once the Compose implementation
   replicates the same behaviour, remove this layout from `res/layout` to
   avoid dead resources.

### 2.2 Convert `SettingsActivity` to Compose

1. **Change `SettingsActivity` to use Compose content.**  Remove
   `setContentView(R.layout.settings_activity)` and replace it with a
   `setContent` block:

   ```kotlin
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)

       // Adopt edge‑to‑edge.  For API >= 29 set fitsSystemWindows to
       // false so the window draws behind system bars.  For older
       // versions you can set it to true to preserve legacy behaviour.
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
           WindowCompat.setDecorFitsSystemWindows(window, false)
       } else {
           WindowCompat.setDecorFitsSystemWindows(window, true)
       }

       setContent {
           OpenTopoTheme(dynamicColor = false) {
               SettingsScreen(
                   onBack = { onBackPressedDispatcher.onBackPressed() },
                   savedInstanceState = savedInstanceState
               )
           }
       }
   }
   ```

   Calling `setDecorFitsSystemWindows(window, false)` on
   API 29+ mirrors the behaviour of `MarkerListActivity` and prepares the
   activity for manual inset handling.  On older versions you may prefer
   `true` to replicate the XML behaviour; the provided example uses a
   conditional check.

2. **Create a `SettingsScreen` composable** which replicates the existing
   layout:

   ```kotlin
   @Composable
   fun SettingsScreen(onBack: () -> Unit, savedInstanceState: Bundle?) {
       // Use a Scaffold to host a top app bar and content.  Apply
       // system bars padding to avoid drawing under the status bar on
       // API >= 29.  On older versions systemBarsPadding() will add zero
       // padding when decorFitsSystemWindows is true.
       Scaffold(
           topBar = {
               SmallTopAppBar(
                   title = { Text(stringResource(R.string.settings)) },
                   navigationIcon = {
                       IconButton(onClick = onBack) {
                           Icon(
                               painterResource(R.drawable.ic_arrow_back_white),
                               contentDescription = null,
                               tint = Color.White
                           )
                       }
                   },
                   colors = TopAppBarDefaults.smallTopAppBarColors(
                       containerColor = MaterialTheme.colorScheme.primary,
                       titleContentColor = Color.White,
                       navigationIconContentColor = Color.White
                   ),
                   modifier = Modifier.statusBarsPadding()
               )
           }
       ) { innerPadding ->
           // Compose does not automatically host PreferenceFragmentCompat.
           // Use AndroidView to embed a FragmentContainerView and add the
           // SettingsFragment dynamically if it has not been added.
           AndroidView(
               factory = { context ->
                   FragmentContainerView(context).apply {
                       id = View.generateViewId()
                       // Set layout params so the fragment occupies the
                       // remaining space beneath the app bar.
                       layoutParams = ViewGroup.LayoutParams(
                           ViewGroup.LayoutParams.MATCH_PARENT,
                           ViewGroup.LayoutParams.MATCH_PARENT
                       )
                   }
               },
               modifier = Modifier
                  .fillMaxSize()
                  .padding(innerPadding)
           ) { view ->
               val fragmentManager = (context as? AppCompatActivity)?.supportFragmentManager
               if (fragmentManager != null && fragmentManager.findFragmentById(view.id) == null) {
                   fragmentManager.beginTransaction()
                      .replace(view.id, SettingsActivity.SettingsFragment())
                      .commit()
               }
           }
       }
   }
   ```

   Notes:

   * The `SmallTopAppBar` replicates the old `Toolbar`.  The title uses
     `R.string.settings`; you can localise as needed.  White text and
     navigation icon colours are preserved.
   * `statusBarsPadding()` adds top padding equal to the status bar
     height when `decorFitsSystemWindows` is false (API 29+).  On older
     versions the padding is zero, preserving pre‑edge‑to‑edge behaviour.
   * The `Scaffold`’s content uses `innerPadding` provided by the
     scaffold.  It ensures the fragment container is placed below the
     app bar.
   * `AndroidView` hosts a `FragmentContainerView` in Compose.  A
     `View.generateViewId()` call is required because Compose does not
     assign IDs automatically.  The fragment transaction is executed
     only when the container has no fragment.  This preserves the
     existing `savedInstanceState` behaviour: the framework restores the
     fragment automatically when the activity recreates.

3. **Remove toolbar setup calls** from `SettingsActivity`.  The
   Compose top app bar handles title colour and navigation icon.  The
   `setSupportActionBar`, `setDisplayHomeAsUpEnabled` and tinting calls
   in `onCreate` are no longer necessary.

4. **Keep Analytics tracking.**  The existing code calls
   `AnalyticsProvider.get(this).trackScreen(...)` in `onCreate`.
   You should retain this call before `setContent` to preserve analytics.

5. **Retain the fragment transaction logic.**  The `SettingsFragment` is
   still a `PreferenceFragmentCompat` reading `R.xml.preferences`.  This
   migration focuses only on removing the layout XML.  A future step can
   convert the preferences screen to full Compose.

### 2.3 Handling system bars across Android versions

Jetpack Compose does not automatically reserve space for system bars, so
you must decide how to handle insets.  Recommended approach:

* **API 29+ (Android 10 and later):** set
  `WindowCompat.setDecorFitsSystemWindows(window, false)` and use
  `Modifier.systemBarsPadding()` or `statusBarsPadding()` on your top bar.
  This draws your app behind the status and navigation bars, resulting in
  an edge‑to‑edge experience.  Compose’s padding modifiers handle the
  correct insets.  If you want the nav bar colour to match your
  background, call `window.navigationBarColor = Color.Transparent.toArgb()`
  in a `SideEffect`.  To ensure icons remain visible on light backgrounds,
  obtain a `WindowInsetsControllerCompat` and set
  `isAppearanceLightStatusBars`/`isAppearanceLightNavigationBars`
  appropriately.

* **API 21–28 (Lollipop to Pie):** you can either leave
  `setDecorFitsSystemWindows` as `true` (legacy behaviour) or adopt
  edge‑to‑edge manually.  If you keep it `true`, remove
  `statusBarsPadding()` so that the system inset is applied implicitly.
  If you adopt edge‑to‑edge, call `false` and use
  `systemBarsPadding()`.  Do not forget to set a dark status bar colour
  (default is `colorPrimaryDark`) because icons cannot be tinted light
  on API 23 and below.

* **API < 21:** Compose is available only on API 21+, so nothing special
  is required.  If you support lower levels, continue using the XML
  layout and guard the Compose code behind an `if` or a flavour.

## 3 Avoiding unintended side effects

* **Empty title bar:** Do not create a `TopAppBar` outside of a
  `Scaffold` and then hide it; otherwise Compose will insert extra
  padding and you might see blank space.  Use the `Scaffold`’s
  `topBar` slot as shown above.
* **Status bar overlap:** If you forget to apply
  `Modifier.statusBarsPadding()` when `decorFitsSystemWindows` is false,
  your toolbar will draw under the system status bar.  Similarly,
  forgetting to apply `innerPadding` to the content will cause the
  fragment to overlap the toolbar.
* **Multiple fragment transactions:** When hosting the
  `SettingsFragment` in `AndroidView`, ensure you check that a fragment
  isn’t already present before replacing it.  Otherwise, Compose
  recomposition may retrigger the transaction.
* **Saving and restoring state:** Because the fragment is now hosted in
  Compose, call `setContent` outside of `if (savedInstanceState == null)`.
  Compose manages its own state and will restore automatically.  The
  fragment manager still saves and restores fragments by ID.

## 4 Next steps

This migration removes the need for an XML layout while preserving
functionality.  After merging this change, consider migrating the
`SettingsFragment` itself to Compose.  This will eliminate the
preference XML file and let you implement settings as composable rows,
switches and dialogs.  The project has already adopted Compose for
dialogs (ORS API key and cache settings), so the next logical step is to
create a `SettingsScreen` composable that uses `PreferenceDataStore` or
`DataStore` to persist settings.

---
By following the instructions above you can safely migrate
`SettingsActivity` to Jetpack Compose, adopt edge‑to‑edge rendering on
modern Android versions and remove the last remaining layout XML from
this screen.