package com.insnaejack.pdfgenerator.di

import android.content.Context
import com.insnaejack.pdfgenerator.google.GoogleDriveService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GoogleModule {

    @Provides
    @Singleton
    fun provideGoogleDriveService(@ApplicationContext context: Context): GoogleDriveService {
        return GoogleDriveService(context)
    }
}