package com.sovereign.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow
import java.util.List

@Dao
interface CaptureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(capture: CaptureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(captures: List<CaptureEntity>)

    @Update
    suspend fun update(capture: CaptureEntity): Int

    @Delete
    suspend fun delete(capture: CaptureEntity)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun getById(id: Long): CaptureEntity?

    @Query("SELECT * FROM captures ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int, offset: Int): List<CaptureEntity>

    @Query("SELECT * FROM captures ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE type = :type ORDER BY created_at DESC")
    fun getByTypeFlow(type: String): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE content LIKE :query OR tags LIKE :query ORDER BY created_at DESC")
    suspend fun search(query: String): List<CaptureEntity>

    @Query("SELECT COUNT(*) FROM captures")
    suspend fun count(): Int

    @Query("DELETE FROM captures WHERE created_at < :before")
    suspend fun deleteOlderThan(before: Long): Int
}

@Dao
interface LanguagePackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pack: LanguagePackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(packs: List<LanguagePackEntity>)

    @Query("SELECT * FROM language_packs WHERE code = :code")
    suspend fun getByCode(code: String): LanguagePackEntity?

    @Query("SELECT * FROM language_packs ORDER BY downloaded_at DESC")
    suspend fun getAll(): List<LanguagePackEntity>

    @Query("SELECT * FROM language_packs WHERE code = :code")
    fun getByCodeFlow(code: String): Flow<LanguagePackEntity?>

    @Query("DELETE FROM language_packs WHERE code = :code")
    suspend fun delete(code: String): Int
}

@Dao
interface SettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: SettingEntity)

    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun get(key: String): SettingEntity?

    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingEntity>

    @Delete
    suspend fun delete(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteByKey(key: String): Int
}

@Dao
interface ModelConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ModelConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<ModelConfigEntity>)

    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun getById(id: String): ModelConfigEntity?

    @Query("SELECT * FROM model_configs ORDER BY downloaded_at DESC")
    suspend fun getAll(): List<ModelConfigEntity>

    @Query("SELECT * FROM model_configs WHERE status = :status")
    suspend fun getByStatus(status: String): List<ModelConfigEntity>

    @Query("SELECT * FROM model_configs WHERE id = :id")
    fun getByIdFlow(id: String): Flow<ModelConfigEntity?>

    @Update
    suspend fun update(config: ModelConfigEntity): Int

    @Delete
    suspend fun delete(config: ModelConfigEntity)
}