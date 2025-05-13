package com.insnaejack.pdfgenerator.di

import android.content.Context
import com.insnaejack.pdfgenerator.billing.BillingClientWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun provideBillingClientWrapper(@ApplicationContext context: Context): BillingClientWrapper {
        return BillingClientWrapper(context)
    }
}
