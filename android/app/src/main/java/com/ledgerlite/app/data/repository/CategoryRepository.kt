package com.ledgerlite.app.data.repository

import com.ledgerlite.app.data.local.dao.CategoryDao
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {

    fun observeAll(): Flow<List<Category>> = dao.observeAll()

    fun observeRecent(limit: Int): Flow<List<Category>> = dao.observeRecent(limit)

    suspend fun getById(id: Long): Category? = dao.getById(id)

    suspend fun create(name: String, color: Long, icon: String = ""): Long {
        val now = DateUtil.nowMillis()
        val category = Category(
            name = name,
            icon = icon,
            color = color,
            sortOrder = dao.maxSortOrder() + 1,
            isDefault = false,
            createdAt = now,
            updatedAt = now
        )
        return dao.insert(category)
    }

    suspend fun update(category: Category) {
        dao.update(category.copy(updatedAt = DateUtil.nowMillis()))
    }

    /** 按给定顺序重排：位置即 sortOrder。只更新发生变动的分类。 */
    suspend fun reorder(orderedCategories: List<Category>) {
        val updates = orderedCategories.mapIndexed { index, cat ->
            if (cat.sortOrder == index) null
            else cat.copy(sortOrder = index, updatedAt = DateUtil.nowMillis())
        }.filterNotNull()
        if (updates.isNotEmpty()) dao.updateAll(updates)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
