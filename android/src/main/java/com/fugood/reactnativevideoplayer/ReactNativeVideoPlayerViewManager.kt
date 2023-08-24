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

  @ReactProp(name = "color")
  override fun setColor(view: ReactNativeVideoPlayerView?, color: String?) {
    view?.setBackgroundColor(Color.parseColor(color))
  }

  companion object {
    const val NAME = "ReactNativeVideoPlayerView"
  }
}
