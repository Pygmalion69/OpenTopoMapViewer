package org.nitri.opentopo.view

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.text.method.LinkMovementMethod
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
        val lifecycleOwner = context.findOwner<LifecycleOwner>()
        val viewModelStoreOwner = context.findOwner<ViewModelStoreOwner>()
        val savedStateRegistryOwner = context.findOwner<SavedStateRegistryOwner>()

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            // Set owners directly on the ComposeView
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    AboutDialogContent()
                }
            }
        }

        val dialog = AlertDialog.Builder(context, R.style.AlertDialogTheme)
            .setPositiveButton(R.string.close) { dialog, _ -> dialog.dismiss() }
            .create()

        // Set owners on the Dialog's window decor view immediately after creation
        dialog.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(lifecycleOwner)
            decorView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            decorView.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
        }

        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setView(composeView, 0, 0, 0, 0)

        val activity = context.findOwner<Activity>()
        val oldStatusBarColor = activity?.window?.statusBarColor

        dialog.setOnShowListener {
            activity?.window?.statusBarColor = context.getColor(R.color.colorPrimaryDark)
        }

        dialog.setOnDismissListener {
            if (oldStatusBarColor != null) {
                activity?.window?.statusBarColor = oldStatusBarColor
            }
        }

        dialog.show()
    }

    private inline fun <reified T> Context.findOwner(): T? {
        var curContext = this
        while (true) {
            if (curContext is T) {
                return curContext
            }
            if (curContext is ContextWrapper) {
                curContext = curContext.baseContext
            } else {
                break
            }
        }
        return null
    }
}

@Composable
fun AboutDialogContent() {
    val context = LocalContext.current
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
fun HtmlText(
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

@Preview(showBackground = true)
@Composable
fun AboutDialogPreview() {
    OpenTopoTheme(dynamicColor = false) {
        AboutDialogContent()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AboutDialogDarkPreview() {
    OpenTopoTheme(dynamicColor = false) {
        AboutDialogContent()
    }
}
