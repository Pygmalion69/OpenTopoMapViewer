package org.nitri.opentopo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import org.nitri.opentopo.analytics.AnalyticsNames
import org.nitri.opentopo.analytics.AnalyticsProvider
import org.nitri.opentopo.util.Utils
import org.nitri.opentopo.util.importOpenTopoMapZipToSqliteCache

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        AnalyticsProvider.get(this).trackScreen(
            AnalyticsNames.Screen.SETTINGS,
            SettingsActivity::class.java.simpleName
        )

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item)
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

            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.dialog_ors_api_key, null, false)
            val input = dialogView.findViewById<EditText>(R.id.input_ors_key)
            val explanation = dialogView.findViewById<TextView>(R.id.ors_explanation)

            explanation.text = Utils.fromHtml(getString(R.string.ors_explanation_html))
            explanation.movementMethod = LinkMovementMethod.getInstance()

            val dialog = AlertDialog.Builder(context, R.style.AlertDialogTheme)
                .setTitle(R.string.ors_api_key_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    val key = input.text.toString().trim()
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