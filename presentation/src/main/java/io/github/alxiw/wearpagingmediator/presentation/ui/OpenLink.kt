package io.github.alxiw.wearpagingmediator.presentation.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri

private const val TAG = "OpenLink"

/**
 * Opens [url] outside of the app.
 *
 * A watch has no browser of its own: when nothing can show the link, the system may answer the
 * intent with the redirect activity of the Wear OS app, which only the system may start, or with
 * nothing at all. Both cases fail only on the way out, so the screen must never rely on the link
 * being opened.
 *
 * @return whether [url] was handed over to an app.
 */
internal fun Context.openLink(url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No app can open $url", e)
        false
    } catch (e: SecurityException) {
        // the Wear OS app redirects a link to the paired phone with an activity of its own, and
        // only the apps that hold the permission of that activity may start it: a third-party app
        // is denied whenever there is no browser to open the link instead
        Log.w(TAG, "No app is allowed to open $url", e)
        false
    }
}
