package com.CYRYEL.com.di

import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.CYRYEL.com.data.auth.AuthRepository
import com.CYRYEL.com.data.auth.FirebaseAuthRepository
import com.CYRYEL.com.data.cart.CartManager
import com.CYRYEL.com.data.category.CategoryRepository
import com.CYRYEL.com.data.category.FirebaseCategoryRepository
import com.CYRYEL.com.data.config.ConfigRepository
import com.CYRYEL.com.data.config.FirebaseConfigRepository
import com.CYRYEL.com.data.delivery.DeliveryRepository
import com.CYRYEL.com.data.delivery.FirebaseDeliveryRepository
import com.CYRYEL.com.data.notificacion.FirebaseNotificacionRepository
import com.CYRYEL.com.data.notificacion.NotificacionRepository
import com.CYRYEL.com.data.local.AppDatabase
import com.CYRYEL.com.data.local.ProductDao
import com.CYRYEL.com.data.order.FirebaseOrderRepository
import com.CYRYEL.com.data.order.OrderRepository
import com.CYRYEL.com.data.product.FirebaseProductRepository
import com.CYRYEL.com.data.product.ProductRepository
import com.CYRYEL.com.data.promotion.FirebasePromotionRepository
import com.CYRYEL.com.data.promotion.PromotionRepository
import com.CYRYEL.com.data.user.FirebaseUserRepository
import com.CYRYEL.com.data.user.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore): AuthRepository {
        return FirebaseAuthRepository(firebaseAuth, firestore)
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tienda_native_cache.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideProductDao(database: AppDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    @Singleton
    fun provideProductRepository(
        firestore: FirebaseFirestore,
        productDao: ProductDao
    ): ProductRepository {
        return FirebaseProductRepository(firestore, productDao)
    }

    @Provides
    @Singleton
    fun provideOrderRepository(firestore: FirebaseFirestore): OrderRepository {
        return FirebaseOrderRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth
    ): UserRepository {
        return FirebaseUserRepository(firestore, firebaseAuth)
    }

    @Provides
    @Singleton
    fun providePromotionRepository(firestore: FirebaseFirestore): PromotionRepository {
        return FirebasePromotionRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(firestore: FirebaseFirestore): CategoryRepository {
        return FirebaseCategoryRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideConfigRepository(firestore: FirebaseFirestore): ConfigRepository {
        return FirebaseConfigRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideNotificacionRepository(firestore: FirebaseFirestore): NotificacionRepository {
        return FirebaseNotificacionRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideDeliveryRepository(firestore: FirebaseFirestore): DeliveryRepository {
        return FirebaseDeliveryRepository(firestore)
    }
}
