package com.example.uri_router

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UrlPatternDao {
    @Query("SELECT * FROM url_patterns ORDER BY pattern ASC")
    fun getAllPatterns(): Flow<List<UrlPattern>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(urlPattern: UrlPattern): Long

    @Delete
    suspend fun delete(urlPattern: UrlPattern)
}

