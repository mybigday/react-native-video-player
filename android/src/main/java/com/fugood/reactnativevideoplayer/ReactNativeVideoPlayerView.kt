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
  protected var mSeekTo = 0
  protected var mLoop = false
  protected var mProgressUpdateInterval = 250L
  protected val mPlaybackParams = PlaybackParams()

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
    initPlayer()
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    player?.release()
    player = null
    isReady = false
  }

  protected fun initPlayer() {
    if (!isReady) {
      return
    }
    post {
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
  }

  protected fun updateProgress() {
    if (player == null) {
      return
    }
    fireEvent("progress", Arguments.createMap().apply {
      putInt("position", player?.currentPosition ?: 0)
    })
    if (player?.isPlaying == true) {
      postDelayed(updateProgressTask, mProgressUpdateInterval)
    }
  }

  fun setSource(url: String?, headers: Map<String, String>?) {
    mUrl = url
    mHeaders = headers
    initPlayer()
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

  fun setSeekTo(seekTo: Int) {
    mSeekTo = seekTo
    this.seekTo(mSeekTo)
  }

  fun setResizeMode(mode: String?) {
    var resizeMode = enumValueOf<AspectFrameLayout.ResizeMode>(mode ?: "cover")
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

  fun seekTo(position: Int) {
    if (!isReady) {
      return
    }
    post {
      player?.seekTo(position)
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
      ?.receiveEvent(id, name, event)
  }

  override fun onPrepared(mp: MediaPlayer?) {
    if (mp == null) {
      return
    }
    mp.setPlaybackParams(mPlaybackParams)
    mp.setLooping(mLoop)
    mp.setVolume(volume, volume)
    if (mSeekTo > 0) {
      mp.seekTo(mSeekTo)
    }
    fireEvent("ready", null)
    if (!mPaused) {
      mp.start()
    }
  }

  override fun onCompletion(mp: MediaPlayer?) {
    removeCallbacks(updateProgressTask)
    fireEvent("end", null)
  }

  override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
    Log.d("VideoPlayer", "onError: $what, $extra")
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
            putString("message", "MEDIA_ERROR_UNKNOWN")
          })
        }
      }
    }
    return false
  }

  override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
    Log.d("VideoPlayer", "onInfo: $what, $extra")
    when (what) {
      MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
        fireEvent("bufferingStart", null)
      }
      MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
        fireEvent("bufferingEnd", null)
      }
      MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
        fireEvent("load", null)
        updateProgress()
      }
    }
    return false
  }

  override fun onSeekComplete(mp: MediaPlayer?) {
    fireEvent("seekTo", Arguments.createMap().apply {
      putInt("position", mp?.currentPosition ?: 0)
    })
  }

  override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
    Log.d("VideoPlayer", "onVideoSizeChanged: $width, $height")
    container.aspectRatio = width.toFloat() / height.toFloat()
    fireEvent("videoSize", Arguments.createMap().apply {
      putInt("width", width)
      putInt("height", height)
    })
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
