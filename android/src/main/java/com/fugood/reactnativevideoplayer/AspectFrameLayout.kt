package com.fugood.reactnativevideoplayer

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Sizes its children to the video's aspect ratio.
 *
 * React Native does not lay out views added from native code, so after every
 * [requestLayout] the measure/layout pass has to be re-run manually — but only
 * once per frame, otherwise the layout loops.
 */
class AspectFrameLayout
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

  enum class ResizeMode {
    CONTAIN,
    COVER,
    STRETCH;

    companion object {
      /** Falls back to [CONTAIN] for unknown values instead of throwing. */
      fun from(value: String?): ResizeMode =
        values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CONTAIN
    }
  }

  private var relayoutScheduled = false

  private val relayoutRunnable = Runnable {
    relayoutScheduled = false
    measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
    )
    layout(left, top, right, bottom)
  }

  var resizeMode: ResizeMode = ResizeMode.CONTAIN
    set(value) {
      if (value != field) {
        field = value
        requestLayout()
      }
    }

  /** Width / height of the video, or a non-positive value when unknown. */
  var aspectRatio: Float = -1.0f
    set(value) {
      val sanitized = if (value.isFinite() && value > 0f) value else -1.0f
      if (sanitized != field) {
        field = sanitized
        requestLayout()
      }
    }

  override fun requestLayout() {
    super.requestLayout()
    if (!relayoutScheduled && width > 0 && height > 0) {
      relayoutScheduled = true
      post(relayoutRunnable)
    }
  }

  override fun onDetachedFromWindow() {
    removeCallbacks(relayoutRunnable)
    relayoutScheduled = false
    super.onDetachedFromWindow()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)

    if (aspectRatio <= 0f || width == 0 || height == 0 ||
      resizeMode == ResizeMode.STRETCH
    ) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      return
    }

    val viewRatio = width.toFloat() / height.toFloat()
    val aspectDeformation = aspectRatio / viewRatio - 1f
    if (abs(aspectDeformation) <= MAX_ASPECT_DEFORMATION) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      return
    }

    var newWidth = width
    var newHeight = height
    when (resizeMode) {
      ResizeMode.COVER ->
        if (aspectDeformation > 0) {
          newWidth = (height * aspectRatio).toInt()
        } else {
          newHeight = (width / aspectRatio).toInt()
        }
      else ->
        if (aspectDeformation > 0) {
          newHeight = (width / aspectRatio).toInt()
        } else {
          newWidth = (height * aspectRatio).toInt()
        }
    }

    super.onMeasure(
      MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY),
    )
  }

  private companion object {
    /** Below this the letterboxing is invisible, so it is not worth resizing. */
    const val MAX_ASPECT_DEFORMATION = 0.01f
  }
}
