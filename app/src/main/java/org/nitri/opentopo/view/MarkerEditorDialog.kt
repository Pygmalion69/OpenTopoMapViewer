package org.nitri.opentopo.view

import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import org.nitri.opentopo.ui.color.APP_COLOR_PALETTE
import org.nitri.opentopo.ui.color.ColorPickerGrid
import org.nitri.opentopo.ui.color.DEFAULT_MARKER_COLOR
import org.nitri.opentopo.ui.theme.OpenTopoTheme

class MarkerEditorDialog : DialogFragment() {

    private var editedName: String = ""
    private var editedDescription: String = ""
    private var editedColor: Int = DEFAULT_MARKER_COLOR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialName = arguments?.getString(ARG_NAME).orEmpty()
        val initialDescription = arguments?.getString(ARG_DESCRIPTION).orEmpty()
        val initialColor = arguments?.getInt(ARG_COLOR, DEFAULT_MARKER_COLOR) ?: DEFAULT_MARKER_COLOR

        editedName = savedInstanceState?.getString(STATE_EDITED_NAME) ?: initialName
        editedDescription = savedInstanceState?.getString(STATE_EDITED_DESCRIPTION) ?: initialDescription
        editedColor = savedInstanceState?.getInt(STATE_EDITED_COLOR) ?: initialColor
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_EDITED_NAME, editedName)
        outState.putString(STATE_EDITED_DESCRIPTION, editedDescription)
        outState.putInt(STATE_EDITED_COLOR, editedColor)
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
                        initialColor = editedColor,
                        onNameChange = { editedName = it },
                        onDescriptionChange = { editedDescription = it },
                        onColorChange = { editedColor = it }
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
                        putInt(RESULT_COLOR, editedColor)
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
        private const val ARG_COLOR = "arg_color"

        private const val STATE_EDITED_NAME = "state_edited_name"
        private const val STATE_EDITED_DESCRIPTION = "state_edited_description"
        private const val STATE_EDITED_COLOR = "state_edited_color"

        const val RESULT_REQUEST_KEY = "marker_editor_result"
        const val RESULT_ACTION = "action"
        const val RESULT_ACTION_UPDATE = "update"
        const val RESULT_ACTION_DELETE = "delete"
        const val RESULT_MARKER_ID = "marker_id"
        const val RESULT_NAME = "name"
        const val RESULT_DESCRIPTION = "description"
        const val RESULT_COLOR = "color"

        fun newInstance(id: Int, name: String, description: String, color: Int): MarkerEditorDialog {
            return MarkerEditorDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, id)
                    putString(ARG_NAME, name)
                    putString(ARG_DESCRIPTION, description)
                    putInt(ARG_COLOR, color)
                }
            }
        }
    }
}

@Composable
private fun MarkerEditorContent(
    initialName: String,
    initialDescription: String,
    initialColor: Int,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onColorChange: (Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var color by remember { mutableIntStateOf(initialColor) }
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_color),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ColorPickerGrid(
                colors = APP_COLOR_PALETTE,
                selectedColor = color,
                onColorSelected = {
                    color = it
                    onColorChange(it)
                    showColorPicker = false
                }
            )
            TextButton(
                onClick = { showColorPicker = false },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    } else {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                minLines = 4,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showColorPicker = true }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.marker_color),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }
    }
}
