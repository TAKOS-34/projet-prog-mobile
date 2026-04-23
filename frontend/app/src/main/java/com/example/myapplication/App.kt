package com.example.myapplication

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.myapplication.utils.ImageLoaderUtil

class App : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoaderUtil.build(this)
}
