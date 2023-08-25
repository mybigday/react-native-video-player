package com.fugood.reactnativevideoplayer

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import android.util.Log

class AspectFrameLayout: FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  )

  enum class ResizeMode {
    CONTAIN,
    COVER,
    STRETCH
  }

  protected var mResizeMode = ResizeMode.STRETCH
  protected var mAspectRatio = -1.0f

  var resizeMode: ResizeMode
    get() = mResizeMode
    set(value) {
      if (value != mResizeMode) {
        mResizeMode = value
        requestLayout()
      }
    }

  var aspectRatio: Float
    get() = mAspectRatio
    set(value) {
      if (value < 0) {
        throw IllegalArgumentException()
      }
      if (value != mAspectRatio) {
        mAspectRatio = value
        requestLayout()
      }
    }

  override fun requestLayout() {
    super.requestLayout()
    post {
      measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
      layout(left, top, right, bottom)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)
    val viewRatio = width.toFloat() / height.toFloat()
    val aspectDeformation = mAspectRatio / viewRatio - 1
    Log.d("AspectFrame", "onMeasure: $aspectDeformation, $viewRatio, $mAspectRatio, $mResizeMode")
    if (mAspectRatio <= 0 || Math.abs(aspectDeformation) <= 0.01 || mResizeMode == ResizeMode.STRETCH) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    } else {
      var newWidth: Int = width
      var newHeight: Int = height
      when (mResizeMode) {
        ResizeMode.COVER -> {
          newWidth = (width.toFloat() * mAspectRatio).toInt()
          if (newWidth < width) {
            val scaleFactor = width.toFloat() / newWidth
            newWidth = (width.toFloat() * scaleFactor).toInt()
            newHeight = (height.toFloat() * scaleFactor).toInt()
          }
        }
        else -> {
          if (aspectDeformation > 0) {
            newHeight = (width.toFloat() / mAspectRatio).toInt()
          } else {
            newWidth = (height.toFloat() * mAspectRatio).toInt()
          }
        }
      }
      val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY)
      val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY)
      super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec)
    }
  }
}
