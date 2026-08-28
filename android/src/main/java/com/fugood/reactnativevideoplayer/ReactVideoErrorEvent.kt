package com.fugood.reactnativevideoplayer

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event

class ReactVideoErrorEvent(surfaceId: Int, viewId: Int, private val message: String) :
  Event<ReactVideoErrorEvent>(surfaceId, viewId) {

  override fun getEventName(): String = EVENT_NAME

  override fun canCoalesce(): Boolean = false

  override fun getEventData(): WritableMap =
    Arguments.createMap().apply { putString("message", message) }

  companion object {
    const val EVENT_NAME = "topError"
  }
}
