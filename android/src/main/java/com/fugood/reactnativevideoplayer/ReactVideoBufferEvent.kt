package com.fugood.reactnativevideoplayer

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event


class ReactVideoBufferEvent(surfaceId: Int, viewId: Int, private val isBuffering: Boolean) : Event<ReactVideoBufferEvent>(surfaceId, viewId) {
  override fun getEventName(): String {
    return EVENT_NAME
  }

  override fun getEventData(): WritableMap {
    val event = Arguments.createMap()
    event.putBoolean("isBuffering", isBuffering)
    return event
  }

  override fun canCoalesce(): Boolean = false

  companion object {
    const val EVENT_NAME = "topBuffer"
  }
}
