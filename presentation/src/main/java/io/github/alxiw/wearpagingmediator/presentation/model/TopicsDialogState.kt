package io.github.alxiw.wearpagingmediator.presentation.model

/**
 * The dialog with the topics of one repo. The name, the description and the counts of the repo are
 * kept by the state, so the dialog does not depend on the paging item it was opened from: the item
 * can be dropped from the window while the dialog is open.
 */
sealed interface TopicsDialogState {

    val repoId: Long
    val repoName: String
    val repoDescription: String?
    val repoStars: Int
    val repoForks: Int

    /** The dialog is open, the topics are being read. */
    data class Loading(
        override val repoId: Long,
        override val repoName: String,
        override val repoDescription: String?,
        override val repoStars: Int,
        override val repoForks: Int
    ) : TopicsDialogState

    data class Loaded(
        override val repoId: Long,
        override val repoName: String,
        override val repoDescription: String?,
        override val repoStars: Int,
        override val repoForks: Int,
        val topics: List<String>
    ) : TopicsDialogState

    data class Failed(
        override val repoId: Long,
        override val repoName: String,
        override val repoDescription: String?,
        override val repoStars: Int,
        override val repoForks: Int
    ) : TopicsDialogState
}
