package org.nitri.opentopo.view

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.nitri.opentopo.R
import androidx.core.net.toUri

/**
 * Preference for the ORS API key with a right-side clickable info icon.
 * The main item click is handled in SettingsFragment; this class only wires the widget icon.
 */
class OrsKeyPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val info = holder.findViewById(R.id.info_button) as? ImageView
        info?.setOnClickListener {
            openHelpUrl()
        }
    }

    private fun openHelpUrl() {
        val url = "https://pygmalion.nitri.org/how-to-enable-basic-routing-with-openrouteservice-ors-in-opentopomap-viewer-1818.html"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_SHORT).show()
        }
    }
}
