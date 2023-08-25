package com.fugood.reactnativevideoplayer

import android.content.Context
import android.util.AttributeSet
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnErrorListener
import android.media.MediaPlayer.OnInfoListener
import android.media.MediaPlayer.OnSeekCompleteListener
import android.media.MediaPlayer.OnVideoSizeChangedListener
import android.view.SurfaceView
import android.view.SurfaceHolder
import android.view.View.MeasureSpec
import android.net.Uri
import android.util.Log

import java.util.Timer
import java.util.TimerTask

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter

class ReactNativeVideoPlayerView : SurfaceView,
  OnPreparedListener, OnCompletionListener, OnErrorListener, OnInfoListener, OnSeekCompleteListener, OnVideoSizeChangedListener,
  SurfaceHolder.Callback {
  protected var mUrl: String? = null
  protected var mResizeMode = "contain"
  protected var mVolume = 1.0f
  protected var mPaused = false
  protected var mSeekTo = 0
  protected var mLoop = false
  protected var mProgressUpdateInterval = 250

  protected var player: MediaPlayer? = null
  protected var progressTimer: Timer? = null
  protected var isReady = false

  constructor(context: Context?) : super(context) {
    init()
  }
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    init()
  }
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    init()
  }

  private fun init() {
    holder.addCallback(this)
    player = MediaPlayer()
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    isReady = true
    player?.setDisplay(holder)
    play(mUrl)
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    player?.setDisplay(null)
    player?.release()
    player = null
    isReady = false
  }

  fun play(url: String?) {
    if (!isReady) {
      return
    }
    player?.reset()
    if (url == null || url.isEmpty()) {
      return
    }
    player?.setDataSource(url)
    player?.prepareAsync()
    player?.setOnPreparedListener(this)
    player?.setOnCompletionListener(this)
    player?.setOnErrorListener(this)
    player?.setOnInfoListener(this)
    player?.setOnSeekCompleteListener(this)
    player?.setOnVideoSizeChangedListener(this)
  }

  fun setUrl(url: String?) {
    mUrl = url
    play(mUrl)
  }

  fun setLoop(loop: Boolean) {
    mLoop = loop
    player?.setLooping(mLoop)
  }

  fun setVolume(volume: Float) {
    mVolume = volume
    player?.setVolume(mVolume, mVolume)
  }

  fun setPaused(paused: Boolean) {
    mPaused = paused
    if (mPaused) {
      player?.pause()
    } else {
      player?.start()
    }
  }

  fun setSeekTo(seekTo: Int) {
    mSeekTo = seekTo
    if (mSeekTo != player?.currentPosition) {
      player?.seekTo(mSeekTo)
    }
  }

  fun setResizeMode(mode: String?) {
    mResizeMode = mode ?: "contain"
    requestLayout()
  }

  fun setProgressUpdateInterval(interval: Int?) {
    mProgressUpdateInterval = interval ?: 250
    progressTimer?.cancel()
    progressTimer = Timer()
    progressTimer?.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        if (player?.isPlaying == true) {
          fireEvent("progress", Arguments.createMap().apply {
            putInt("position", player?.currentPosition ?: 0)
          })
        }
      }
    }, 0, mProgressUpdateInterval.toLong())
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)
    val videoWidth = player?.videoWidth ?: 0
    val videoHeight = player?.videoHeight ?: 0
    val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
    val viewProportion = width.toFloat() / height.toFloat()
    when (mResizeMode) {
      "cover" -> {
        if (videoProportion > viewProportion) {
          setMeasuredDimension(width, (width / videoProportion).toInt())
        } else {
          setMeasuredDimension((height * videoProportion).toInt(), height)
        }
      }
      "contain" -> {
        if (videoProportion > viewProportion) {
          setMeasuredDimension((height * videoProportion).toInt(), height)
        } else {
          setMeasuredDimension(width, (width / videoProportion).toInt())
        }
      }
      else -> {
        setMeasuredDimension(width, height)
      }
    }
  }

  private fun fireEvent(name: String, event: WritableMap?) {
    (context as ReactContext)
      .getJSModule(RCTEventEmitter::class.java)
      .receiveEvent(id, name, event)
  }

  override fun onPrepared(mp: MediaPlayer?) {
    fireEvent("ready", null)
    player?.setLooping(mLoop)
    player?.setVolume(mVolume, mVolume)
    if (mSeekTo > 0) {
      mp?.seekTo(mSeekTo)
    }
    if (!mPaused) {
      mp?.start()
    }
  }

  override fun onCompletion(mp: MediaPlayer?) {
    fireEvent("end", null)
  }

  override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
    when (what) {
      MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
        fireEvent("error", Arguments.createMap().apply {
          putString("message", "MEDIA_ERROR_UNKNOWN")
        })
      }
      MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
        fireEvent("error", Arguments.createMap().apply {
          putString("message", "MEDIA_ERROR_SERVER_DIED")
        })
      }
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
    }
    return false
  }

  override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
    when (what) {
      MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
        fireEvent("bufferingStart", null)
      }
      MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
        fireEvent("bufferingEnd", null)
      }
      MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
        fireEvent("play", null)
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
    requestLayout()
    fireEvent("videoSize", Arguments.createMap().apply {
      putInt("width", width)
      putInt("height", height)
    })
  }

  val duration: Int
    get() = player?.duration ?: 0

  val currentPosition: Int
    get() = player?.currentPosition ?: 0

  val isPlaying: Boolean
    get() = player?.isPlaying ?: false
}
