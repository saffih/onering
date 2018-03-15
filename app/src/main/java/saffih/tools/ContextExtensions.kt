package saffih.tools

import android.content.Context
import android.os.Handler
import android.widget.Toast

/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 3/08/17.
 */

fun Context.post(posted: () -> Unit): Boolean {
    return Handler(mainLooper).post(posted)
}

fun Context.toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
    val h = Handler(mainLooper)
    h.post({ Toast.makeText(this, txt, duration).show() })
}

fun Context.postDelayed(r: () -> Unit, delayMillis: Long) = Handler(this.mainLooper).postDelayed(r, delayMillis)


fun Context.removeCallbacks(r: () -> Unit) = Handler(this.mainLooper).removeCallbacks(r)

