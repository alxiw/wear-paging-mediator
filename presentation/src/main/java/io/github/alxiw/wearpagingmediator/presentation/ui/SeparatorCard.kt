package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.tooling.preview.devices.WearDevices

@Composable
fun SeparatorCard(description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = description,
                color = MaterialTheme.colorScheme.secondaryContainer,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun SeparatorCardPreview() {
    MaterialTheme {
        AppScaffold {
            ScreenScaffold {
                TransformingLazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { Spacer(modifier = Modifier.height(65.dp)) }
                    item { SeparatorCard(description = "10K+ stars") }
                }
            }
        }
    }
}
