package org.nitri.opentopo.diagnostics

import android.content.res.Resources
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Scoped to one outgoing input hierarchy; never inspects or logs input contents. */
class FocusBoundary(private val name: String) {
    private var clearComposeFocus: (() -> Unit)? = null
    private var root: View? = null

    @Composable
    fun Bind() {
        val focusManager = LocalFocusManager.current
        val view = LocalView.current
        val owner = LocalLifecycleOwner.current
        DisposableEffect(focusManager, view, owner) {
            root = view
            clearComposeFocus = { focusManager.clearFocus(force = true) }
            recordFocus("$name.open", view)
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) clear("pause")
            }
            owner.lifecycle.addObserver(observer)
            onDispose {
                clear("dispose")
                owner.lifecycle.removeObserver(observer)
                clearComposeFocus = null
                root = null
            }
        }
    }

    fun clear(reason: String) {
        recordFocus("$name.$reason", root)
        clearComposeFocus?.invoke()
        root?.clearFocus()
    }
}

fun recordFocus(event: String, root: View?) {
    fun describe(view: View?): String {
        if (view == null) return "none"
        val id = if (view.id == View.NO_ID) "none" else try {
            view.resources.getResourceName(view.id)
        } catch (_: Resources.NotFoundException) {
            "0x${view.id.toString(16)}"
        }
        return "${view.javaClass.simpleName} id=$id attached=${view.isAttachedToWindow}"
    }
    FocusDiagnostics.log("$event root=[${describe(root)}] focus=[${describe(root?.findFocus())}]")
}

/** Intercept removal itself: OnDismiss/OnCancel listeners can run after window removal. */
class FocusAlertDialog(
    context: android.content.Context,
    private val boundary: FocusBoundary,
    themeResId: Int = 0
) : androidx.appcompat.app.AlertDialog(context, themeResId) {
    override fun dismiss() {
        boundary.clear("dismiss")
        super.dismiss()
    }

    override fun hide() {
        boundary.clear("hide")
        super.hide()
    }
}

class FocusDialog(context: android.content.Context, private val boundary: FocusBoundary) :
    android.app.Dialog(context) {
    override fun dismiss() {
        boundary.clear("dismiss")
        super.dismiss()
    }

    override fun hide() {
        boundary.clear("hide")
        super.hide()
    }
}
