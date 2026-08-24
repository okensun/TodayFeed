package com.okensun.todayfeed

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class TodayFeedApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    lateinit var client: OkHttpClient

    /**
     * Pictures go through the client this project already builds, so they share its connection
     * pool and its timeouts instead of opening a second stack of their own.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()
}
