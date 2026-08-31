package com.salespro.core.di

import android.content.Context
import com.salespro.data.AppDatabase
import com.salespro.data.SalesDao
import com.salespro.data.SalesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideSalesDao(database: AppDatabase): SalesDao {
        return database.salesDao()
    }

    @Provides
    fun provideBusinessProfileDao(database: AppDatabase): com.salespro.data.BusinessProfileDao {
        return database.businessProfileDao()
    }

    @Provides
    @Singleton
    fun provideSalesRepository(salesDao: SalesDao, profileDao: com.salespro.data.BusinessProfileDao): SalesRepository {
        return SalesRepository(salesDao, profileDao)
    }

    @Provides
    @Singleton
    fun provideReceiptShareManager(): com.salespro.utils.ReceiptShareManager {
        return com.salespro.utils.DefaultReceiptShareManager()
    }
}
