package com.dotkios.ulaaa.di

import android.content.Context
import androidx.room.Room
import com.dotkios.ulaaa.data.local.UlaaaDatabase
import com.dotkios.ulaaa.data.local.dao.BucketDao
import com.dotkios.ulaaa.data.local.dao.TripDao
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
    fun provideDatabase(@ApplicationContext context: Context): UlaaaDatabase =
        Room.databaseBuilder(context, UlaaaDatabase::class.java, "ulaaa.db").build()

    @Provides
    fun provideTripDao(db: UlaaaDatabase): TripDao = db.tripDao()

    @Provides
    fun provideBucketDao(db: UlaaaDatabase): BucketDao = db.bucketDao()
}
