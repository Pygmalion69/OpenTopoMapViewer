package org.nitri.opentopo.view

import android.content.Context
import android.content.ContextWrapper
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.nitri.opentopo.R
import org.nitri.opentopo.model.MarkerModel
import org.nitri.opentopo.ui.theme.OpenTopoTheme

object MarkerEditorDialog {
    fun show(
        context: Context,
        markerModel: MarkerModel,
        onUpdate: (MarkerModel) -> Unit,
        onDelete: ((MarkerModel) -> Unit)? = null
    ) {
        val lifecycleOwner = context.findOwner<LifecycleOwner>()
        val viewModelStoreOwner = context.findOwner<ViewModelStoreOwner>()
        val savedStateRegistryOwner = context.findOwner<SavedStateRegistryOwner>()

        var editedName by mutableStateOf(markerModel.name)
        var editedDescription by mutableStateOf(markerModel.description)

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

            setContent {
                OpenTopoTheme(dynamicColor = false) {
                    MarkerEditorContent(
                        initialName = markerModel.name,
                        initialDescription = markerModel.description,
                        onNameChange = { editedName = it },
                        onDescriptionChange = { editedDescription = it }
                    )
                }
            }
        }

        val dialogBuilder = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.edit_marker))
            .setView(composeView)
            .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                val updated = markerModel.copy(
                    name = editedName,
                    description = editedDescription
                )
                onUpdate(updated)
            }
            .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

        if (onDelete != null) {
            dialogBuilder.setNeutralButton(context.getString(R.string.delete)) { _, _ ->
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.confirm_delete))
                    .setMessage(context.getString(R.string.prompt_confirm_delete))
                    .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                        onDelete(markerModel)
                    }
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .create()
                    .also {
                        it.requestWindowFeature(Window.FEATURE_NO_TITLE)
                        it.show()
                    }
            }
        }

        val dialog = dialogBuilder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.window?.decorView?.let { decorView ->
            // Standard pattern for AppCompat + Compose dialog interoperability
            // Set owners on the Dialog's window decor view immediately after creation
            // to prevent "ViewTreeLifecycleOwner not found from AlertDialogLayout"
            decorView.setViewTreeLifecycleOwner(lifecycleOwner)
            decorView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            decorView.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
        }

        dialog.setOnShowListener {

            // Keyboard/IME handling workaround for Compose in Dialog
            // Must run after AlertDialog has configured its window.
            dialog.window?.clearFlags(
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            )
            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )
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
