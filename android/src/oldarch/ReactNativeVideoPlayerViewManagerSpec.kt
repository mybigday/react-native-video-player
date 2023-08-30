package com.fugood.reactnativevideoplayer

import android.view.View
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.SimpleViewManager

abstract class ReactNativeVideoPlayerViewManagerSpec<T : View> : SimpleViewManager<T>() {
  abstract fun setSource(view: T, value: ReadableMap?)
  abstract fun setLoop(view: T, value: Boolean)
  abstract fun setVolume(view: T, value: Float)
  abstract fun setPaused(view: T, value: Boolean)
  abstract fun setMuted(view: T, value: Boolean)
  abstract fun setSeek(view: T, value: Float)
  abstract fun setResizeMode(view: T, value: String?)
  abstract fun setSpeed(view: T, value: Float)
  abstract fun setProgressUpdateInterval(view: T, value: Int)
  // commands
  abstract fun seek(view: T, value: Float)
  abstract fun play(view: T)
  abstract fun pause(view: T)
  abstract fun stop(view: T)
}
