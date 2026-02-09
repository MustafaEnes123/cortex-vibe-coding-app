package com.enesy.bookmarker

import android.app.Application
import com.enesy.bookmarker.data.AppDatabase
import com.enesy.bookmarker.data.AuthRepository
import com.enesy.bookmarker.data.SyncRepository
import com.enesy.bookmarker.data.ThemeRepository

class BookmarkerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val themeRepository: ThemeRepository by lazy { ThemeRepository(this) }
    val authRepository: AuthRepository by lazy { AuthRepository() }
    val syncRepository: SyncRepository by lazy { SyncRepository(database.bookmarkerDao()) }
}