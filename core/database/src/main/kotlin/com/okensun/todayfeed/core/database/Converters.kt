package com.okensun.todayfeed.core.database

import androidx.room.TypeConverter
import java.time.Duration
import java.time.Instant

/**
 * Shared across every component's database. `Instant` and `Duration` are available on API 24
 * through core library desugaring, which is why the freshness policy can speak in them.
 */
class Converters {
    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun durationToSeconds(value: Duration?): Long? = value?.seconds

    @TypeConverter
    fun secondsToDuration(value: Long?): Duration? = value?.let(Duration::ofSeconds)
}
