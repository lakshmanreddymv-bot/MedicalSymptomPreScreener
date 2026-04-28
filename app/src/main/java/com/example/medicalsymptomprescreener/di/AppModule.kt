package com.example.medicalsymptomprescreener.di

import android.content.Context
import androidx.room.Room
import com.example.medicalsymptomprescreener.BuildConfig
import com.example.medicalsymptomprescreener.data.api.GeminiRetrofitService
import com.example.medicalsymptomprescreener.data.api.GeminiTriageApi
import com.example.medicalsymptomprescreener.data.api.GeminiTriageApiImpl
import com.example.medicalsymptomprescreener.data.api.GooglePlacesApiImpl
import com.example.medicalsymptomprescreener.data.api.GooglePlacesRetrofitService
import com.example.medicalsymptomprescreener.data.local.SymptomDao
import com.example.medicalsymptomprescreener.data.local.SymptomDatabase
import com.example.medicalsymptomprescreener.data.monitor.ConnectivityNetworkMonitor
import com.example.medicalsymptomprescreener.data.repository.FacilityRepositoryImpl
import com.example.medicalsymptomprescreener.data.repository.SymptomRepositoryImpl
import com.example.medicalsymptomprescreener.domain.monitor.NetworkMonitor
import com.example.medicalsymptomprescreener.domain.repository.FacilityRepository
import com.example.medicalsymptomprescreener.domain.repository.SymptomRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()

    // Gemini Retrofit instance
    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideGeminiRetrofitService(@Named("gemini") retrofit: Retrofit): GeminiRetrofitService =
        retrofit.create(GeminiRetrofitService::class.java)

    @Provides
    @Singleton
    fun provideGeminiTriageApi(
        service: GeminiRetrofitService,
        gson: Gson
    ): GeminiTriageApi = GeminiTriageApiImpl(service, BuildConfig.geminiapikey, gson)

    // Maps / Places Retrofit instance
    @Provides
    @Singleton
    @Named("places")
    fun providePlacesRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://places.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideGooglePlacesRetrofitService(@Named("places") retrofit: Retrofit): GooglePlacesRetrofitService =
        retrofit.create(GooglePlacesRetrofitService::class.java)

    @Provides
    @Singleton
    fun provideGooglePlacesApiImpl(service: GooglePlacesRetrofitService): GooglePlacesApiImpl =
        GooglePlacesApiImpl(service, BuildConfig.mapsapikey)

    // Room
    @Provides
    @Singleton
    fun provideSymptomDatabase(@ApplicationContext context: Context): SymptomDatabase =
        Room.databaseBuilder(context, SymptomDatabase::class.java, "symptom_history.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideSymptomDao(database: SymptomDatabase): SymptomDao = database.symptomDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindSymptomRepository(impl: SymptomRepositoryImpl): SymptomRepository

    @Binds
    @Singleton
    abstract fun bindFacilityRepository(impl: FacilityRepositoryImpl): FacilityRepository
}
