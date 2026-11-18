package io.github.alxiw.wearpagingmediator.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.alxiw.wearpagingmediator.data.local.dao.RemoteKeysDao
import io.github.alxiw.wearpagingmediator.data.local.dao.ReposDao
import io.github.alxiw.wearpagingmediator.data.local.model.RepoRemoteKeyEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoTopicCrossRef
import io.github.alxiw.wearpagingmediator.data.local.model.TopicEntity

@Database(
    entities = [
        RepoEntity::class,
        RepoRemoteKeyEntity::class,
        TopicEntity::class,
        RepoTopicCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
internal abstract class GitHubDatabase : RoomDatabase() {

    abstract fun reposDao(): ReposDao
    abstract fun remoteKeysDao(): RemoteKeysDao

    companion object {
        const val DB_NAME = "wearpagingmediator.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `topics` (`topic` TEXT NOT NULL, PRIMARY KEY(`topic`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `repo_topic_cross_ref` (`id` INTEGER NOT NULL, `topic` TEXT NOT NULL, PRIMARY KEY(`id`, `topic`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_repo_topic_cross_ref_topic` ON `repo_topic_cross_ref` (`topic`)")
            }
        }
    }
}
