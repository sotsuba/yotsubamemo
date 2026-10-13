package com.example.assignment03.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "journal_entries", indices = [Index(value = ["created_at", "id"])])
internal data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?,
    @ColumnInfo(defaultValue = "''") val content: String = "",
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    val mood: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Dao
internal interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY created_at DESC, id DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    fun observe(id: Long): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun get(id: Long): JournalEntryEntity?

    @Query("SELECT photo_path FROM journal_entries WHERE photo_path IS NOT NULL")
    suspend fun photoPaths(): List<String>

    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update
    suspend fun update(entry: JournalEntryEntity): Int

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [JournalEntryEntity::class], version = 1, exportSchema = true)
internal abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}
