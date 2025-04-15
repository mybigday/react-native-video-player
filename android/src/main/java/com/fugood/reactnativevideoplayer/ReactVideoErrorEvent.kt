package com.fugood.reactnativevideoplayer

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event


class ReactVideoErrorEvent(surfaceId: Int, viewId: Int, private val error: String) : Event<ReactVideoErrorEvent>(surfaceId, viewId) {
  override fun getEventName(): String {
    return EVENT_NAME
  }

  override fun canCoalesce(): Boolean = false

  override fun getEventData(): WritableMap {
    val eventData = Arguments.createMap()
    eventData.putString("error", error)
    return eventData
  }

  companion object {
    const val EVENT_NAME = "topError"
  }
}
