package com.fugood.reactnativevideoplayer

import android.view.View

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.ReactNativeVideoPlayerViewManagerDelegate
import com.facebook.react.viewmanagers.ReactNativeVideoPlayerViewManagerInterface
import com.facebook.soloader.SoLoader

abstract class ReactNativeVideoPlayerViewManagerSpec<T : View> : SimpleViewManager<T>(), ReactNativeVideoPlayerViewManagerInterface<T> {
  private val mDelegate: ViewManagerDelegate<T>

  init {
    mDelegate = ReactNativeVideoPlayerViewManagerDelegate(this)
  }

  override fun getDelegate(): ViewManagerDelegate<T>? {
    return mDelegate
  }

  companion object {
    init {
      if (BuildConfig.CODEGEN_MODULE_REGISTRATION != null) {
        SoLoader.loadLibrary(BuildConfig.CODEGEN_MODULE_REGISTRATION)
      }
    }
  }
}
