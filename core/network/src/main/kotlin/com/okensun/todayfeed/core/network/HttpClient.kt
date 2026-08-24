package com.okensun.todayfeed.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * One client for every source. The Retrofit instance is not shared, because each source has its
 * own base address, but the connection pool and the timeouts should be.
 */
fun todayFeedHttpClient(debug: Boolean): OkHttpClient =
    OkHttpClient
        .Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .apply {
            if (debug) {
                // Headers only. A body would put whole articles in the log, and a log is not the
                // place for content.
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }
                )
            }
        }.build()

private const val TIMEOUT_SECONDS = 15L
