package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.model.ListFooter
import io.github.alxiw.wearpagingmediator.presentation.model.ListNotice
import io.github.alxiw.wearpagingmediator.presentation.ui.preview.WearCardPreview
import io.github.alxiw.wearpagingmediator.presentation.ui.preview.WearPreview

/**
 * The head of the list: what is shown above the results while the current query has none yet.
 * The text and the retry of the state are resolved once, here, so the list only places the item.
 */
@Composable
internal fun ListNoticeItem(notice: ListNotice) {
    when (notice) {
        ListNotice.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        ListNotice.Empty -> MessageCard(text = stringResource(R.string.empty))
    }
}

/** The tail of the list: the state of the load right after the last item of the query. */
@Composable
internal fun ListFooterItem(footer: ListFooter, onRetry: () -> Unit) {
    when (footer) {
        ListFooter.RefreshError -> ErrorCard(
            message = stringResource(R.string.error_refresh),
            onRetry = onRetry
        )
        ListFooter.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        ListFooter.AppendError -> ErrorCard(
            message = stringResource(R.string.error_append),
            onRetry = onRetry
        )
        ListFooter.EndOfList -> MessageCard(text = stringResource(R.string.end_of_list))
    }
}

@WearPreview
@Composable
private fun ListNoticeLoadingPreview() {
    WearCardPreview { ListNoticeItem(ListNotice.Loading) }
}

@WearPreview
@Composable
private fun ListNoticeEmptyPreview() {
    WearCardPreview { ListNoticeItem(ListNotice.Empty) }
}

@WearPreview
@Composable
private fun ListFooterLoadingPreview() {
    WearCardPreview { ListFooterItem(ListFooter.Loading, onRetry = {}) }
}

@WearPreview
@Composable
private fun ListFooterRefreshErrorPreview() {
    WearCardPreview { ListFooterItem(ListFooter.RefreshError, onRetry = {}) }
}

@WearPreview
@Composable
private fun ListFooterAppendErrorPreview() {
    WearCardPreview { ListFooterItem(ListFooter.AppendError, onRetry = {}) }
}

@WearPreview
@Composable
private fun ListFooterEndOfListPreview() {
    WearCardPreview { ListFooterItem(ListFooter.EndOfList, onRetry = {}) }
}
