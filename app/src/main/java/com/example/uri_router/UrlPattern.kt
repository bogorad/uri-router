package com.example.uri_router

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "url_patterns",
    indices = [Index(value = ["pattern"], unique = true)]
)
data class UrlPattern(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pattern: String
)

