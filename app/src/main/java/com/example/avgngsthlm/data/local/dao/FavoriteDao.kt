package com.example.avgngsthlm.data.local.dao

import androidx.room.*
import com.example.avgngsthlm.data.local.entity.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY name ASC")
    fun getAll(): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun getById(id: Int): Favorite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: Favorite): Long

    @Delete
    suspend fun delete(favorite: Favorite)

    @Update
    suspend fun update(favorite: Favorite)
}
