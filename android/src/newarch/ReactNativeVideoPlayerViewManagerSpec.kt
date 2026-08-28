package com.fugood.reactnativevideoplayer

import android.view.View
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.ReactNativeVideoPlayerViewManagerDelegate
import com.facebook.react.viewmanagers.ReactNativeVideoPlayerViewManagerInterface

abstract class ReactNativeVideoPlayerViewManagerSpec<T : View> :
  SimpleViewManager<T>(), ReactNativeVideoPlayerViewManagerInterface<T> {

  private val mDelegate: ViewManagerDelegate<T> =
    ReactNativeVideoPlayerViewManagerDelegate(this)

  override fun getDelegate(): ViewManagerDelegate<T> = mDelegate
}
