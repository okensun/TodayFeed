package com.okensun.todayfeed

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * The clock, and only the clock. Everything else is provided by the module that owns it, so this
 * one does not accumulate knowledge of how each core module is set up. Time has no owner: the
 * freshness policy takes it as a parameter and the data layer reads it, so it is provided here.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemUTC()
}
