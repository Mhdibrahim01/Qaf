package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Persists full AI dictionary definitions per word permanently.
 * One request covers all parts of speech of that word and is saved forever.
 */
@Immutable
@Serializable
@Entity(tableName = "word_details")
data class WordDetailEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String, // lowercase word
    val json: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
