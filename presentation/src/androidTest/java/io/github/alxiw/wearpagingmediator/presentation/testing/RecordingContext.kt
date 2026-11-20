package io.github.alxiw.wearpagingmediator.presentation.testing

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

/**
 * A context that records the intents the screen starts instead of leaving the app.
 *
 * [failure] makes every start fail the way a denied or an unsupported one does, so a test can
 * check that the screen survives a link it cannot open.
 */
internal class RecordingContext(
    base: Context,
    private val failure: RuntimeException? = null
) : ContextWrapper(base) {

    /** The intents the screen tried to start, in the order it tried them. */
    val startedIntents = mutableListOf<Intent>()

    override fun startActivity(intent: Intent) {
        startedIntents += intent
        failure?.let { throw it }
    }
}
