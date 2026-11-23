package io.github.alxiw.wearpagingmediator.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.alxiw.wearpagingmediator.data.local.dao.RemoteKeysDao
import io.github.alxiw.wearpagingmediator.data.local.dao.ReposDao
import io.github.alxiw.wearpagingmediator.data.local.dao.SearchQueriesDao
import io.github.alxiw.wearpagingmediator.data.local.model.RepoRemoteKeyEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoTopicCrossRef
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryEntity
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryResultEntity

@Database(
    entities = [
        RepoEntity::class,
        RepoRemoteKeyEntity::class,
        RepoTopicCrossRef::class,
        SearchQueryResultEntity::class,
        SearchQueryEntity::class
    ],
    version = 6,
    exportSchema = true
)
internal abstract class GitHubDatabase : RoomDatabase() {

    abstract fun reposDao(): ReposDao
    abstract fun remoteKeysDao(): RemoteKeysDao
    abstract fun searchQueriesDao(): SearchQueriesDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // columns follow the snake_case naming of the tables
                db.execSQL("ALTER TABLE `search_query_results` RENAME COLUMN `repoId` TO `repo_id`")
                db.execSQL("ALTER TABLE `repo_remote_keys` RENAME COLUMN `repoId` TO `repo_id`")
                db.execSQL("ALTER TABLE `repo_remote_keys` RENAME COLUMN `prevKey` TO `prev_key`")
                db.execSQL("ALTER TABLE `repo_remote_keys` RENAME COLUMN `nextKey` TO `next_key`")
                db.execSQL("ALTER TABLE `repo_topic_cross_ref` RENAME COLUMN `id` TO `repo_id`")
                // renaming a column does not rename its index, but Room derives the name from it
                db.execSQL("DROP INDEX IF EXISTS `index_search_query_results_repoId`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_query_results_repo_id` ON `search_query_results` (`repo_id`)")

                // the cache is bounded now: the opened queries are registered to find the ones to evict
                db.execSQL("CREATE TABLE IF NOT EXISTS `search_queries` (`query` TEXT NOT NULL, `accessed_at` INTEGER NOT NULL, PRIMARY KEY(`query`))")
                // The moment a query was used is not known for the cached ones, so they are all
                // equally old and the least recently used ones are picked by name.
                db.execSQL("""
                    INSERT OR IGNORE INTO `search_queries` (`query`, `accessed_at`)
                    SELECT `query`, 0 FROM `search_query_results`
                    UNION
                    SELECT `query`, 0 FROM `repo_remote_keys`
                """)
                // 5 is the limit of the version this migration produces: a later version may keep
                // more queries, but this migration must keep using its own limit.
                db.execSQL("""
                    CREATE TEMP TABLE `evicted_queries` AS
                    SELECT `query` FROM `search_queries`
                    ORDER BY `accessed_at` DESC, `query` DESC
                    LIMIT -1 OFFSET 5
                """)
                db.execSQL("DELETE FROM `search_query_results` WHERE `query` IN (SELECT `query` FROM `evicted_queries`)")
                db.execSQL("DELETE FROM `repo_remote_keys` WHERE `query` IN (SELECT `query` FROM `evicted_queries`)")
                db.execSQL("DELETE FROM `search_queries` WHERE `query` IN (SELECT `query` FROM `evicted_queries`)")
                db.execSQL("DROP TABLE `evicted_queries`")

                // write-only: the topics of a repo are read from `repo_topic_cross_ref`
                db.execSQL("DROP TABLE IF EXISTS `topics`")

                // Only rows without any reference are removed: `search_query_results` cascades from
                // `repos`, so a repository still referenced by another query would take that
                // query's results with it.
                db.execSQL("""
                    DELETE FROM `repos`
                    WHERE NOT EXISTS (SELECT 1 FROM `search_query_results` WHERE `search_query_results`.`repo_id` = `repos`.`id`)
                    AND NOT EXISTS (SELECT 1 FROM `repo_remote_keys` WHERE `repo_remote_keys`.`repo_id` = `repos`.`id`)
                """)
                db.execSQL("""
                    DELETE FROM `repo_topic_cross_ref`
                    WHERE NOT EXISTS (SELECT 1 FROM `repos` WHERE `repos`.`id` = `repo_topic_cross_ref`.`repo_id`)
                """)
            }
        }

        /**
         * A refresh of version 5 could replace a deep page and leave only that slice in
         * `search_query_results`: its first position is no longer 0, which makes a cold start open
         * the list in the middle of the search. The initial load was also two pages, so a stored
         * position could disagree with the page of a remote key.
         *
         * The cache is kept only where it is still readable from its first result: the positions of
         * a query must be a contiguous prefix starting at 0. Any other query is refreshed from the
         * network the next time it is opened. The limit of the search result offset is the one of
         * version 6, a later version may serve more results without changing this migration.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TEMP TABLE `truncated_queries` AS
                    SELECT `query` FROM `search_query_results`
                    GROUP BY `query`
                    HAVING MIN(`position`) <> 0
                        OR COUNT(*) <> MAX(`position`) - MIN(`position`) + 1
                """)
                db.execSQL("DELETE FROM `repo_remote_keys` WHERE `query` IN (SELECT `query` FROM `truncated_queries`)")
                db.execSQL("DELETE FROM `search_query_results` WHERE `query` IN (SELECT `query` FROM `truncated_queries`)")
                db.execSQL("DROP TABLE `truncated_queries`")

                // A key of a result beyond the offset GitHub serves would still point to a page
                // that always fails with HTTP 422.
                db.execSQL("""
                    DELETE FROM `repo_remote_keys`
                    WHERE EXISTS (
                        SELECT 1 FROM `search_query_results`
                        WHERE `search_query_results`.`query` = `repo_remote_keys`.`query`
                        AND `search_query_results`.`repo_id` = `repo_remote_keys`.`repo_id`
                        AND `search_query_results`.`position` >= 1000
                    )
                """)
                db.execSQL("DELETE FROM `search_query_results` WHERE `position` >= 1000")

                // A page can be cached as a key without its result, e.g. when a cache was cleared
                // page by page. Such a key has no page position to restore from.
                db.execSQL("""
                    DELETE FROM `repo_remote_keys`
                    WHERE NOT EXISTS (
                        SELECT 1 FROM `search_query_results`
                        WHERE `search_query_results`.`query` = `repo_remote_keys`.`query`
                        AND `search_query_results`.`repo_id` = `repo_remote_keys`.`repo_id`
                    )
                """)

                // Only rows without any reference are removed: `search_query_results` cascades from
                // `repos`, so a repository still referenced by another query would take that
                // query's results with it.
                db.execSQL("""
                    DELETE FROM `repos`
                    WHERE NOT EXISTS (SELECT 1 FROM `search_query_results` WHERE `search_query_results`.`repo_id` = `repos`.`id`)
                    AND NOT EXISTS (SELECT 1 FROM `repo_remote_keys` WHERE `repo_remote_keys`.`repo_id` = `repos`.`id`)
                """)
                db.execSQL("""
                    DELETE FROM `repo_topic_cross_ref`
                    WHERE NOT EXISTS (SELECT 1 FROM `repos` WHERE `repos`.`id` = `repo_topic_cross_ref`.`repo_id`)
                """)
            }
        }
    }
}
