package org.nitri.opentopo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.launch
import org.nitri.opentopo.analytics.AnalyticsNames
import org.nitri.opentopo.analytics.AnalyticsProvider
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import org.nitri.opentopo.util.Utils
import org.nitri.opentopo.util.importOpenTopoMapZipToSqliteCache

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsProvider.get(this).trackScreen(
            AnalyticsNames.Screen.SETTINGS,
            SettingsActivity::class.java.simpleName
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }

        setContent {
            OpenTopoTheme(dynamicColor = false) {
                SettingsScreen(
                    onBack = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val profileKeys = listOf(
            "driving-car", "driving-hgv",
            "cycling-regular", "cycling-road", "cycling-mountain", "cycling-electric",
            "foot-walking", "foot-hiking",
            "wheelchair"
        )

        private val openZipLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                importZip(it)
            }
        }

        private fun importZip(uri: Uri) {
            val context = context ?: return
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            Toast.makeText(context, R.string.import_started, Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch {
                val result = importOpenTopoMapZipToSqliteCache(context, uri)
                if (result.error != null) {
                    Toast.makeText(context, getString(R.string.import_failed, result.error), Toast.LENGTH_LONG).show()
                } else if (result.imported > 0) {
                    Toast.makeText(context, getString(R.string.import_success, result.imported), Toast.LENGTH_LONG).show()
                    Log.d("SettingsFragment", "Imported ${result.imported}, skipped ${result.skipped}")
                    if (result.skippedSamples.isNotEmpty()) {
                        Log.d("SettingsFragment", "Skipped samples: ${result.skippedSamples.joinToString()}")
                    }
                } else {
                    Toast.makeText(context, R.string.import_no_tiles, Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val apiKey = prefs.getString(PREF_ORS_API_KEY, null)
            val currentProfile = prefs.getString(PREF_ORS_PROFILE, "driving-car")

            val setKeyPref = findPreference<Preference>("ors_set_key")
            val eraseKeyPref = findPreference<Preference>("ors_erase_key")
            val profilePref = findPreference<Preference>("ors_select_profile")
            val tapCompassToRotatePref = findPreference<Preference>(PREF_TAP_COMPASS_TO_ROTATE)
            val importTilesPref = findPreference<Preference>("import_tiles")

            importTilesPref?.setOnPreferenceClickListener {
                openZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"))
                true
            }

            tapCompassToRotatePref?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (!enabled) {
                    prefs.edit { putBoolean(PREF_ROTATE, false) }
                }
                true
            }

            if (apiKey.isNullOrBlank()) {
                setKeyPref?.isVisible = true
                eraseKeyPref?.isVisible = false
                profilePref?.isVisible = false

                setKeyPref?.setOnPreferenceClickListener {
                    showOrsApiKeyDialog()
                    true
                }
            } else {
                setKeyPref?.isVisible = false
                eraseKeyPref?.isVisible = true
                profilePref?.isVisible = true

                eraseKeyPref?.setOnPreferenceClickListener {
                    showEraseConfirmationDialog()
                    true
                }

                context?.let { ctx ->
                    val profileIndex = profileKeys.indexOf(currentProfile)
                    val label = getProfileLabels(ctx).getOrNull(profileIndex) ?: currentProfile
                    profilePref?.summary = label
                }

                profilePref?.setOnPreferenceClickListener {
                    showRoutingProfileDialog()
                    true
                }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "cache_settings" -> {
                    showCacheSettings()
                    return true
                }
            }
            return super.onPreferenceTreeClick(preference)
        }

        private fun showCacheSettings() {
            val cacheSettingsFragment = CacheSettingsFragment()
            activity?.supportFragmentManager?.let { fm ->
                cacheSettingsFragment.show(fm, "cache_settings")
            }
        }

        private fun showOrsApiKeyDialog() {
            val context = requireContext()

            val lifecycleOwner = viewLifecycleOwner
            val viewModelStoreOwner = requireActivity()
            val savedStateRegistryOwner = requireActivity()

            var enteredKey = ""

            val composeView = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

                setContent {
                    OpenTopoTheme(dynamicColor = false) {
                        OrsApiKeyDialogContent(onKeyChange = { enteredKey = it })
                    }
                }
            }

            val dialog = AlertDialog.Builder(context, R.style.AlertDialogTheme)
                .setTitle(R.string.ors_api_key_title)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    val key = enteredKey.trim()
                    if (key.isNotEmpty()) {
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .edit { putString(PREF_ORS_API_KEY, key) }
                        Toast.makeText(context, "Key saved", Toast.LENGTH_SHORT).show()
                        val intent = Intent(ACTION_API_KEY_CHANGED)
                        context.sendBroadcast(intent)
                        recreate()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()

            dialog.setView(composeView, 0, 0, 0, 0)

            // Required for Compose inside AppCompat AlertDialog.
            // Without owners on the dialog decor view, Compose may fail with:
            // "ViewTreeLifecycleOwner not found from AlertDialogLayout".
            dialog.window?.decorView?.let { decorView ->
                decorView.setViewTreeLifecycleOwner(lifecycleOwner)
                decorView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                decorView.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            }

            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.show()

        }

        private fun showRoutingProfileDialog() {
            val context = requireContext()
            val profiles = profileKeys
            val labels = getProfileLabels(context)
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.select_ors_profile)
                .setItems(labels) { _, which ->
                    val selectedKey = profiles[which]
                    prefs.edit { putString(PREF_ORS_PROFILE, selectedKey) }
                    Toast.makeText(context, context.getString(R.string.profile_set, labels[which]), Toast.LENGTH_SHORT).show()
                    recreate()
                }
                .create()
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.show()
        }

        private fun getProfileLabels(context: Context): Array<String> {
            return arrayOf(
                context.getString(R.string.profile_driving_car),
                context.getString(R.string.profile_driving_hgv),
                context.getString(R.string.profile_cycling_regular),
                context.getString(R.string.profile_cycling_road),
                context.getString(R.string.profile_cycling_mountain),
                context.getString(R.string.profile_cycling_electric),
                context.getString(R.string.profile_foot_walking),
                context.getString(R.string.profile_foot_hiking),
                context.getString(R.string.profile_wheelchair)
            )
        }

        private fun showEraseConfirmationDialog() {
            val context = requireContext()
            val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.erase_api_key)
                .setMessage(R.string.ors_erase_confirm)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .edit { remove(PREF_ORS_API_KEY) }
                    val intent = Intent(ACTION_API_KEY_CHANGED)
                    context.sendBroadcast(intent)
                    Toast.makeText(context, R.string.key_erased, Toast.LENGTH_SHORT).show()
                    recreate()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.show()
        }

        private fun recreate() {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(id, SettingsFragment())
                ?.commit()
        }
    }

    companion object {
        const val PREF_FULLSCREEN = "fullscreen"
        const val PREF_FULLSCREEN_ON_MAP_TAP = "fullscreen_on_map_tap"
        const val PREF_KEEP_SCREEN_ON = "keep_screen_on"
        const val PREF_TAP_COMPASS_TO_ROTATE = "tap_compass_to_rotate"
        const val PREF_ROTATE = "rotate"
        const val PREF_MAX_ZOOM_LEVEL = "max_zoom_level"
        const val PREF_KML_ENABLED = "kml_enabled"
        const val PREF_ORS_API_KEY = "ors_api_key"
        const val PREF_ORS_PROFILE = "ors_profile"
        const val ACTION_API_KEY_CHANGED = "org.nitri.opentopo.API_KEY_CHANGED"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        AndroidView(
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = View.generateViewId()
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
            val fragmentManager = (view.context as? AppCompatActivity)?.supportFragmentManager
            if (fragmentManager != null && fragmentManager.findFragmentById(view.id) == null) {
                fragmentManager.beginTransaction()
                    .replace(view.id, SettingsActivity.SettingsFragment())
                    .commit()
            }
        }
    }
}

@Composable
private fun HtmlText(
    html: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { it.text = Utils.fromHtml(html) }
    )
}

@Composable
private fun OrsApiKeyDialogContent(onKeyChange: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        HtmlText(
            html = stringResource(R.string.ors_explanation_html),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = key,
            onValueChange = {
                key = it
                onKeyChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            label = { Text(stringResource(R.string.ors_hint)) },
            singleLine = true
        )
    }
}
