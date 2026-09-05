package org.nitri.opentopo.view

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.nitri.opentopo.ui.theme.OpenTopoTheme

class ConfirmationDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val titleRes = requireArguments().getInt(ARG_TITLE)
        val messageRes = requireArguments().getInt(ARG_MESSAGE)
        val iconRes = requireArguments().getInt(ARG_ICON)
        val confirmButtonRes = requireArguments().getInt(ARG_CONFIRM_BUTTON)
        val dismissButtonRes = requireArguments().getInt(ARG_DISMISS_BUTTON)
        val requestKey = requireArguments().getString(ARG_REQUEST_KEY)
            ?: error("A Fragment Result request key is required")

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    ConfirmationDialogContent(
                        title = stringResource(titleRes),
                        message = stringResource(messageRes),
                        iconRes = iconRes,
                        confirmButton = stringResource(confirmButtonRes),
                        dismissButton = stringResource(dismissButtonRes),
                        onConfirm = {
                            parentFragmentManager.setFragmentResult(requestKey, bundleOf())
                            dismiss()
                        },
                        onDismiss = { dismiss() }
                    )
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(composeView)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setOnShowListener {
                window?.decorView?.let { decorView ->
                    decorView.setViewTreeLifecycleOwner(this@ConfirmationDialogFragment)
                    decorView.setViewTreeViewModelStoreOwner(this@ConfirmationDialogFragment)
                    decorView.setViewTreeSavedStateRegistryOwner(this@ConfirmationDialogFragment)
                }
            }
        }
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_ICON = "icon"
        private const val ARG_CONFIRM_BUTTON = "confirm_button"
        private const val ARG_DISMISS_BUTTON = "dismiss_button"
        private const val ARG_REQUEST_KEY = "request_key"

        fun newInstance(
            @StringRes titleRes: Int,
            @StringRes messageRes: Int,
            @DrawableRes iconRes: Int,
            @StringRes confirmButtonRes: Int,
            @StringRes dismissButtonRes: Int,
            requestKey: String
        ) = ConfirmationDialogFragment().apply {
            arguments = bundleOf(
                ARG_TITLE to titleRes,
                ARG_MESSAGE to messageRes,
                ARG_ICON to iconRes,
                ARG_CONFIRM_BUTTON to confirmButtonRes,
                ARG_DISMISS_BUTTON to dismissButtonRes,
                ARG_REQUEST_KEY to requestKey
            )
        }
    }
}

@Composable
private fun ConfirmationDialogContent(
    title: String,
    message: String,
    @DrawableRes iconRes: Int,
    confirmButton: String,
    dismissButton: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Text(
                text = message,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(dismissButton)
                }
                TextButton(onClick = onConfirm) {
                    Text(confirmButton)
                }
            }
        }
    }
}
