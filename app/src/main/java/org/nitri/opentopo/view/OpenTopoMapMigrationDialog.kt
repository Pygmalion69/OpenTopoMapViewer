package org.nitri.opentopo.view

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import org.nitri.opentopo.OpenTopoMapSourceMigration
import org.nitri.opentopo.R

class OpenTopoMapMigrationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setMessage(R.string.otm_source_migration_notice)
            .setPositiveButton(android.R.string.ok) { _, _ -> acknowledge() }
            .create()

    override fun onCancel(dialog: DialogInterface) {
        acknowledge()
        super.onCancel(dialog)
    }

    // onDismiss also runs during view destruction; only user dismissal acknowledges.
    private fun acknowledge() {
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
            putBoolean(OpenTopoMapSourceMigration.NOTICE_PENDING, false)
        }
    }
}
