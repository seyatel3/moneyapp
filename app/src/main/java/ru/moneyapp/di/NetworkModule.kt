package ru.moneyapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import ru.moneyapp.data.auth.AuthTokenInterceptor
import ru.moneyapp.data.remote.DriveService
import ru.moneyapp.data.remote.OAuth2Service
import ru.moneyapp.data.remote.SheetsService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val OKHTTP_TIMEOUT_SECONDS = 60L

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthTokenInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("noAuth")
    fun provideOkHttpClientNoAuth(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideSheetsService(okHttpClient: OkHttpClient, moshi: Moshi): SheetsService =
        Retrofit.Builder()
            .baseUrl(SheetsService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SheetsService::class.java)

    @Provides
    @Singleton
    fun provideDriveService(okHttpClient: OkHttpClient, moshi: Moshi): DriveService =
        Retrofit.Builder()
            .baseUrl(DriveService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DriveService::class.java)

    @Provides
    @Singleton
    fun provideOAuth2Service(
        @Named("noAuth") okHttpClientNoAuth: OkHttpClient,
        moshi: Moshi
    ): OAuth2Service =
        Retrofit.Builder()
            .baseUrl(OAuth2Service.BASE_URL)
            .client(okHttpClientNoAuth)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OAuth2Service::class.java)
}
