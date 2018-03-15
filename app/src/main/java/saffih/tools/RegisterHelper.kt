package saffih.tools

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 3/08/17.
 */
private class Memoize1<in T, out R>(val f: (T) -> R) : (T) -> R {
    private val values = mutableMapOf<T, R>()
    override fun invoke(x: T): R {
        return values.getOrPut(x, { f(x) })
    }
}

fun <T, R> ((T) -> R).memoize(): (T) -> R = Memoize1(this)

//// todo should use list of all perms
fun Activity.activityCheckForPermission(perm: List<String>, code: Int): Boolean {
    val me = this
    val missing = perm.filter { ContextCompat.checkSelfPermission(me, it) != PackageManager.PERMISSION_GRANTED }
    if (missing.isEmpty()) return true

//    me.requestPermissions(missing.toTypedArray(), code)
    val recheck = missing.map { ContextCompat.checkSelfPermission(me, it) != PackageManager.PERMISSION_GRANTED }.all { it == true }
    return recheck

}

fun Context.permissionGranted(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

fun Context.permissionsMissing(perms: List<String>) =
        perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }


abstract class RegisterHelper(val context: Context, val retryInterval: Long = 1000) {

    private val registerHook = { register() }


    abstract val requiredPermissions: List<String>

    fun register() {
        if (context.permissionsMissing(requiredPermissions).isEmpty()) {
            onRegister()
        } else {
            context.postDelayed(registerHook, retryInterval)
        }
    }

    protected abstract fun onRegister()

    fun unregister() {
        context.removeCallbacks(registerHook)
        onUnregister()
    }

    protected abstract fun onUnregister()
}
