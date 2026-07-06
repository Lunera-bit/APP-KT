package com.cyryel.di

import android.content.Context
import androidx.room.Room
import com.cyryel.data.auth.AuthRepository
import com.cyryel.data.auth.FirebaseAuthRepository
import com.cyryel.data.cart.CartManager
import com.cyryel.data.category.CategoryRepository
import com.cyryel.data.category.FirebaseCategoryRepository
import com.cyryel.data.config.ConfigRepository
import com.cyryel.data.config.FirebaseConfigRepository
import com.cyryel.data.notificacion.FirebaseNotificacionRepository
import com.cyryel.data.notificacion.NotificacionRepository
import com.cyryel.data.local.AppDatabase
import com.cyryel.data.local.ProductDao
import com.cyryel.data.order.FirebaseOrderRepository
import com.cyryel.data.order.OrderRepository
import com.cyryel.data.product.FirebaseProductRepository
import com.cyryel.data.product.ProductRepository
import com.cyryel.data.promotion.FirebasePromotionRepository
import com.cyryel.data.promotion.PromotionRepository
import com.cyryel.data.user.FirebaseUserRepository
import com.cyryel.data.user.UserRepository
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
}
