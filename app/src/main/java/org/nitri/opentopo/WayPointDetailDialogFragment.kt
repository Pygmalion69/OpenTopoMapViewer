package org.nitri.opentopo

import android.app.Dialog
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.nitri.opentopo.model.WayPointItem
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import org.nitri.opentopo.view.HtmlText

class WayPointDetailDialogFragment : DialogFragment() {
    private var mCallback: Callback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragmentActivity = requireActivity()
        mCallback =
            fragmentActivity.supportFragmentManager.findFragmentByTag(
                BaseMainActivity.GPX_DETAIL_FRAGMENT_TAG
            ) as Callback?

        val wayPoint = mCallback?.getSelectedWayPointItem()?.wayPoint
        val name = wayPoint?.name.orEmpty()
        val description = wayPoint?.desc
            ?.takeIf { it.isNotBlank() }
            ?.replace("href=\"//", "href=\"http://")

        val composeView = ComposeView(fragmentActivity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    WayPointDetailContent(
                        name = name,
                        description = description
                    )
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        val dialog = AlertDialog.Builder(fragmentActivity)
            .setView(composeView)
            .create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setOnShowListener {
            dialog.window?.decorView?.let { decorView ->
                decorView.setViewTreeLifecycleOwner(this)
                decorView.setViewTreeViewModelStoreOwner(this)
                decorView.setViewTreeSavedStateRegistryOwner(this)
            }
        }

        return dialog
    }

    internal interface Callback {
        fun getSelectedWayPointItem(): WayPointItem?
    }
}

@Composable
private fun WayPointDetailContent(
    name: String,
    description: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        if (description != null) {
            HtmlText(
                html = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}
