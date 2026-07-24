package com.ledgerlite.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.domain.model.BigItemStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BigItemDao {

    @Query("SELECT * FROM big_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BigItem>>

    @Query("SELECT * FROM big_items WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: BigItemStatus): Flow<List<BigItem>>

    @Query("SELECT * FROM big_items WHERE id = :id")
    suspend fun getById(id: Long): BigItem?

    @Query("SELECT * FROM big_items WHERE id = :id")
    fun observeById(id: Long): Flow<BigItem?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: BigItem): Long

    @Update
    suspend fun update(item: BigItem)

    @Query("UPDATE big_items SET status = :status, endedAt = :endedAt, updatedAt = :now WHERE id = :id")
    suspend fun endItem(id: Long, status: BigItemStatus, endedAt: Long, now: Long)
}
