package org.nitri.opentopo.view

import android.app.Activity
import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.nitri.opentopo.R
import org.nitri.opentopo.ui.theme.OpenTopoTheme
import org.nitri.opentopo.util.Utils

object AboutDialog {

    fun show(context: Context) {
        val composeView = ComposeView(context).apply {
            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    val activity = context as? Activity
                    val statusBarColor = context.getColor(R.color.colorPrimaryDark)
                    SideEffect {
                        activity?.window?.statusBarColor = statusBarColor
                    }
                    AboutDialogContent(context)
                }
            }
        }

        // Using androidx.appcompat.app.AlertDialog to avoid MaterialComponents dependency crash
        val dialog = AlertDialog.Builder(context, R.style.AlertDialogTheme)
            .setPositiveButton(R.string.close) { dialog, _ -> dialog.dismiss() }
            .create()

        // Force no title before show
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        
        // Remove padding around the custom view
        dialog.setView(composeView, 0, 0, 0, 0)

        val lifecycleOwner = context as? LifecycleOwner
        val viewModelStoreOwner = context as? ViewModelStoreOwner
        val savedStateRegistryOwner = context as? SavedStateRegistryOwner

        dialog.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(lifecycleOwner)
            decorView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            decorView.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
        }

        dialog.show()

        // Extremely aggressive panel hiding after show()
        dialog.findViewById<View>(androidx.appcompat.R.id.topPanel)?.visibility = View.GONE
        dialog.findViewById<View>(androidx.appcompat.R.id.title_template)?.visibility = View.GONE
        dialog.findViewById<View>(androidx.appcompat.R.id.alertTitle)?.visibility = View.GONE
        dialog.findViewById<View>(androidx.appcompat.R.id.titleDividerNoCustom)?.visibility = View.GONE
        
        // Also hide contentPanel as we use setView which goes into customPanel
        dialog.findViewById<View>(androidx.appcompat.R.id.contentPanel)?.visibility = View.GONE
    }

    @Composable
    private fun AboutDialogContent(context: Context) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = Utils.getAppName(context),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HtmlText(
                html = stringResource(R.string.app_version, Utils.getAppVersion(context)),
                modifier = Modifier.padding(top = 8.dp)
            )
            HtmlText(
                html = stringResource(R.string.app_author),
                modifier = Modifier.padding(top = 8.dp)
            )
            HtmlText(
                html = stringResource(R.string.app_product_page),
                modifier = Modifier.padding(top = 16.dp)
            )
            HtmlText(
                html = stringResource(R.string.issue_tracker),
                modifier = Modifier.padding(top = 8.dp)
            )
            HtmlText(
                html = stringResource(R.string.about_open_topo_map),
                modifier = Modifier.padding(top = 16.dp)
            )
            HtmlText(
                html = stringResource(R.string.about_open_street_map),
                modifier = Modifier.padding(top = 8.dp)
            )
            HtmlText(
                html = stringResource(R.string.about_open_topo_map_r),
                modifier = Modifier.padding(top = 8.dp)
            )
            HtmlText(
                html = stringResource(R.string.about_top_o_map),
                modifier = Modifier.padding(top = 8.dp)
            )
            HtmlText(
                html = stringResource(R.string.about_freemap_sk),
                modifier = Modifier.padding(top = 8.dp)
            )
            HtmlText(
                html = stringResource(R.string.about_waymarked_trails),
                modifier = Modifier.padding(top = 16.dp)
            )
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
}
