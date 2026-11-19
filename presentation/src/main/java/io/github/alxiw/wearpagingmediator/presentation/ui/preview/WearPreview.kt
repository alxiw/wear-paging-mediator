package io.github.alxiw.wearpagingmediator.presentation.ui.preview

import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices

/**
 * A preview on the two round watches the results are read on: the same content is checked on a
 * small and a large screen, since the list only fits one card at a time on the small one.
 */
@Preview(
    name = "small round",
    group = "Wear",
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true
)
@Preview(
    name = "large round",
    group = "Wear",
    device = WearDevices.LARGE_ROUND,
    showSystemUi = true
)
@Target(AnnotationTarget.FUNCTION)
annotation class WearPreview
