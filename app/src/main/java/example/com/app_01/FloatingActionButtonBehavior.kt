package example.com.app_01

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.util.AttributeSet
import android.view.View

typealias T = View

class FloatingActionButtonBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<T>() {

    override fun layoutDependsOn(parent: CoordinatorLayout, child: T, dependency: View): Boolean {

        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: T, dependency: View): Boolean {

        val translationY = Math.min(0.0f, (dependency.translationY - dependency.height).toFloat())
        child.translationY = translationY
        return true
    }
}