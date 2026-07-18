package org.nitri.opentopo.view

import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.nitri.opentopo.R
import org.nitri.opentopo.ui.theme.OpenTopoTheme

class MarkerEditorDialog : DialogFragment() {

    private var editedName: String = ""
    private var editedDescription: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialName = arguments?.getString(ARG_NAME).orEmpty()
        val initialDescription = arguments?.getString(ARG_DESCRIPTION).orEmpty()

        editedName = savedInstanceState?.getString(STATE_EDITED_NAME) ?: initialName
        editedDescription = savedInstanceState?.getString(STATE_EDITED_DESCRIPTION) ?: initialDescription
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_EDITED_NAME, editedName)
        outState.putString(STATE_EDITED_DESCRIPTION, editedDescription)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val markerId = arguments?.getInt(ARG_ID) ?: -1

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@MarkerEditorDialog)
            setViewTreeViewModelStoreOwner(this@MarkerEditorDialog)
            setViewTreeSavedStateRegistryOwner(this@MarkerEditorDialog)

            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    MarkerEditorContent(
                        initialName = editedName,
                        initialDescription = editedDescription,
                        onNameChange = { editedName = it },
                        onDescriptionChange = { editedDescription = it }
                    )
                }
            }
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_marker)
            .setView(composeView)
            .setPositiveButton(R.string.ok) { _, _ ->
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    Bundle().apply {
                        putString(RESULT_ACTION, RESULT_ACTION_UPDATE)
                        putInt(RESULT_MARKER_ID, markerId)
                        putString(RESULT_NAME, editedName)
                        putString(RESULT_DESCRIPTION, editedDescription)
                    }
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.delete) { _, _ ->
                showDeleteConfirmation(markerId)
            }

        val dialog = builder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Standard pattern for AppCompat + Compose dialog interoperability
        // Set owners on the Dialog's window decor view immediately after creation
        // to prevent "ViewTreeLifecycleOwner not found from AlertDialogLayout"
        dialog.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        dialog.setOnShowListener {
            // Keyboard/IME handling workaround for Compose in Dialog
            // Must run after AlertDialog has configured its window.
            // Using SOFT_INPUT_ADJUST_NOTHING for stability in portrait and landscape.
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        return dialog
    }

    private fun showDeleteConfirmation(markerId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.prompt_confirm_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    Bundle().apply {
                        putString(RESULT_ACTION, RESULT_ACTION_DELETE)
                        putInt(RESULT_MARKER_ID, markerId)
                    }
                )
                dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also {
                it.requestWindowFeature(Window.FEATURE_NO_TITLE)
                it.show()
            }
    }

    companion object {
        const val TAG = "MarkerEditorDialog"

        private const val ARG_ID = "arg_id"
        private const val ARG_NAME = "arg_name"
        private const val ARG_DESCRIPTION = "arg_description"

        private const val STATE_EDITED_NAME = "state_edited_name"
        private const val STATE_EDITED_DESCRIPTION = "state_edited_description"

        const val RESULT_REQUEST_KEY = "marker_editor_result"
        const val RESULT_ACTION = "action"
        const val RESULT_ACTION_UPDATE = "update"
        const val RESULT_ACTION_DELETE = "delete"
        const val RESULT_MARKER_ID = "marker_id"
        const val RESULT_NAME = "name"
        const val RESULT_DESCRIPTION = "description"

        fun newInstance(id: Int, name: String, description: String): MarkerEditorDialog {
            return MarkerEditorDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, id)
                    putString(ARG_NAME, name)
                    putString(ARG_DESCRIPTION, description)
                }
            }
        }
    }
}

@Composable
private fun MarkerEditorContent(
    initialName: String,
    initialDescription: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onNameChange(it)
            },
            label = { Text(stringResource(R.string.name)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                onDescriptionChange(it)
            },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text
            )
        )
    }
}
