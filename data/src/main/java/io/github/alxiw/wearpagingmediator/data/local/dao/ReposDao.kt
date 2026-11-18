package io.github.alxiw.wearpagingmediator.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity

@Dao
internal interface ReposDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repos: List<RepoEntity>)

    @Query("SELECT * FROM repos WHERE " +
            "name LIKE :query OR description LIKE :query " +
            "ORDER BY stars DESC, name ASC")
    fun getRepos(query: String): PagingSource<Int, RepoEntity>

    @Query("DELETE FROM repos")
    suspend fun deleteAll()
}
