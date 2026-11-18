package io.github.alxiw.wearpagingmediator.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.alxiw.wearpagingmediator.data.local.dao.RemoteKeysDao
import io.github.alxiw.wearpagingmediator.data.local.dao.ReposDao
import io.github.alxiw.wearpagingmediator.data.local.model.RepoRemoteKeyEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity

@Database(
    entities = [RepoEntity::class, RepoRemoteKeyEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class GitHubDatabase : RoomDatabase() {

    abstract fun reposDao(): ReposDao
    abstract fun remoteKeysDao(): RemoteKeysDao

    companion object {
        const val DB_NAME = "wearpagingmediator.db"
    }
}
