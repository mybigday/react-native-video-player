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
import android.view.View
import android.view.TextureView
import android.view.Surface
import android.view.SurfaceView
import android.view.SurfaceHolder
import android.view.View.MeasureSpec
import android.view.Gravity
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.widget.FrameLayout
import android.net.Uri
import android.util.Log

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.uimanager.UIManagerModule

class ReactNativeVideoPlayerView : FrameLayout, SurfaceHolder.Callback, TextureView.SurfaceTextureListener, LifecycleEventListener,
  OnPreparedListener, OnCompletionListener, OnErrorListener, OnInfoListener, OnSeekCompleteListener, OnVideoSizeChangedListener {

  enum class State {
    IDLE,
    PREPARING,
    PREPARED,
    PLAYING,
    PAUSED,
    STOPPED,
    COMPLETED
  }

  protected var mUrl: String? = null
  protected var mHeaders: Map<String, String>? = null
  protected var mMuted = false
  protected var mVolume = 1.0f
  protected var mPaused = false
  protected var mSeekTo = 0L
  protected var mLoop = false
  protected var mProgressUpdateInterval = 250L
  protected var mUseTextureView = false
  protected val mPlaybackParams = PlaybackParams()
  // State
  protected var mState = State.IDLE
  protected var mPosition = 0L
  protected var mBackgroundPaused = false

  protected val container = AspectFrameLayout(context)
  protected var videoView: View? = null
  protected var player: MediaPlayer? = MediaPlayer()
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
    val aspectParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    aspectParams.gravity = Gravity.CENTER

    // defer create view to avoid switching view type
    post {
      if (videoView == null) {
        setupVideoView()
      }
    }
    container.layoutParams = aspectParams
    addViewInLayout(container, 0, aspectParams)
    (context as ReactContext).addLifecycleEventListener(this)
  }

  private fun setupVideoView() {
    val params = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    params.gravity = Gravity.CENTER

    if (videoView != null) {
      container.removeView(videoView)
    }

    if (mUseTextureView) {
      Log.d("ReactNativeVideoPlayerView", "Using TextureView")
      val view = TextureView(context)
      view.layoutParams = params
      view.surfaceTextureListener = this

      videoView = view
    } else {
      Log.d("ReactNativeVideoPlayerView", "Using SurfaceView")
      val view = SurfaceView(context)
      view.layoutParams = params
      view.setZOrderOnTop(true)
      view.holder.addCallback(this)

      videoView = view
    }
    container.addView(videoView, 0, params)
  }

  private fun moveToBackground() {
    mBackgroundPaused = true
    mPosition = player?.currentPosition?.toLong() ?: 0L
    player?.pause()
  }

  private fun resume() {
    if (mBackgroundPaused) {
      mBackgroundPaused = false
      if (!mUseTextureView) {
        player?.setDisplay((videoView as SurfaceView).holder)
      }
      if (mState == State.PLAYING) {
        player?.seekTo(mPosition, MediaPlayer.SEEK_CLOSEST)
      }
    }
  }

  override fun onHostPause() {
    if (mUseTextureView) {
      moveToBackground()
    }
  }

  override fun onHostDestroy() {
    release()
  }

  override fun onHostResume() {
    if (mUseTextureView) {
      resume()
    }
  }

  override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
    isReady = true
    initPlayer()
  }

  override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
  }

  override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
  }

  override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
    player?.setSurface(null)
    return true
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    isReady = true
    if (!mBackgroundPaused) {
      initPlayer()
    } else {
      resume()
    }
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    moveToBackground()
    player?.setDisplay(null)
  }

  fun release() {
    (context as ReactContext).removeLifecycleEventListener(this)
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
    } else {
      player!!.reset()
    }
    if (mUseTextureView) {
      player!!.setSurface(Surface((videoView as TextureView).surfaceTexture))
    } else {
      player!!.setDisplay((videoView as SurfaceView).holder)
      player!!.setScreenOnWhilePlaying(true)
    }
    if (mUrl?.isEmpty() == false) {
      mState = State.PREPARING
      player!!.setDataSource(context, Uri.parse(mUrl), mHeaders)
      player!!.prepareAsync()
      player!!.setOnPreparedListener(this)
      player!!.setOnCompletionListener(this)
      player!!.setOnErrorListener(this)
      player!!.setOnInfoListener(this)
      player!!.setOnSeekCompleteListener(this)
      player!!.setOnVideoSizeChangedListener(this)
    } else {
      mState = State.IDLE
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
      val nextDelay = Math.min(
        mProgressUpdateInterval,
        player?.duration?.minus(player?.currentPosition ?: 0)?.toLong() ?: Long.MAX_VALUE
      )
      postDelayed(updateProgressTask, nextDelay)
    }
  }

  fun setUseTextureView(useTextureView: Boolean) {
    if (mUseTextureView != useTextureView) {
      mUseTextureView = useTextureView
      isReady = false
      setupVideoView()
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
    val value = enumValueOf<AspectFrameLayout.ResizeMode>((mode ?: "").uppercase())
    if (value != null) {
      container.resizeMode = value
    }
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
    post {
      player?.seekTo((position * 1000).toLong(), MediaPlayer.SEEK_CLOSEST)
    }
  }

  fun play() {
    if (!isReady) {
      return
    }
    post {
      if (mState == State.STOPPED || mState == State.IDLE) {
        initPlayer()
      } else {
        player?.start()
        updateProgress()
        mState = State.PLAYING
      }
    }
  }

  fun pause() {
    if (!isReady) {
      return
    }
    post {
      player?.pause()
      removeCallbacks(updateProgressTask)
      updateProgress()
      mState = State.PAUSED
    }
  }

  fun stop() {
    if (!isReady) {
      return
    }
    post {
      player?.stop()
      removeCallbacks(updateProgressTask)
      updateProgress()
      mState = State.STOPPED
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
    mState = State.PREPARED
    mp.setPlaybackParams(mPlaybackParams)
    mp.setLooping(mLoop)
    mp.setVolume(volume, volume)
    fireEvent("ready", null)
    if (mSeekTo > 0) {
      if (!mPaused) {
        mState = State.PLAYING
      }
      mp.seekTo(mSeekTo, MediaPlayer.SEEK_CLOSEST)
    } else {
      mp.start()
      if (mPaused) {
        mp.pause()
      } else {
        mState = State.PLAYING
      }
    }
    if (!mUseTextureView) {
      (videoView as SurfaceView).setZOrderOnTop(false)
    }
  }

  override fun onCompletion(mp: MediaPlayer?) {
    removeCallbacks(updateProgressTask)
    fireEvent("end", null)
    mState = State.COMPLETED
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
    if (mState == State.PLAYING) {
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
