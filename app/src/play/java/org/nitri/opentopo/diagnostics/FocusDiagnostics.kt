package org.nitri.opentopo.diagnostics

import com.google.firebase.crashlytics.FirebaseCrashlytics

internal object FocusDiagnostics {
    fun log(message: String) = FirebaseCrashlytics.getInstance().log(message)
}
