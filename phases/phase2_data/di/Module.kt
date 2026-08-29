package com.sovereign.di

import android.content.Context
import androidx.room.Room
import com.sovereign.data.db.SovereignDatabase
import com.sovereign.data.repository.CaptureRepository
import com.sovereign.data.repository.LanguagePackRepository
import com.sovereign.data.repository.ModelConfigRepository
import com.sovereign.data.repository.SettingRepository
import com.sovereign.data.security.VaultManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

object AppModule {
    private var database: SovereignDatabase? = null
    private var vaultManager: VaultManager? = null
    private var captureRepository: CaptureRepository? = null
    private var languagePackRepository: LanguagePackRepository? = null
    private var settingRepository: SettingRepository? = null
    private var modelConfigRepository: ModelConfigRepository? = null
    private var scope: CoroutineScope? = null
    private var job: Job? = null

    fun init(context: Context) {
        database = Room.databaseBuilder(
            context.applicationContext,
            SovereignDatabase::class.java,
            "sovereign_db"
        ).fallbackToDestructiveMigration().build()

        vaultManager = VaultManager(context)
        captureRepository = CaptureRepository(database!!.captureDao(), vaultManager!!)
        languagePackRepository = LanguagePackRepository(database!!.languagePackDao())
        settingRepository = SettingRepository(database!!.settingDao())
        modelConfigRepository = ModelConfigRepository(database!!.modelConfigDao())
        job = Job()
        scope = CoroutineScope(Dispatchers.IO + job!!)
    }

    fun getDatabase(): SovereignDatabase = database!!
    fun getVaultManager(): VaultManager = vaultManager!!
    fun getCaptureRepository(): CaptureRepository = captureRepository!!
    fun getLanguagePackRepository(): LanguagePackRepository = languagePackRepository!!
    fun getSettingRepository(): SettingRepository = settingRepository!!
    fun getModelConfigRepository(): ModelConfigRepository = modelConfigRepository!!
    fun getScope(): CoroutineScope = scope!!
    
    fun cleanup() {
        job?.cancel()
        database?.close()
    }
}