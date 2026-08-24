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
    // `Lazy`, so a cold start does not build the client and load the system trust store before
    // the first frame. The fetcher reads it when a picture is actually wanted.
    @Inject
    lateinit var client: dagger.Lazy<OkHttpClient>

    /**
     * Pictures go through the client this project already builds, so they share its connection
     * pool and its timeouts instead of opening a second stack of their own.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client.get() })) }
            .build()
}
