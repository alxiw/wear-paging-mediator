package io.github.alxiw.wearpagingmediator.presentation.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold

/**
 * The scaffold a single card is previewed in: a card is only shown the way the list shows it, with
 * the background and the padding of the screen, so a preview cannot disagree with the real screen.
 */
@Composable
internal fun WearCardPreview(content: @Composable () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    MaterialTheme {
        AppScaffold {
            ScreenScaffold(scrollState = listState) { contentPadding ->
                TransformingLazyColumn(
                    state = listState,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { content() }
                }
            }
        }
    }
}
