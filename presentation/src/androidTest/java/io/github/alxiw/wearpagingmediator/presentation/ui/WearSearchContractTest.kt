package io.github.alxiw.wearpagingmediator.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.input.RemoteInputIntentHelper
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.testing.VOICE_REPLY_KEY
import io.github.alxiw.wearpagingmediator.presentation.testing.intentWithReply
import io.github.alxiw.wearpagingmediator.presentation.testing.label
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the contract the search of the screen is launched with: the intent asks the watch for a
 * remote input, and only a reply of that input is taken as a query.
 */
@RunWith(AndroidJUnit4::class)
class WearSearchContractTest {

    private val contract = WearSearchContract()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun intentAsksTheWatchForARemoteInput() {
        val input = label(R.string.search_label)

        val intent = contract.createIntent(context, input)

        assertTrue(RemoteInputIntentHelper.isActionRemoteInput(intent))
        val remoteInput = RemoteInputIntentHelper.getRemoteInputsExtra(intent)?.single()
        assertNotNull(remoteInput)
        assertEquals(VOICE_REPLY_KEY, remoteInput?.resultKey)
        assertEquals(input, remoteInput?.label?.toString())
    }

    @Test
    fun replyOfTheRemoteInputIsTheQuery() {
        val intent = intentWithReply("kotlin")

        assertEquals("kotlin", contract.parseResult(Activity.RESULT_OK, intent))
    }

    @Test
    fun cancelledInputHasNoQuery() {
        assertNull(contract.parseResult(Activity.RESULT_CANCELED, intentWithReply("kotlin")))
    }

    @Test
    fun resultWithoutTheRemoteInputHasNoQuery() {
        assertNull(contract.parseResult(Activity.RESULT_OK, Intent()))
    }

    @Test
    fun missingIntentHasNoQuery() {
        assertNull(contract.parseResult(Activity.RESULT_OK, null))
    }
}
