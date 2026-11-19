package io.github.alxiw.wearpagingmediator.presentation.ui

import android.app.Activity
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContract
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender

/** The voice panel the search button opens: the reply of the user is the query of the results. */
class WearSearchContract : ActivityResultContract<String, String?>() {
    override fun createIntent(context: Context, input: String): Intent {
        val remoteInput = RemoteInput.Builder(EXTRA_VOICE_REPLY)
            .setLabel(input)
            .wearableExtender {
                setEmojisAllowed(false)
                setInputActionType(EditorInfo.IME_ACTION_SEARCH)
            }
            .build()
        return RemoteInputIntentHelper.createActionRemoteInputIntent().also {
            RemoteInputIntentHelper.putRemoteInputsExtra(it, listOf(remoteInput))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return RemoteInput.getResultsFromIntent(intent)?.getString(EXTRA_VOICE_REPLY)
    }
}

private const val EXTRA_VOICE_REPLY = "extra_voice_reply"
