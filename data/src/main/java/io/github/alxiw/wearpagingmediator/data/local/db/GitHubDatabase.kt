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
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryResultEntity
import io.github.alxiw.wearpagingmediator.data.local.model.TopicEntity

@Database(
    entities = [
        RepoEntity::class,
        RepoRemoteKeyEntity::class,
        TopicEntity::class,
        RepoTopicCrossRef::class,
        SearchQueryResultEntity::class
    ],
    version = 4,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remote keys now belong to a search query, old rows have no query to migrate to,
                // so they are recreated empty and will be filled on the next refresh.
                db.execSQL("DROP TABLE IF EXISTS `repo_remote_keys`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `repo_remote_keys` (`repoId` INTEGER NOT NULL, `query` TEXT NOT NULL, `prevKey` INTEGER, `nextKey` INTEGER, PRIMARY KEY(`repoId`, `query`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `search_query_results` (`query` TEXT NOT NULL, `repoId` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`query`, `repoId`), FOREIGN KEY(`repoId`) REFERENCES `repos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_query_results_repoId` ON `search_query_results` (`repoId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Queries are canonical now (lower case, words separated by a space instead of an
                // underscore), so every cached query is keyed differently and would never be read
                // again. The per-query cache is dropped and is filled on the next refresh; `repos`
                // is shared between queries and stays.
                db.execSQL("DELETE FROM `search_query_results`")
                db.execSQL("DELETE FROM `repo_remote_keys`")
            }
        }
    }
}
