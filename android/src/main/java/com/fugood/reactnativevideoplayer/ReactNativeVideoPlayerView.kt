package com.fugood.reactnativevideoplayer

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.events.Event
import kotlin.math.max
import kotlin.math.min

/**
 * Wraps [MediaPlayer] in a React Native view.
 *
 * `MediaPlayer` is a strict state machine: the platform documentation lists,
 * for every method, the states it may be called in, and notes that "calling
 * this method in an invalid state transfers the object to the Error state".
 * An object in the Error state is dead until `reset()`. [State] therefore
 * mirrors the documented states exactly and every call is gated on the
 * documented valid set — guessing here silently bricks the player.
 *
 * It also owns scarce global resources (a codec instance and a `Surface`), so
 * every path that can end the view's life — the view being dropped, the host
 * activity going away, the surface type changing, the surface being destroyed —
 * funnels into [releasePlayer] / [release].
 */
class ReactNativeVideoPlayerView
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
) :
  FrameLayout(context, attrs, defStyleAttr),
  SurfaceHolder.Callback,
  TextureView.SurfaceTextureListener,
  LifecycleEventListener,
  MediaPlayer.OnPreparedListener,
  MediaPlayer.OnCompletionListener,
  MediaPlayer.OnErrorListener,
  MediaPlayer.OnInfoListener,
  MediaPlayer.OnSeekCompleteListener,
  MediaPlayer.OnVideoSizeChangedListener {

  /** The states from the `MediaPlayer` documentation, one to one. */
  private enum class State {
    IDLE,
    INITIALIZED,
    PREPARING,
    PREPARED,
    STARTED,
    PAUSED,
    // Unreachable as written: `stop()` deliberately pauses and rewinds rather
    // than calling MediaPlayer.stop(), so the object never enters Stopped. Kept
    // so the enum stays a faithful mirror of the documented state machine.
    STOPPED,
    PLAYBACK_COMPLETED,
    ERROR,
  }

  private val surfaceId = UIManagerHelper.getSurfaceId(context)
  private val container = AspectFrameLayout(context)

  private var videoView: View? = null
  private var textureSurface: Surface? = null
  private var player: MediaPlayer? = null

  private var state = State.IDLE
  private var surfaceReady = false
  private var released = false

  /** Position to restore once the surface and the player are back, in ms. */
  private var resumePositionMs = 0L
  /** Playback was paused by us — host lifecycle or surface loss — not by props. */
  private var playbackSuspended = false
  /** Seek requested before the player could accept one, in ms. `-1` for none. */
  private var pendingSeekMs = -1L

  // Props
  private var uri: String? = null
  private var headers: Map<String, String>? = null
  private var muted = false
  private var volume = 1.0f
  private var paused = false
  private var loop = false
  private var speed = 1.0f
  private var progressUpdateInterval = DEFAULT_PROGRESS_UPDATE_INTERVAL
  private var useTextureView = false

  private val updateProgressTask = Runnable { emitProgressAndReschedule() }

  init {
    val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    params.gravity = Gravity.CENTER
    container.layoutParams = params
    addViewInLayout(container, 0, params)

    // Defer creating the surface view so a `useTextureView` prop arriving in
    // the same commit does not force us to build both kinds.
    post {
      if (videoView == null && !released) {
        setupVideoView()
      }
    }

    (context as ReactContext).addLifecycleEventListener(this)
  }

  // ------------------------------------------------ documented state guards

  /** `getCurrentPosition`, `isPlaying`: valid in every state but Error. */
  private fun canQueryPosition() = state != State.ERROR && state != State.PREPARING

  /** `getDuration`: {Prepared, Started, Paused, Stopped, PlaybackCompleted}. */
  private fun canQueryDuration() =
    state == State.PREPARED ||
      state == State.STARTED ||
      state == State.PAUSED ||
      state == State.STOPPED ||
      state == State.PLAYBACK_COMPLETED

  /** `start`, `seekTo`: {Prepared, Started, Paused, PlaybackCompleted}. */
  private fun canStartOrSeek() =
    state == State.PREPARED ||
      state == State.STARTED ||
      state == State.PAUSED ||
      state == State.PLAYBACK_COMPLETED

  /**
   * `pause`: {Started, Paused, PlaybackCompleted} only. Notably *not* Prepared
   * — a player that was prepared while `paused` was set has never been started,
   * and pausing it would move it to the Error state.
   */
  private fun canPause() =
    state == State.STARTED ||
      state == State.PAUSED ||
      state == State.PLAYBACK_COMPLETED

  /** `setVolume`, `setLooping`: every state but Error. */
  private fun canConfigure() = state != State.ERROR

  // ---------------------------------------------------------------- surface

  private fun setupVideoView() {
    val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    params.gravity = Gravity.CENTER

    detachVideoView()

    videoView =
      if (useTextureView) {
        TextureView(context).apply {
          layoutParams = params
          surfaceTextureListener = this@ReactNativeVideoPlayerView
        }
      } else {
        SurfaceView(context).apply {
          layoutParams = params
          // Draw on top until the first frame is ready, so the window
          // background does not flash through.
          setZOrderOnTop(true)
          holder.addCallback(this@ReactNativeVideoPlayerView)
        }
      }

    container.addView(videoView, 0, params)
  }

  /** Detaches the current video view and drops the surface it owned. */
  private fun detachVideoView() {
    val current = videoView ?: return
    when (current) {
      is SurfaceView -> current.holder.removeCallback(this)
      is TextureView -> current.surfaceTextureListener = null
    }
    container.removeView(current)
    videoView = null
    surfaceReady = false
    releaseTextureSurface()
  }

  private fun releaseTextureSurface() {
    textureSurface?.release()
    textureSurface = null
  }

  /** `setDisplay`/`setSurface`/`setScreenOnWhilePlaying` are valid in any state. */
  private fun attachSurface(mp: MediaPlayer): Boolean {
    when (val view = videoView) {
      is TextureView -> {
        val texture = view.surfaceTexture ?: return false
        releaseTextureSurface()
        val surface = Surface(texture)
        textureSurface = surface
        mp.setSurface(surface)
      }
      is SurfaceView -> {
        mp.setDisplay(view.holder)
        mp.setScreenOnWhilePlaying(true)
      }
      else -> return false
    }
    return true
  }

  override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
    surfaceReady = true
    onSurfaceReady()
  }

  override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

  override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

  override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
    surfaceReady = false
    suspendPlayback()
    player?.setSurface(null)
    releaseTextureSurface()
    // We released our Surface wrapper; let the framework free the texture too.
    return true
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    surfaceReady = true
    onSurfaceReady()
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    surfaceReady = false
    suspendPlayback()
    player?.setDisplay(null)
  }

  private fun onSurfaceReady() {
    if (released) {
      return
    }
    val mp = player
    if (mp != null && state != State.IDLE && state != State.ERROR) {
      // The player survived; hand it the new surface back.
      if (attachSurface(mp) && playbackSuspended) {
        resumeAfterSuspend(mp)
      }
    } else {
      preparePlayer()
    }
  }

  // ----------------------------------------------------------------- player

  private fun preparePlayer() {
    if (released || !surfaceReady) {
      return
    }

    stopProgressUpdates()

    // `reset()` is valid in every state and returns the object to Idle, which
    // is the only state `setDataSource` accepts.
    val mp = player?.also { it.reset() } ?: MediaPlayer().also { player = it }
    state = State.IDLE

    if (!attachSurface(mp)) {
      return
    }

    val source = uri
    if (source.isNullOrEmpty()) {
      return
    }

    // Listeners must be attached before prepareAsync(), otherwise a fast
    // (cached or local) source can finish preparing before we are listening.
    mp.setOnPreparedListener(this)
    mp.setOnCompletionListener(this)
    mp.setOnErrorListener(this)
    mp.setOnInfoListener(this)
    mp.setOnSeekCompleteListener(this)
    mp.setOnVideoSizeChangedListener(this)

    try {
      mp.setDataSource(context, Uri.parse(source), headers)
      state = State.INITIALIZED
      mp.setLooping(loop)
      mp.setVolume(effectiveVolume, effectiveVolume)
      mp.prepareAsync()
      state = State.PREPARING
    } catch (error: Exception) {
      Log.w(TAG, "Failed to open $source", error)
      state = State.ERROR
      emitEvent(ReactVideoErrorEvent(surfaceId, id, error.message ?: "Failed to open the video."))
    }
  }

  private fun releasePlayer() {
    stopProgressUpdates()
    player?.let { mp ->
      mp.setOnPreparedListener(null)
      mp.setOnCompletionListener(null)
      mp.setOnErrorListener(null)
      mp.setOnInfoListener(null)
      mp.setOnSeekCompleteListener(null)
      mp.setOnVideoSizeChangedListener(null)
      // "After release(), you must not interact with the object."
      mp.release()
    }
    player = null
    releaseTextureSurface()
    state = State.IDLE
    playbackSuspended = false
  }

  /** Full teardown. Called when the view is dropped or the activity goes away. */
  fun release() {
    if (released) {
      return
    }
    released = true
    (context as? ReactContext)?.removeLifecycleEventListener(this)
    detachVideoView()
    releasePlayer()
  }

  override fun onDetachedFromWindow() {
    removeCallbacks(updateProgressTask)
    super.onDetachedFromWindow()
  }

  // -------------------------------------------------------------- lifecycle

  private fun suspendPlayback() {
    val mp = player ?: return
    if (state != State.STARTED) {
      return
    }
    playbackSuspended = true
    resumePositionMs = currentPositionMs()
    pausePlayer(mp)
    stopProgressUpdates()
  }

  private fun resumeAfterSuspend(mp: MediaPlayer) {
    playbackSuspended = false
    if (paused) {
      return
    }
    if (resumePositionMs > 0 && canStartOrSeek()) {
      seekPlayerTo(mp, resumePositionMs)
      resumePositionMs = 0L
    }
    startPlayback()
  }

  override fun onHostPause() {
    suspendPlayback()
  }

  override fun onHostResume() {
    val mp = player ?: return
    if (playbackSuspended && surfaceReady) {
      resumeAfterSuspend(mp)
    }
  }

  override fun onHostDestroy() {
    release()
  }

  // -------------------------------------------------------------- transport

  private fun currentPositionMs(): Long =
    if (canQueryPosition()) {
      runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)
    } else {
      0L
    }

  private fun durationMs(): Long =
    if (canQueryDuration()) {
      runCatching { player?.duration?.toLong() ?: 0L }.getOrDefault(0L)
    } else {
      0L
    }

  private fun seekPlayerTo(mp: MediaPlayer, positionMs: Long) {
    if (!canStartOrSeek()) {
      pendingSeekMs = positionMs
      return
    }
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        mp.seekTo(positionMs, MediaPlayer.SEEK_CLOSEST)
      } else {
        mp.seekTo(positionMs.toInt())
      }
    }
  }

  private fun startPlayback() {
    val mp = player ?: return
    if (!canStartOrSeek()) {
      return
    }
    // "After the object is prepared, calling setPlaybackParams with non-zero
    // speed is equivalent to calling start()", so it is only ever applied here,
    // immediately before starting.
    if (speed != 1.0f) {
      runCatching { mp.playbackParams = mp.playbackParams.setSpeed(speed) }
    }
    runCatching { mp.start() }
    state = State.STARTED
    scheduleProgressUpdates()
  }

  private fun pausePlayer(mp: MediaPlayer) {
    if (!canPause()) {
      // Prepared-but-never-started: there is nothing to pause, and calling
      // pause() here would move the player into the Error state.
      return
    }
    runCatching { mp.pause() }
    state = State.PAUSED
  }

  private fun pausePlayback() {
    val mp = player ?: return
    pausePlayer(mp)
    stopProgressUpdates()
    emitProgress()
  }

  // --------------------------------------------------------------- progress

  private fun scheduleProgressUpdates() {
    removeCallbacks(updateProgressTask)
    postDelayed(updateProgressTask, progressUpdateInterval)
  }

  private fun stopProgressUpdates() {
    removeCallbacks(updateProgressTask)
  }

  private fun emitProgress() {
    if (!canQueryDuration()) {
      return
    }
    emitEvent(
      ReactVideoProgressEvent(
        surfaceId,
        id,
        currentPositionMs() / 1000.0,
        durationMs() / 1000.0,
      )
    )
  }

  private fun emitProgressAndReschedule() {
    if (released) {
      return
    }
    emitProgress()
    if (state == State.STARTED) {
      val remaining = max(0L, durationMs() - currentPositionMs())
      val delay =
        if (remaining > 0) min(progressUpdateInterval, remaining) else progressUpdateInterval
      postDelayed(updateProgressTask, max(MIN_PROGRESS_UPDATE_INTERVAL, delay))
    }
  }

  // ------------------------------------------------------------------ props

  fun setUseTextureView(value: Boolean) {
    if (useTextureView == value) {
      return
    }
    useTextureView = value
    releasePlayer()
    setupVideoView()
  }

  fun setSource(url: String?, requestHeaders: Map<String, String>?) {
    if (uri == url && headers == requestHeaders) {
      return
    }
    uri = url
    headers = requestHeaders
    pendingSeekMs = -1L
    resumePositionMs = 0L
    playbackSuspended = false
    if (surfaceReady) {
      preparePlayer()
    }
  }

  fun setLoop(value: Boolean) {
    loop = value
    if (canConfigure()) {
      runCatching { player?.isLooping = value }
    }
  }

  fun setVolume(value: Float) {
    volume = value
    applyVolume()
  }

  fun setMuted(value: Boolean) {
    muted = value
    applyVolume()
  }

  private val effectiveVolume: Float
    get() = if (muted) 0.0f else volume

  private fun applyVolume() {
    if (!canConfigure()) {
      return
    }
    val level = effectiveVolume
    runCatching { player?.setVolume(level, level) }
  }

  fun setPaused(value: Boolean) {
    if (paused == value) {
      return
    }
    paused = value
    if (value) {
      pausePlayback()
    } else {
      startPlayback()
    }
  }

  fun setResizeMode(mode: String?) {
    container.resizeMode = AspectFrameLayout.ResizeMode.from(mode)
  }

  fun setSpeed(value: Float) {
    if (speed == value) {
      return
    }
    speed = value
    // Only safe to push straight through while already started; in any other
    // state a non-zero speed is documented to be equivalent to start().
    if (state == State.STARTED) {
      runCatching { player?.let { it.playbackParams = it.playbackParams.setSpeed(value) } }
    }
  }

  fun setProgressUpdateInterval(interval: Int?) {
    val requested = (interval ?: 0).toLong()
    progressUpdateInterval =
      if (requested > 0) {
        max(MIN_PROGRESS_UPDATE_INTERVAL, requested)
      } else {
        DEFAULT_PROGRESS_UPDATE_INTERVAL
      }
    if (state == State.STARTED) {
      scheduleProgressUpdates()
    }
  }

  // --------------------------------------------------------------- commands

  fun seekTo(positionSeconds: Float) {
    val positionMs = (positionSeconds * 1000).toLong()
    val mp = player
    if (mp == null || !canStartOrSeek()) {
      pendingSeekMs = positionMs
      return
    }
    seekPlayerTo(mp, positionMs)
  }

  fun play() {
    paused = false
    when (state) {
      State.IDLE, State.ERROR, State.STOPPED -> preparePlayer()
      State.PREPARING, State.INITIALIZED -> Unit // starts itself once prepared
      else -> startPlayback()
    }
  }

  fun pause() {
    paused = true
    pausePlayback()
  }

  /**
   * Pauses and rewinds. `MediaPlayer.stop()` is deliberately avoided: it would
   * force a full asynchronous re-prepare before playback could resume, while
   * pause + seek keeps the player usable and restarts instantly.
   */
  fun stop() {
    paused = true
    stopProgressUpdates()
    val mp = player
    if (mp != null) {
      pausePlayer(mp)
      seekPlayerTo(mp, 0L)
    }
    resumePositionMs = 0L
    emitProgress()
  }

  // ----------------------------------------------------------------- events

  // React Native 0.87 deprecated the reactTag argument, but the replacement
  // overload does not exist on the older releases this library supports.
  @Suppress("DEPRECATION")
  private fun emitEvent(event: Event<*>) {
    if (released) {
      return
    }
    UIManagerHelper.getEventDispatcherForReactTag(context as ReactContext, id)
      ?.dispatchEvent(event)
  }

  override fun onPrepared(mp: MediaPlayer) {
    if (released) {
      return
    }
    state = State.PREPARED
    runCatching { mp.isLooping = loop }
    applyVolume()

    // "Prepared" means the media is loaded and playback can begin — the first
    // frame is not on screen yet, that is MEDIA_INFO_VIDEO_RENDERING_START.
    emitEvent(ReactVideoLoadEvent(surfaceId, id))

    val seekTarget = if (pendingSeekMs >= 0) pendingSeekMs else resumePositionMs
    pendingSeekMs = -1L
    resumePositionMs = 0L
    if (seekTarget > 0) {
      seekPlayerTo(mp, seekTarget)
    }

    if (!paused) {
      startPlayback()
    }
    // When `paused` is set we deliberately leave the player in Prepared rather
    // than starting and pausing it; `canPause()` knows not to touch it.
  }

  override fun onCompletion(mp: MediaPlayer) {
    stopProgressUpdates()
    // A looping player restarts internally and never reaches completion.
    state = State.PLAYBACK_COMPLETED
    emitProgress()
    emitEvent(ReactVideoEndEvent(surfaceId, id))
  }

  override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
    stopProgressUpdates()
    state = State.ERROR

    val message =
      when (extra) {
        MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK ->
          "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK"
        MediaPlayer.MEDIA_ERROR_IO -> "MEDIA_ERROR_IO"
        MediaPlayer.MEDIA_ERROR_MALFORMED -> "MEDIA_ERROR_MALFORMED"
        MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "MEDIA_ERROR_UNSUPPORTED"
        MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "MEDIA_ERROR_TIMED_OUT"
        else ->
          if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            "MEDIA_ERROR_SERVER_DIED"
          } else {
            "MEDIA_ERROR_UNKNOWN"
          }
      }
    emitEvent(ReactVideoErrorEvent(surfaceId, id, message))

    if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
      // "the application must release the MediaPlayer object and instantiate a
      // new one."
      releasePlayer()
      preparePlayer()
    }
    // True means handled, which also stops OnCompletionListener from firing.
    return true
  }

  override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
    when (what) {
      MediaPlayer.MEDIA_INFO_BUFFERING_START ->
        emitEvent(ReactVideoBufferEvent(surfaceId, id, true))
      MediaPlayer.MEDIA_INFO_BUFFERING_END ->
        emitEvent(ReactVideoBufferEvent(surfaceId, id, false))
      MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
        // "The player just pushed the very first video frame for rendering."
        // Stop drawing the SurfaceView on top so it composites normally with
        // the rest of the React tree.
        (videoView as? SurfaceView)?.setZOrderOnTop(false)
        emitEvent(ReactVideoReadyEvent(surfaceId, id))
        emitProgress()
      }
    }
    return false
  }

  override fun onSeekComplete(mp: MediaPlayer) {
    emitProgress()
  }

  override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
    container.aspectRatio =
      if (width > 0 && height > 0) width.toFloat() / height.toFloat() else -1.0f
  }

  private companion object {
    const val TAG = "RNVideoPlayerView"
    const val DEFAULT_PROGRESS_UPDATE_INTERVAL = 250L

    /** Faster than one frame at 60fps is pure overhead. */
    const val MIN_PROGRESS_UPDATE_INTERVAL = 16L
  }
}
