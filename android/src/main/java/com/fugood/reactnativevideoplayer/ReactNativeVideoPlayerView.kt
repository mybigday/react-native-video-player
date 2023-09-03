package com.fugood.reactnativevideoplayer

import android.content.Context
import android.util.AttributeSet
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.MediaPlayer.OnPreparedListener
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnErrorListener
import android.media.MediaPlayer.OnInfoListener
import android.media.MediaPlayer.OnSeekCompleteListener
import android.media.MediaPlayer.OnVideoSizeChangedListener
import android.view.SurfaceView
import android.view.SurfaceHolder
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import android.net.Uri
import android.util.Log

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.UIManagerModule

class ReactNativeVideoPlayerView : FrameLayout, SurfaceHolder.Callback,
  OnPreparedListener, OnCompletionListener, OnErrorListener, OnInfoListener, OnSeekCompleteListener, OnVideoSizeChangedListener {

  protected var mUrl: String? = null
  protected var mHeaders: Map<String, String>? = null
  protected var mMuted = false
  protected var mVolume = 1.0f
  protected var mPaused = false
  protected var mSeekTo = 0L
  protected var mLoop = false
  protected var mProgressUpdateInterval = 250L
  protected val mPlaybackParams = PlaybackParams()
  // State
  protected var mPlaying = false
  protected var mPosition = 0L
  protected var mBackgroundPaused = false

  protected val container = AspectFrameLayout(context)
  protected val surface = SurfaceView(context)
  protected var player: MediaPlayer? = null
  protected var isReady = false

  protected val updateProgressTask = object : Runnable {
    override fun run() {
      updateProgress()
    }
  }

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    val params = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    params.gravity = android.view.Gravity.CENTER
    val aspectParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    aspectParams.gravity = android.view.Gravity.CENTER

    surface.layoutParams = params
    surface.holder.addCallback(this)
    container.addView(surface, 0, params)
    container.layoutParams = aspectParams
    addViewInLayout(container, 0, aspectParams)
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    isReady = true
    if (!mBackgroundPaused) {
      initPlayer()
    } else {
      mBackgroundPaused = false
      player?.setDisplay(holder)
      if (mPlaying) {
        player?.seekTo(mPosition, MediaPlayer.SEEK_CLOSEST)
      }
    }
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    player?.setDisplay(null)
    mBackgroundPaused = true
    mPlaying = player?.isPlaying ?: false
    mPosition = player?.currentPosition?.toLong() ?: 0L
    player?.pause()
  }

  fun release() {
    player?.release()
    player = null
    isReady = false
  }

  protected fun initPlayer() {
    if (!isReady) {
      return
    }
    if (player == null) {
      player = MediaPlayer()
      player!!.setDisplay(surface.holder)
    } else {
      player!!.reset()
    }
    if (mUrl?.isEmpty() == false) {
      player!!.setDataSource(context, Uri.parse(mUrl), mHeaders)
      player!!.setScreenOnWhilePlaying(true)
      player!!.prepareAsync()
      player!!.setOnPreparedListener(this)
      player!!.setOnCompletionListener(this)
      player!!.setOnErrorListener(this)
      player!!.setOnInfoListener(this)
      player!!.setOnSeekCompleteListener(this)
      player!!.setOnVideoSizeChangedListener(this)
    }
  }

  protected fun updateProgress() {
    if (player == null || mBackgroundPaused) {
      return
    }
    fireEvent("progress", Arguments.createMap().apply {
      putDouble("currentTime", (player?.currentPosition ?: 0).toDouble() / 1000)
      putDouble("duration", (player?.duration ?: 0).toDouble() / 1000)
    })
    if (player?.isPlaying == true) {
      postDelayed(updateProgressTask, mProgressUpdateInterval)
    }
  }

  fun setSource(url: String?, headers: Map<String, String>?) {
    mUrl = url
    mHeaders = headers
    if (!isReady) {
      return
    }
    post {
      initPlayer()
    }
  }

  fun setLoop(loop: Boolean) {
    mLoop = loop
    if (!isReady) {
      return
    }
    post {
      player?.setLooping(mLoop)
    }
  }

  fun setVolume(volume: Float) {
    mVolume = volume
    if (!isReady) {
      return
    }
    post {
      player?.setVolume(this.volume, this.volume)
    }
  }

  fun setPaused(paused: Boolean) {
    mPaused = paused
    if (!isReady) {
      return
    }
    post {
      if (mPaused) {
        player?.pause()
      } else {
        player?.start()
      }
    }
  }

  fun setMuted(muted: Boolean) {
    mMuted = muted
    if (!isReady) {
      return
    }
    post {
      player?.setVolume(volume, volume)
    }
  }

  fun setResizeMode(mode: String?) {
    val resizeMode = enumValueOf<AspectFrameLayout.ResizeMode>((mode ?: "contain").uppercase())
    container.resizeMode = resizeMode
  }

  fun setSpeed(speed: Float) {
    mPlaybackParams.speed = speed
    if (!isReady) {
      return
    }
    post {
      player?.setPlaybackParams(mPlaybackParams)
    }
  }

  fun seekTo(position: Float) {
    if (!isReady) {
      mSeekTo = (position * 1000).toLong()
      return
    }
    mPlaying = player?.isPlaying ?: false
    post {
      player?.seekTo((position * 1000).toLong(), MediaPlayer.SEEK_CLOSEST)
    }
  }

  fun play() {
    if (!isReady) {
      return
    }
    post {
      player?.start()
      updateProgress()
    }
  }

  fun pause() {
    if (!isReady) {
      return
    }
    post {
      player?.pause()
      removeCallbacks(updateProgressTask)
    }
  }

  fun stop() {
    if (!isReady) {
      return
    }
    post {
      player?.stop()
      removeCallbacks(updateProgressTask)
    }
  }

  fun setProgressUpdateInterval(interval: Int?) {
    mProgressUpdateInterval = (interval ?: 0).toLong()
  }

  private fun fireEvent(name: String, event: WritableMap?) {
    (context as ReactContext)
      .getNativeModule(UIManagerModule::class.java)
      ?.receiveEvent(id, name, event ?: Arguments.createMap())
  }

  override fun onPrepared(mp: MediaPlayer?) {
    if (mp == null) {
      return
    }
    mp.setPlaybackParams(mPlaybackParams)
    mp.setLooping(mLoop)
    mp.setVolume(volume, volume)
    if (mSeekTo > 0) {
      mPlaying = true
      mp.seekTo(mSeekTo, MediaPlayer.SEEK_CLOSEST)
    }
    fireEvent("ready", null)
    if (!mPaused && mSeekTo <= 0) {
      mp.start()
    }
  }

  override fun onCompletion(mp: MediaPlayer?) {
    removeCallbacks(updateProgressTask)
    fireEvent("end", null)
  }

  override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
    if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
      player?.release()
      player = null
      initPlayer()
      return true
    } else {
      when (extra) {
        MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> {
          fireEvent("error", Arguments.createMap().apply {
            putString("message", "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK")
          })
        }
        MediaPlayer.MEDIA_ERROR_IO -> {
          fireEvent("error", Arguments.createMap().apply {
            putString("message", "MEDIA_ERROR_IO")
          })
        }
        MediaPlayer.MEDIA_ERROR_MALFORMED -> {
          fireEvent("error", Arguments.createMap().apply {
            putString("message", "MEDIA_ERROR_MALFORMED")
          })
        }
        MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> {
          fireEvent("error", Arguments.createMap().apply {
            putString("message", "MEDIA_ERROR_UNSUPPORTED")
          })
        }
        MediaPlayer.MEDIA_ERROR_TIMED_OUT -> {
          fireEvent("error", Arguments.createMap().apply {
            putString("message", "MEDIA_ERROR_TIMED_OUT")
          })
        }
        else -> {
          fireEvent("error", Arguments.createMap().apply {
            putString("error", "MEDIA_ERROR_UNKNOWN")
          })
        }
      }
    }
    return false
  }

  override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
    when (what) {
      MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
        fireEvent("buffering", Arguments.createMap().apply {
          putBoolean("isBuffering", true)
        })
      }
      MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
        fireEvent("buffering", Arguments.createMap().apply {
          putBoolean("isBuffering", false)
        })
      }
      MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
        fireEvent("load", null)
        updateProgress()
      }
    }
    return false
  }

  override fun onSeekComplete(mp: MediaPlayer?) {
    if (mPlaying) {
      updateProgress()
      mp?.start()
    }
  }

  override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
    container.aspectRatio = width.toFloat() / height.toFloat()
  }

  val volume: Float
    get() = if (mMuted) 0.0f else mVolume

  val duration: Int
    get() = player?.duration ?: 0

  val currentPosition: Int
    get() = player?.currentPosition ?: 0

  val isPlaying: Boolean
    get() = player?.isPlaying ?: false
}
