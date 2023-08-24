package com.fugood.reactnativevideoplayer

import android.graphics.Color
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

@ReactModule(name = ReactNativeVideoPlayerViewManager.NAME)
class ReactNativeVideoPlayerViewManager :
  ReactNativeVideoPlayerViewManagerSpec<ReactNativeVideoPlayerView>() {
  override fun getName(): String {
    return NAME
  }

  public override fun createViewInstance(context: ThemedReactContext): ReactNativeVideoPlayerView {
    return ReactNativeVideoPlayerView(context)
  }

  @ReactProp(name = "url")
  override fun setUrl(view: ReactNativeVideoPlayerView?, value: String?) {
    view?.setUrl(value)
  }

  @ReactProp(name = "loop")
  override fun setLoop(view: ReactNativeVideoPlayerView?, value: Boolean?) {
    view?.setLoop(value ?: false)
  }

  @ReactProp(name = "volume")
  override fun setVolume(view: ReactNativeVideoPlayerView?, value: Float) {
    view?.setVolume(value)
  }

  @ReactProp(name = "paused")
  override fun setPaused(view: ReactNativeVideoPlayerView?, value: Boolean?) {
    view?.setPaused(value ?: false)
  }

  @ReactProp(name = "seek")
  override fun setSeek(view: ReactNativeVideoPlayerView?, value: Int?) {
    view?.setSeekTo(value ?: 0)
  }

  @ReactProp(name = "resizeMode")
  override fun setResizeMode(view: ReactNativeVideoPlayerView?, value: String?) {
    view?.setResizeMode(value)
  }

  @ReactProp(name = "progressUpdateInterval")
  override fun setProgressUpdateInterval(view: ReactNativeVideoPlayerView?, value: Int?) {
    view?.setProgressUpdateInterval(value)
  }

  // override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any> {
  //   return mapOf(
  //     "seekTo" to mapOf("registrationName" to "onSeekTo"),
  //     "bufferingStart" to mapOf("registrationName" to "onStartBuffering"),
  //     "bufferingEnd" to mapOf("registrationName" to "onEndBuffering"),
  //     "play" to mapOf("registrationName" to "onPlay"),
  //     "progress" to mapOf("registrationName" to "onProgress"),
  //     "end" to mapOf("registrationName" to "onEnd"),
  //     "error" to mapOf("registrationName" to "onError"),
  //     "videoSize" to mapOf("registrationName" to "onVideoSize")
  //   )
  // }

  companion object {
    const val NAME = "ReactNativeVideoPlayerView"
  }
}
