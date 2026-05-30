package com.catamsp.Daemon.apps

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

class AppIconFetcher(
    private val data: AbstractDetailedAppInfo,
    private val context: Context,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val drawable = data.getIcon(context)
        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<AbstractDetailedAppInfo> {
        private val appContext = context.applicationContext

        override fun create(data: AbstractDetailedAppInfo, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(data, appContext, options)
        }
    }
}
