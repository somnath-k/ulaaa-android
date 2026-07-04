package com.dotkios.ulaaa.di

import com.dotkios.ulaaa.data.repository.AuthRepository
import com.dotkios.ulaaa.data.repository.AuthRepositoryImpl
import com.dotkios.ulaaa.data.repository.BucketRepository
import com.dotkios.ulaaa.data.repository.BucketRepositoryImpl
import com.dotkios.ulaaa.data.repository.ChatRepository
import com.dotkios.ulaaa.data.repository.ChatRepositoryImpl
import com.dotkios.ulaaa.data.repository.PlacesRepository
import com.dotkios.ulaaa.data.repository.PlacesRepositoryImpl
import com.dotkios.ulaaa.data.repository.RecommendationRepository
import com.dotkios.ulaaa.data.repository.RecommendationRepositoryImpl
import com.dotkios.ulaaa.data.repository.TripDetailRepository
import com.dotkios.ulaaa.data.repository.TripDetailRepositoryImpl
import com.dotkios.ulaaa.data.repository.TripRepository
import com.dotkios.ulaaa.data.repository.TripRepositoryImpl
import com.dotkios.ulaaa.data.repository.UserRepository
import com.dotkios.ulaaa.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindPlacesRepository(impl: PlacesRepositoryImpl): PlacesRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    @Binds
    @Singleton
    abstract fun bindBucketRepository(impl: BucketRepositoryImpl): BucketRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(impl: RecommendationRepositoryImpl): RecommendationRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindTripDetailRepository(impl: TripDetailRepositoryImpl): TripDetailRepository
}
