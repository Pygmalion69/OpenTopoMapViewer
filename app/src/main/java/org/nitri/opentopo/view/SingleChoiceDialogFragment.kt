package org.nitri.opentopo.view

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.nitri.opentopo.R
import org.nitri.opentopo.ui.theme.OpenTopoTheme

class SingleChoiceDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = requireArguments()
        val titleRes = arguments.getInt(ARG_TITLE)
        val entries = arguments.getStringArray(ARG_ENTRIES).orEmpty().toList()
        val selectedIndex = arguments.getInt(ARG_SELECTED_INDEX)
        val requestKey = arguments.getString(ARG_REQUEST_KEY)
            ?: error("A Fragment Result request key is required")

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    SingleChoiceDialogContent(
                        title = stringResource(titleRes),
                        entries = entries,
                        selectedIndex = selectedIndex,
                        onSelected = { index ->
                            parentFragmentManager.setFragmentResult(
                                requestKey,
                                bundleOf(RESULT_SELECTED_INDEX to index)
                            )
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
                    decorView.setViewTreeLifecycleOwner(this@SingleChoiceDialogFragment)
                    decorView.setViewTreeViewModelStoreOwner(this@SingleChoiceDialogFragment)
                    decorView.setViewTreeSavedStateRegistryOwner(this@SingleChoiceDialogFragment)
                }
            }
        }
    }

    companion object {
        const val RESULT_SELECTED_INDEX = "selected_index"

        private const val ARG_TITLE = "title"
        private const val ARG_ENTRIES = "entries"
        private const val ARG_SELECTED_INDEX = "selected_index"
        private const val ARG_REQUEST_KEY = "request_key"

        fun newInstance(
            @StringRes titleRes: Int,
            entries: Array<String>,
            selectedIndex: Int,
            requestKey: String
        ) = SingleChoiceDialogFragment().apply {
            arguments = bundleOf(
                ARG_TITLE to titleRes,
                ARG_ENTRIES to entries,
                ARG_SELECTED_INDEX to selectedIndex,
                ARG_REQUEST_KEY to requestKey
            )
        }
    }
}

@Composable
private fun SingleChoiceDialogContent(
    title: String,
    entries: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .selectableGroup()
            ) {
                itemsIndexed(entries) { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = index == selectedIndex,
                                onClick = { onSelected(index) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = null
                        )
                        Text(
                            text = entry,
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
