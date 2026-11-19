package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import io.github.alxiw.wearpagingmediator.presentation.R

/**
 * A failed load with a way to retry it, shown instead of the missing results and in the footer.
 */
@Composable
fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(65.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onRetry),
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.retry),
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun ErrorCardPreview() {
    MaterialTheme {
        AppScaffold {
            ScreenScaffold {
                TransformingLazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { Spacer(modifier = Modifier.height(60.dp)) }
                    item { ErrorCard(message = stringResource(R.string.error_refresh), onRetry = {}) }
                }
            }
        }
    }
}
