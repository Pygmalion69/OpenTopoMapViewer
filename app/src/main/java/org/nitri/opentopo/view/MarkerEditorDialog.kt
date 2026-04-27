package org.nitri.opentopo.view

import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import org.nitri.opentopo.R
import org.nitri.opentopo.model.MarkerModel

object MarkerEditorDialog {
    fun show(
        context: Context,
        markerModel: MarkerModel,
        onUpdate: (MarkerModel) -> Unit,
        onDelete: ((MarkerModel) -> Unit)? = null
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_marker, null)
        val nameInput = dialogView.findViewById<TextInputLayout>(R.id.nameInput)
        val descriptionInput = dialogView.findViewById<TextInputLayout>(R.id.descriptionInput)

        nameInput.editText?.setText(markerModel.name)
        descriptionInput.editText?.setText(markerModel.description)

        val dialogBuilder = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.edit_marker))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                markerModel.name = nameInput.editText?.text.toString()
                markerModel.description = descriptionInput.editText?.text.toString()
                onUpdate(markerModel)
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

        dialogBuilder.create().also {
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
            it.show()
        }
    }
}
