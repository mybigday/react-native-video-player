package com.fugood.reactnativevideoplayer

import android.graphics.Color
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

@ReactModule(name = ReactNativeVideoPlayerViewManager.NAME)
class ReactNativeVideoPlayerViewManager :
  ReactNativeVideoPlayerViewManagerSpec<ReactNativeVideoPlayerView>() {
  override fun getName() = NAME

  public override fun createViewInstance(context: ThemedReactContext) =
    ReactNativeVideoPlayerView(context)

  @ReactProp(name = "source")
  override fun setSource(view: ReactNativeVideoPlayerView?, value: ReadableMap?) {
    view?.setSource(
      value?.getString("uri"),
      value?.getMap("headers")?.toHashMap()?.mapValues { it.value.toString() }
    )
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

  @ReactProp(name = "muted")
  override fun setMuted(view: ReactNativeVideoPlayerView?, value: Boolean?) {
    view?.setMuted(value ?: false)
  }

  @ReactProp(name = "seek")
  override fun setSeek(view: ReactNativeVideoPlayerView?, value: Int?) {
    view?.setSeekTo(value ?: 0)
  }

  @ReactProp(name = "resizeMode")
  override fun setResizeMode(view: ReactNativeVideoPlayerView?, value: String?) {
    view?.setResizeMode(value)
  }

  @ReactProp(name = "speed")
  override fun setSpeed(view: ReactNativeVideoPlayerView?, value: Float) {
    view?.setSpeed(value)
  }

  @ReactProp(name = "progressUpdateInterval")
  override fun setProgressUpdateInterval(view: ReactNativeVideoPlayerView?, value: Int?) {
    view?.setProgressUpdateInterval(value)
  }

  override fun receiveCommand(view: ReactNativeVideoPlayerView, commandId: Int, args: ReadableArray?) {
    when (commandId) {
      COMMAND_SEEK -> view.seekTo(args?.getInt(0) ?: 0)
      COMMAND_PLAY -> view.play()
      COMMAND_PAUSE -> view.pause()
      COMMAND_STOP -> view.stop()
      else -> throw IllegalArgumentException("Invalid commandId: $commandId")
    }
  }

  override fun getCommandsMap(): Map<String, Int> {
    return mutableMapOf(
      "seek" to COMMAND_SEEK,
      "play" to COMMAND_PLAY,
      "pause" to COMMAND_PAUSE,
      "stop" to COMMAND_STOP
    )
  }

  override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any> {
    return mutableMapOf(
      "buffering" to mutableMapOf("registrationName" to "onBuffer"),
      "ready" to mutableMapOf("registrationName" to "onReadyForDisplay"),
      "load" to mutableMapOf("registrationName" to "onLoad"),
      "progress" to mutableMapOf("registrationName" to "onProgress"),
      "end" to mutableMapOf("registrationName" to "onEnd"),
      "error" to mutableMapOf("registrationName" to "onError")
    )
  }

  companion object {
    const val NAME = "ReactNativeVideoPlayerView"

    private const val COMMAND_SEEK = 1
    private const val COMMAND_PLAY = 2
    private const val COMMAND_PAUSE = 3
    private const val COMMAND_STOP = 4
  }
}
