package com.sovereign.data.repository

import com.sovereign.data.db.CaptureEntity
import com.sovereign.data.db.CaptureDao
import com.sovereign.data.db.LanguagePackEntity
import com.sovereign.data.db.LanguagePackDao
import com.sovereign.data.db.ModelConfigEntity
import com.sovereign.data.db.ModelConfigDao
import com.sovereign.data.db.SettingEntity
import com.sovereign.data.db.SettingDao
import com.sovereign.data.security.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import java.util.List as JList

class CaptureRepository(private val dao: CaptureDao, private val vault: VaultManager) {
    suspend fun insert(capture: CaptureEntity): Long = dao.insert(capture)

    suspend fun insertAll(captures: JList<CaptureEntity>) = dao.insertAll(captures)

    suspend fun update(capture: CaptureEntity): Int = dao.update(capture)

    suspend fun delete(capture: CaptureEntity) = dao.delete(capture)

    suspend fun deleteById(id: Long): Int = dao.deleteById(id)

    suspend fun getById(id: Long): CaptureEntity? = dao.getById(id)

    suspend fun getAll(limit: Int = 50, offset: Int = 0): JList<CaptureEntity> = dao.getAll(limit, offset)

    fun getAllFlow(): Flow<JList<CaptureEntity>> = dao.getAllFlow()

    fun getByTypeFlow(type: String): Flow<JList<CaptureEntity>> = dao.getByTypeFlow(type)

    suspend fun search(query: String): JList<CaptureEntity> = dao.search("%$query%")

    suspend fun count(): Int = dao.count()

    suspend fun deleteOlderThan(before: Long): Int = dao.deleteOlderThan(before)

    suspend fun insertEncrypted(content: String, type: String, language: String = "en"): Long {
        val encryptedContent = vault.encryptString(content)
        val entity = CaptureEntity(
            content = encryptedContent,
            type = type,
            language = language,
            isEncrypted = true
        )
        return dao.insert(entity)
    }

    fun decryptContent(entity: CaptureEntity): String {
        return if (entity.isEncrypted) {
            vault.decryptString(entity.content)
        } else {
            entity.content
        }
    }
}

class LanguagePackRepository(private val dao: LanguagePackDao) {
    suspend fun insert(pack: LanguagePackEntity) = dao.insert(pack)

    suspend fun insertAll(packs: JList<LanguagePackEntity>) = dao.insertAll(packs)

    suspend fun getByCode(code: String): LanguagePackEntity? = dao.getByCode(code)

    suspend fun getAll(): JList<LanguagePackEntity> = dao.getAll()

    fun getByCodeFlow(code: String): Flow<LanguagePackEntity?> = dao.getByCodeFlow(code)

    suspend fun delete(code: String): Int = dao.delete(code)

    fun getAllFlow(): Flow<JList<LanguagePackEntity>> = kotlinx.coroutines.flow.flow { emit(dao.getAll()) }
}

class SettingRepository(private val dao: SettingDao) {
    suspend fun set(key: String, value: String) {
        dao.insert(SettingEntity(key, value))
    }

    suspend fun get(key: String): String? = dao.getValue(key)

    suspend fun getOrDefault(key: String, default: String): String = dao.getValue(key) ?: default

    suspend fun getAll(): JList<SettingEntity> = dao.getAll()

    suspend fun delete(key: String): Int = dao.deleteByKey(key)

    suspend fun getBoolean(key: String, default: Boolean = false): Boolean =
        dao.getValue(key)?.toBoolean() ?: default

    suspend fun getInt(key: String, default: Int = 0): Int =
        dao.getValue(key)?.toIntOrNull() ?: default

    suspend fun getLong(key: String, default: Long = 0L): Long =
        dao.getValue(key)?.toLongOrNull() ?: default
}

class ModelConfigRepository(private val dao: ModelConfigDao) {
    suspend fun insert(config: ModelConfigEntity) = dao.insert(config)

    suspend fun insertAll(configs: JList<ModelConfigEntity>) = dao.insertAll(configs)

    suspend fun getById(id: String): ModelConfigEntity? = dao.getById(id)

    suspend fun getAll(): JList<ModelConfigEntity> = dao.getAll()

    suspend fun getByStatus(status: String): JList<ModelConfigEntity> = dao.getByStatus(status)

    fun getByIdFlow(id: String): Flow<ModelConfigEntity?> = dao.getByIdFlow(id)

    suspend fun update(config: ModelConfigEntity): Int = dao.update(config)

    suspend fun delete(config: ModelConfigEntity) = dao.delete(config)

    suspend fun updateStatus(id: String, status: String, localPath: String = "") {
        dao.getById(id)?.let { config ->
            val updated = config.copy(status = status, localPath = localPath)
            dao.update(updated)
        }
    }
}