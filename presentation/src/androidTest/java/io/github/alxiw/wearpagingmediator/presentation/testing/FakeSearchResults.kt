package io.github.alxiw.wearpagingmediator.presentation.testing

import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

/** The key the search contract reads the reply of the voice input from, see `WearSearchContract`. */
internal const val VOICE_REPLY_KEY = "extra_voice_reply"

/**
 * Answers the search request of the screen instead of showing the system input of the watch.
 *
 * The reply is delivered through the remote input of the contract, so the screen parses the reply
 * exactly the way it parses the one of a real watch input.
 */
internal class FakeSearchResults(
    private val reply: String? = null,
    private val resultCode: Int = if (reply == null) Activity.RESULT_CANCELED else Activity.RESULT_OK
) : ActivityResultRegistryOwner {

    /** The inputs the screen launched the search with, e.g. the label of the search. */
    val inputs = mutableListOf<Any?>()

    override val activityResultRegistry: ActivityResultRegistry =
        object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                inputs += input
                dispatchResult(requestCode, resultCode, intentWithReply(reply))
            }
        }
}

/** The intent a watch delivers when the user is done with the voice input. */
@Suppress("DEPRECATION")
internal fun intentWithReply(reply: String?): Intent {
    val intent = Intent()
    if (reply != null) {
        val remoteInput = RemoteInput.Builder(VOICE_REPLY_KEY).build()
        val results = Bundle().apply { putCharSequence(VOICE_REPLY_KEY, reply) }
        RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, results)
    }
    return intent
}
