package com.fugood.reactnativevideoplayer

import android.view.View
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.SimpleViewManager

abstract class ReactNativeVideoPlayerViewManagerSpec<T : View> : SimpleViewManager<T>() {
  abstract fun setUrl(view: T?, value: String?)
  abstract fun setLoop(view: T?, value: Boolean?)
  abstract fun setVolume(view: T?, value: Float)
  abstract fun setPaused(view: T?, value: Boolean?)
  abstract fun setSeek(view: T?, value: Int?)
  abstract fun setResizeMode(view: T?, value: String?)
  abstract fun setProgressUpdateInterval(view: T?, value: Int?)
}
