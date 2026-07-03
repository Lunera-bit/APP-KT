package com.cyryel.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items")
    suspend fun getAll(): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CartItemEntity>)

    @Query("DELETE FROM cart_items WHERE compositeId = :compositeId")
    suspend fun deleteById(compositeId: String)

    @Query("DELETE FROM cart_items")
    suspend fun deleteAll()
}
