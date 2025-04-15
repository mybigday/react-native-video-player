package com.fugood.reactnativevideoplayer

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event


class ReactVideoReadyEvent(surfaceId: Int, viewId: Int) : Event<ReactVideoReadyEvent>(surfaceId, viewId) {
  override fun getEventName(): String {
    return EVENT_NAME
  }

  override fun getEventData(): WritableMap = Arguments.createMap()

  override fun canCoalesce(): Boolean = false

  companion object {
    const val EVENT_NAME = "topReadyForDisplay"
  }
}
