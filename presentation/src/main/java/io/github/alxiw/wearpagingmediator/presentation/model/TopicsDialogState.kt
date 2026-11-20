package io.github.alxiw.wearpagingmediator.presentation.model

/**
 * The dialog with the topics of one repo. The name and the description of the repo are kept by the
 * state, so the dialog does not depend on the paging item it was opened from: the item can be
 * dropped from the window while the dialog is open.
 */
sealed interface TopicsDialogState {

    val repoId: Long
    val repoName: String
    val repoDescription: String?

    /** The dialog is open, the topics are being read. */
    data class Loading(
        override val repoId: Long,
        override val repoName: String,
        override val repoDescription: String?
    ) : TopicsDialogState

    data class Loaded(
        override val repoId: Long,
        override val repoName: String,
        override val repoDescription: String?,
        val topics: List<String>
    ) : TopicsDialogState

    data class Failed(
        override val repoId: Long,
        override val repoName: String,
        override val repoDescription: String?
    ) : TopicsDialogState
}
