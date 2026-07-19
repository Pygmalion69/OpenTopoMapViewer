package org.nitri.opentopo.ui.color

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.nitri.opentopo.R

class ColorPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var color: Int = Color.TRANSPARENT

    init {
        widgetLayoutResource = R.layout.preference_color_widget
    }

    fun setColor(color: Int) {
        this.color = color
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val swatch = holder.findViewById(R.id.color_swatch)
        val drawable = swatch.background as? GradientDrawable
        drawable?.setColor(color)
    }
}
