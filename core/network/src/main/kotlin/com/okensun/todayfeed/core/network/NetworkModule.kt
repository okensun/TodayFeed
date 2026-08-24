package com.okensun.todayfeed.core.network

import android.content.Context
import com.okensun.todayfeed.core.freshness.Connectivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
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
     * Reading the platform is this module's job, not the application module's.
     *
     * Every read behind [Connectivity] is a binder call, and both collectors are on the main
     * thread, so the dispatcher is passed in rather than chosen inside.
     */
    @Provides
    @Singleton
    fun connectivity(
        @ApplicationContext context: Context,
    ): Connectivity = AndroidConnectivity(context, Dispatchers.IO)
}
