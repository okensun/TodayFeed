package com.okensun.todayfeed.core.network

import com.okensun.todayfeed.core.freshness.Connection
import com.okensun.todayfeed.core.freshness.Connectivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    /**
     * One client for every source, so the connection pool and the timeouts are shared. Each
     * source builds its own Retrofit on top, because the base address is per source.
     */
    @Provides
    @Singleton
    fun httpClient(): OkHttpClient = todayFeedHttpClient(debug = BuildConfig.DEBUG)

    /**
     * A stand-in that always answers unmetered. Pass 5 replaces the body with a read of
     * `NET_CAPABILITY_NOT_METERED`, at which point the metered behaviour becomes real. It lives
     * here because reading the platform is this module's job, not the application module's.
     */
    @Provides
    @Singleton
    fun connectivity(): Connectivity =
        object : Connectivity {
            override fun current(): Connection = Connection.Unmetered

            override fun observe(): Flow<Connection> = flowOf(Connection.Unmetered)
        }
}
