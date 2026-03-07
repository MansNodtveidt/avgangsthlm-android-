package com.example.avgngsthlm.data.local.dao

import androidx.room.*
import com.example.avgngsthlm.data.local.entity.AutoRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoRuleDao {
    @Query("SELECT * FROM auto_rules ORDER BY id ASC")
    fun getAll(): Flow<List<AutoRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AutoRule): Long

    @Delete
    suspend fun delete(rule: AutoRule)

    @Update
    suspend fun update(rule: AutoRule)

    @Query("DELETE FROM auto_rules WHERE favoriteId = :favoriteId")
    suspend fun deleteByFavoriteId(favoriteId: Int)
}
