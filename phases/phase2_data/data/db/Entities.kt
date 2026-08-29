package com.sovereign.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "language") val language: String = "en",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_encrypted") val isEncrypted: Boolean = false,
    @ColumnInfo(name = "tags") val tags: String = "",
    @ColumnInfo(name = "metadata") val metadata: String = ""
)

@Entity(tableName = "language_packs")
data class LanguagePackEntity(
    @PrimaryKey val code: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "native_name") val nativeName: String,
    @ColumnInfo(name = "flag") val flag: String,
    @ColumnInfo(name = "region") val region: String,
    @ColumnInfo(name = "version") val version: Int = 1,
    @ColumnInfo(name = "downloaded_at") val downloadedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "file_path") val filePath: String = "",
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long = 0
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "model_configs")
data class ModelConfigEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "repo_id") val repoId: String,
    @ColumnInfo(name = "filename") val filename: String,
    @ColumnInfo(name = "quantization") val quantization: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "local_path") val localPath: String = "",
    @ColumnInfo(name = "status") val status: String = "pending",
    @ColumnInfo(name = "downloaded_at") val downloadedAt: Long = 0,
    @ColumnInfo(name = "config_json") val configJson: String = ""
)