package org.nitri.opentopo

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

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
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
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
            val fm = requireActivity().supportFragmentManager
            cacheSettingsFragment.show(fm, "cache_settings")
        }
    }



    companion object {
        const val PREF_FULLSCREEN = "fullscreen"
        const val PREF_FULLSCREEN_ON_MAP_TAP = "fullscreen_on_map_tap"
        const val PREF_KEEP_SCREEN_ON = "keep_screen_on"
        const val PREF_TAP_COMPASS_TO_ROTATE = "tap_compass_to_rotate"
        const val PREF_ROTATE = "rotate"
    }
}