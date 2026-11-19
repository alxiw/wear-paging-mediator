package io.github.alxiw.wearpagingmediator.presentation.testing

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

/** A context that records the intents the screen starts instead of leaving the app. */
internal class RecordingContext(base: Context) : ContextWrapper(base) {

    /** The intents the screen started, in the order they were started. */
    val startedIntents = mutableListOf<Intent>()

    override fun startActivity(intent: Intent) {
        startedIntents += intent
    }
}
