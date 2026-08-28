package com.fugood.reactnativevideoplayer

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class ReactNativeVideoPlayerViewPackage : ReactPackage {
  override fun createViewManagers(
    reactContext: ReactApplicationContext
  ): List<ViewManager<*, *>> = listOf(ReactNativeVideoPlayerViewManager())

  // Deprecated upstream in favour of BaseReactPackage, which does not exist on
  // the older React Native releases this library supports.
  @Deprecated("Kept for compatibility with React Native < 0.74")
  @Suppress("DEPRECATION")
  override fun createNativeModules(
    reactContext: ReactApplicationContext
  ): List<NativeModule> = emptyList()
}
