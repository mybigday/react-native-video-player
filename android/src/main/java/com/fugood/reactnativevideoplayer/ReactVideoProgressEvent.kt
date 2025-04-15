package com.fugood.reactnativevideoplayer

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event


class ReactVideoProgressEvent(
  surfaceId: Int,
  viewId: Int,
  private val currentTime: Double,
  private val duration: Double
) : Event<ReactVideoProgressEvent>(surfaceId, viewId) {
  override fun getEventName(): String {
    return EVENT_NAME
  }

  override fun canCoalesce(): Boolean = false

  override fun getEventData(): WritableMap {
    val event = Arguments.createMap()
    event.putDouble("currentTime", currentTime)
    event.putDouble("duration", duration)
    return event
  }

  companion object {
    const val EVENT_NAME = "topProgress"
  }
}
