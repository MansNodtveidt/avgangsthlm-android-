package com.example.avgngsthlm.data.repository

import com.example.avgngsthlm.data.local.dao.AutoRuleDao
import com.example.avgngsthlm.data.local.entity.AutoRule
import kotlinx.coroutines.flow.Flow

class AutoRuleRepository(private val dao: AutoRuleDao) {

    val allRules: Flow<List<AutoRule>> = dao.getAll()

    suspend fun insert(rule: AutoRule): Long = dao.insert(rule)

    suspend fun delete(rule: AutoRule) = dao.delete(rule)

    suspend fun update(rule: AutoRule) = dao.update(rule)

    suspend fun deleteByFavoriteId(favoriteId: Int) = dao.deleteByFavoriteId(favoriteId)
}
