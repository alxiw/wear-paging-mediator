package io.github.alxiw.wearpagingmediator.data.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.alxiw.wearpagingmediator.data.remote.api.GitHubApi
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    internal fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor {
            message -> Log.d("HELLO", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    internal fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    @Singleton
    internal fun provideRetrofit(client: OkHttpClient, moshi: Moshi): GitHubApi {
        return Retrofit.Builder()
            .baseUrl(GitHubApi.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApi::class.java)
    }

    @Provides
    @Singleton
    internal fun providesGitHubDatabase(@ApplicationContext app: Context): GitHubDatabase {
        return Room.databaseBuilder(
            app,
            GitHubDatabase::class.java,
            GitHubDatabase.DB_NAME
        )
            .addMigrations(
                GitHubDatabase.MIGRATION_1_2,
                GitHubDatabase.MIGRATION_2_3,
                GitHubDatabase.MIGRATION_3_4
            )
            .build()
    }
}
