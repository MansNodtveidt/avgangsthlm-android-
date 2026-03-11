package com.example.avgngsthlm.data.repository

import com.example.avgngsthlm.data.local.dao.FavoriteDao
import com.example.avgngsthlm.data.local.entity.Favorite
import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val dao: FavoriteDao) {

    val allFavorites: Flow<List<Favorite>> = dao.getAll()

    suspend fun getById(id: Int): Favorite? = dao.getById(id)

    suspend fun insert(favorite: Favorite): Long = dao.insert(favorite)

    suspend fun delete(favorite: Favorite) = dao.delete(favorite)

    suspend fun update(favorite: Favorite) = dao.update(favorite)

    suspend fun getCount(): Int = dao.getCount()

    suspend fun updateSlSiteId(id: Int, slSiteId: String) = dao.updateSlSiteId(id, slSiteId)

    suspend fun resetAllSlSiteIds() = dao.resetAllSlSiteIds()
}
