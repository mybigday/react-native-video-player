#import "ReactNativeVideoPlayerView.h"
#import "RNVideoPlayerUtils.h"

#import <AVFoundation/AVFoundation.h>

#include <cmath>

#ifdef RCT_NEW_ARCH_ENABLED

#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/EventEmitters.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/Props.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

#else

#import <React/RCTConvert.h>

#endif

#pragma mark - Constants

// Only key paths that AVFoundation documents as key-value observable are used.
// `AVPlayer.currentItem` is *not* documented as observable, so chained key
// paths through it (`currentItem.status`, `currentItem.playbackBufferEmpty`, …)
// are unsupported; item state is observed on the item itself instead.
static NSString *const kPlayerStatusKeyPath = @"status";
static NSString *const kPlayerTimeControlStatusKeyPath = @"timeControlStatus";
static NSString *const kPlayerWaitingReasonKeyPath = @"reasonForWaitingToPlay";
static NSString *const kLayerReadyForDisplayKeyPath = @"readyForDisplay";
static NSString *const kItemStatusKeyPath = @"status";

/// KVO contexts, so observations belonging to a superclass are never consumed
/// or removed by us.
static void *const kPlayerObserverContext = (void *)&kPlayerObserverContext;
static void *const kLayerObserverContext = (void *)&kLayerObserverContext;
static void *const kItemObserverContext = (void *)&kItemObserverContext;

static const NSInteger kDefaultProgressUpdateInterval = 250;
/// Anything faster than one frame at 60fps is pure overhead.
static const NSInteger kMinimumProgressUpdateInterval = 16;

static const float kDefaultVolume = 1.0f;
static const float kDefaultSpeed = 1.0f;

#pragma mark - Private interface

@interface ReactNativeVideoPlayerView ()
#ifdef RCT_NEW_ARCH_ENABLED
    <RCTReactNativeVideoPlayerViewViewProtocol>
#endif

- (void)emitOnReadyForDisplay;
- (void)emitOnLoad;
- (void)emitOnEnd;
- (void)emitOnError:(nullable NSError *)error;
- (void)emitOnBuffer:(BOOL)isBuffering;
- (void)emitOnProgress:(Float64)currentTime duration:(Float64)duration;

@end

@implementation ReactNativeVideoPlayerView {
  /// The view that hosts `_playerLayer`. `self` on the legacy architecture,
  /// `self.contentView` on Fabric.
  __weak UIView *_hostView;

  AVPlayer *_player;
  AVPlayerLayer *_playerLayer;
  AVPlayerItem *_observedItem;
  id _timeObserver;

  /// Guard the observer registrations so add and remove always happen exactly
  /// once, whichever teardown path runs first.
  BOOL _observingPlayer;
  BOOL _torndown;

  // Props mirrored natively so playback can be restored after a seek, an app
  // foreground, or a Fabric view recycle.
  BOOL _loop;
  BOOL _paused;
  BOOL _muted;
  BOOL _isBuffering;
  BOOL _didEmitReadyForDisplay;
  float _volume;
  float _speed;
  NSInteger _progressUpdateInterval;

#ifdef RCT_NEW_ARCH_ENABLED
  /// Set by `-prepareForRecycle`, which drops the player item without touching
  /// `_props`. Tells the next `-updateProps:oldProps:` to rebuild the item even
  /// though the `source` prop compares equal.
  BOOL _needsSourceReload;
#endif
}

#pragma mark - Setup and teardown

- (void)setUpWithHostView:(UIView *)hostView {
  _hostView = hostView;

  _loop = NO;
  _paused = NO;
  _muted = NO;
  _isBuffering = NO;
  _didEmitReadyForDisplay = NO;
  _volume = kDefaultVolume;
  _speed = kDefaultSpeed;
  _progressUpdateInterval = kDefaultProgressUpdateInterval;
  _torndown = NO;

  _player = [[AVPlayer alloc] init];
  _player.volume = _volume;
  _player.muted = _muted;

  _playerLayer = [AVPlayerLayer playerLayerWithPlayer:_player];
  _playerLayer.videoGravity = AVLayerVideoGravityResizeAspect;
  _playerLayer.frame = hostView.bounds;
  [hostView.layer addSublayer:_playerLayer];
  hostView.layer.needsDisplayOnBoundsChange = YES;

  [self addPlayerObservers];
  [self scheduleTimeObserver];

  [[NSNotificationCenter defaultCenter]
      addObserver:self
         selector:@selector(applicationDidBecomeActive:)
             name:UIApplicationDidBecomeActiveNotification
           object:nil];
}

- (void)dealloc {
  [self teardown];
}

/// Releases every observer and the player itself. Safe to call more than once;
/// after it runs the view no longer plays anything.
- (void)teardown {
  if (_torndown) {
    return;
  }
  _torndown = YES;

  [[NSNotificationCenter defaultCenter] removeObserver:self];
  [self removePlayerObservers];
  [self removeTimeObserver];
  [self detachCurrentItem];

  [_player pause];
  [_player replaceCurrentItemWithPlayerItem:nil];
  _player = nil;

  [_playerLayer removeFromSuperlayer];
  _playerLayer.player = nil;
  _playerLayer = nil;
}

- (void)addPlayerObservers {
  if (_observingPlayer || _player == nil) {
    return;
  }
  _observingPlayer = YES;

  NSKeyValueObservingOptions options =
      NSKeyValueObservingOptionNew | NSKeyValueObservingOptionInitial;

  // Documented as key-value observable on AVPlayer.
  [_player addObserver:self
            forKeyPath:kPlayerStatusKeyPath
               options:options
               context:kPlayerObserverContext];
  [_player addObserver:self
            forKeyPath:kPlayerTimeControlStatusKeyPath
               options:options
               context:kPlayerObserverContext];
  [_player addObserver:self
            forKeyPath:kPlayerWaitingReasonKeyPath
               options:options
               context:kPlayerObserverContext];

  // Documented as key-value observable on AVPlayerLayer: "the first video
  // frame has been made ready for display for the current item".
  [_playerLayer addObserver:self
                 forKeyPath:kLayerReadyForDisplayKeyPath
                    options:options
                    context:kLayerObserverContext];
}

- (void)removePlayerObservers {
  if (!_observingPlayer) {
    return;
  }
  _observingPlayer = NO;

  [_player removeObserver:self
               forKeyPath:kPlayerStatusKeyPath
                  context:kPlayerObserverContext];
  [_player removeObserver:self
               forKeyPath:kPlayerTimeControlStatusKeyPath
                  context:kPlayerObserverContext];
  [_player removeObserver:self
               forKeyPath:kPlayerWaitingReasonKeyPath
                  context:kPlayerObserverContext];
  [_playerLayer removeObserver:self
                    forKeyPath:kLayerReadyForDisplayKeyPath
                       context:kLayerObserverContext];
}

- (void)removeTimeObserver {
  // "Each call to -addPeriodicTimeObserverForInterval:queue:usingBlock: should
  // be paired with a corresponding call to -removeTimeObserver:. Releasing the
  // observer object without a call to -removeTimeObserver: will result in
  // undefined behavior."
  if (_timeObserver != nil) {
    [_player removeTimeObserver:_timeObserver];
    _timeObserver = nil;
  }
}

#pragma mark - Player item lifecycle

- (void)detachCurrentItem {
  if (_observedItem == nil) {
    return;
  }

  [_observedItem removeObserver:self
                     forKeyPath:kItemStatusKeyPath
                        context:kItemObserverContext];

  NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
  [center removeObserver:self
                    name:AVPlayerItemDidPlayToEndTimeNotification
                  object:_observedItem];
  [center removeObserver:self
                    name:AVPlayerItemFailedToPlayToEndTimeNotification
                  object:_observedItem];

  _observedItem = nil;
}

/// Swaps in `item`, moving the per-item observers with it. Passing `nil` clears
/// the current item.
- (void)playItem:(nullable AVPlayerItem *)item {
  if (_torndown) {
    return;
  }

  [self detachCurrentItem];
  _observedItem = item;
  _isBuffering = NO;
  _didEmitReadyForDisplay = NO;

  if (item != nil) {
    // `AVPlayerItem.status` is the documented observable; note that it stops
    // updating once the item is removed from its player, which is why the
    // observer is torn down together with the item.
    [item addObserver:self
           forKeyPath:kItemStatusKeyPath
              options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionInitial
              context:kItemObserverContext];

    NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
    [center addObserver:self
               selector:@selector(playerItemDidPlayToEndTime:)
                   name:AVPlayerItemDidPlayToEndTimeNotification
                 object:item];
    [center addObserver:self
               selector:@selector(playerItemFailedToPlayToEndTime:)
                   name:AVPlayerItemFailedToPlayToEndTimeNotification
                 object:item];
  }

  [_player replaceCurrentItemWithPlayerItem:item];
}

- (void)playerItemDidPlayToEndTime:(NSNotification *)notification {
  if (notification.object != _observedItem) {
    return;
  }
  if (_loop) {
    [self seekTo:0];
  } else {
    [self emitOnEnd];
  }
}

- (void)playerItemFailedToPlayToEndTime:(NSNotification *)notification {
  if (notification.object != _observedItem) {
    return;
  }
  NSError *error =
      notification.userInfo[AVPlayerItemFailedToPlayToEndTimeErrorKey];
  [self emitOnError:error];
}

- (void)applicationDidBecomeActive:(NSNotification *)notification {
  // iOS pauses playback when the app is backgrounded; restore it unless the
  // component asked to stay paused or has since left the screen.
  if (self.window != nil) {
    [self applyPlaybackRate];
  }
}

#pragma mark - Layout

- (void)layoutSubviews {
  [super layoutSubviews];
  UIView *hostView = _hostView ?: self;
  // Resizing a layer is implicitly animated, which lags a frame behind the
  // view it is hosted in.
  [CATransaction begin];
  [CATransaction setDisableActions:YES];
  _playerLayer.frame = hostView.bounds;
  [CATransaction commit];
}

- (void)didMoveToWindow {
  [super didMoveToWindow];
  if (self.window == nil) {
    [_player pause];
  } else {
    [self applyPlaybackRate];
  }
}

#pragma mark - KVO

- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary *)change
                       context:(void *)context {
  if (context == kPlayerObserverContext) {
    [self playerDidChangeValueForKeyPath:keyPath];
  } else if (context == kLayerObserverContext) {
    if (_playerLayer.readyForDisplay && !_didEmitReadyForDisplay) {
      _didEmitReadyForDisplay = YES;
      [self emitOnReadyForDisplay];
    }
  } else if (context == kItemObserverContext) {
    [self itemDidChangeValueForKeyPath:keyPath ofItem:object];
  } else {
    [super observeValueForKeyPath:keyPath
                         ofObject:object
                           change:change
                          context:context];
  }
}

- (void)playerDidChangeValueForKeyPath:(NSString *)keyPath {
  AVPlayer *player = _player;
  if (player == nil) {
    return;
  }

  if ([keyPath isEqualToString:kPlayerStatusKeyPath]) {
    if (player.status == AVPlayerStatusFailed) {
      [self emitOnError:player.error];
    }
    return;
  }

  // `timeControlStatus` and `reasonForWaitingToPlay` together are the
  // documented way to tell "stalled, waiting for data" apart from "paused" and
  // from "briefly evaluating the buffering rate" — Apple explicitly recommends
  // not showing a waiting indicator for the latter.
  BOOL waitingForBuffer =
      player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate &&
      player.reasonForWaitingToPlay == AVPlayerWaitingToMinimizeStallsReason;
  [self emitOnBuffer:waitingForBuffer];
}

- (void)itemDidChangeValueForKeyPath:(NSString *)keyPath
                              ofItem:(AVPlayerItem *)item {
  if (item != _observedItem || ![keyPath isEqualToString:kItemStatusKeyPath]) {
    return;
  }

  switch (item.status) {
  case AVPlayerItemStatusReadyToPlay:
    [self emitOnLoad];
    [self applyPlaybackRate];
    break;
  case AVPlayerItemStatusFailed:
    // A failed item "can no longer be used for playback and a new instance
    // needs to be created in its place". Recovery therefore requires a new
    // `source`, which always builds a fresh item — retrying on this one would
    // silently do nothing.
    [self emitOnError:item.error];
    break;
  case AVPlayerItemStatusUnknown:
    break;
  }
}

#pragma mark - Progress reporting

- (void)scheduleTimeObserver {
  [self removeTimeObserver];

  if (_player == nil) {
    return;
  }

  NSInteger milliseconds =
      MAX(kMinimumProgressUpdateInterval, _progressUpdateInterval);
  CMTime interval =
      CMTimeMakeWithSeconds((Float64)milliseconds / 1000.0, NSEC_PER_SEC);

  // The player retains this block for as long as the observer lives, so it must
  // only ever hold a weak reference back to the view. The queue must be serial;
  // the main queue is where every other mutation of this view happens.
  __weak __typeof(self) weakSelf = self;
  _timeObserver =
      [_player addPeriodicTimeObserverForInterval:interval
                                            queue:dispatch_get_main_queue()
                                       usingBlock:^(CMTime time) {
                                         [weakSelf reportProgressAtTime:time];
                                       }];
}

- (void)reportProgressAtTime:(CMTime)time {
  AVPlayerItem *item = _player.currentItem;
  if (item == nil || !CMTIME_IS_NUMERIC(time)) {
    return;
  }

  Float64 currentTime = CMTimeGetSeconds(time);
  if (!std::isfinite(currentTime)) {
    return;
  }

  // Live streams report an indefinite duration; surface it as 0 rather than NaN.
  CMTime durationTime = item.duration;
  Float64 duration =
      CMTIME_IS_NUMERIC(durationTime) ? CMTimeGetSeconds(durationTime) : 0.0;
  if (!std::isfinite(duration)) {
    duration = 0.0;
  }

  [self emitOnProgress:currentTime duration:duration];
}

#pragma mark - Props

- (void)setResizeModeString:(NSString *)resizeMode {
  AVLayerVideoGravity gravity = AVLayerVideoGravityResizeAspect;
  if ([resizeMode isEqualToString:@"stretch"]) {
    gravity = AVLayerVideoGravityResize;
  } else if ([resizeMode isEqualToString:@"cover"]) {
    gravity = AVLayerVideoGravityResizeAspectFill;
  }
  _playerLayer.videoGravity = gravity;
}

- (void)setPausedValue:(BOOL)paused {
  _paused = paused;
  [self applyPlaybackRate];
}

- (void)setVolumeValue:(float)volume {
  _volume = volume;
  _player.volume = volume;
}

- (void)setSpeedValue:(float)speed {
  _speed = speed;
  [self applyPlaybackRate];
}

- (void)setMutedValue:(BOOL)muted {
  _muted = muted;
  _player.muted = muted;
}

- (void)setLoopValue:(BOOL)loop {
  _loop = loop;
}

- (void)setProgressUpdateIntervalValue:(NSInteger)milliseconds {
  NSInteger interval =
      milliseconds > 0 ? milliseconds : kDefaultProgressUpdateInterval;
  if (interval == _progressUpdateInterval && _timeObserver != nil) {
    return;
  }
  _progressUpdateInterval = interval;
  [self scheduleTimeObserver];
}

/// Single place that decides whether the player should be running, and how
/// fast. Assigning `rate` rather than calling `-play` keeps `speed` honoured;
/// both must run on the main thread before iOS 16.
- (void)applyPlaybackRate {
  if (_player == nil) {
    return;
  }
  if (_paused || _player.currentItem == nil) {
    _player.rate = 0.0f;
  } else {
    _player.rate = _speed;
  }
}

#pragma mark - Commands

- (void)play {
  _paused = NO;
  [self applyPlaybackRate];
}

- (void)pause {
  _paused = YES;
  [self applyPlaybackRate];
}

/// The codegen'd command protocol spells this `seek:`; `seekTo:` is the name
/// the legacy view manager and the public header use.
- (void)seek:(float)position {
  [self seekTo:(Float64)position];
}

- (void)seekTo:(Float64)seconds {
  if (_player == nil) {
    return;
  }
  CMTime time = CMTimeMakeWithSeconds(seconds, NSEC_PER_SEC);
  __weak __typeof(self) weakSelf = self;
  [_player seekToTime:time
      completionHandler:^(BOOL finished) {
        if (finished) {
          [weakSelf applyPlaybackRate];
        }
      }];
}

- (void)stop {
  _paused = YES;
  [_player pause];
  [_player seekToTime:kCMTimeZero];
}

#ifdef RCT_NEW_ARCH_ENABLED

#pragma mark - Fabric

+ (ComponentDescriptorProvider)componentDescriptorProvider {
  return concreteComponentDescriptorProvider<
      ReactNativeVideoPlayerViewComponentDescriptor>();
}

/// Opts this component out of `RCTComponentViewRegistry`'s recycle pool
/// (React Native 0.74+). A pooled view keeps its `AVPlayer`, layer and
/// observers alive for the lifetime of the pool — which only empties on a
/// memory warning — and never receives `-invalidate`. Rebuilding an `AVPlayer`
/// is cheap next to loading an asset, so there is nothing to gain by pooling.
+ (BOOL)shouldBeRecycled {
  return NO;
}

- (instancetype)initWithFrame:(CGRect)frame {
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps =
        std::make_shared<const ReactNativeVideoPlayerViewProps>();
    _props = defaultProps;

    UIView *contentView = [[UIView alloc] initWithFrame:self.bounds];
    contentView.autoresizingMask =
        UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    self.contentView = contentView;

    [self setUpWithHostView:contentView];
  }
  return self;
}

/// Called for unmounted views that will *not* be pooled — the documented place
/// to release resources. With `+shouldBeRecycled` returning `NO` this is the
/// normal end of life for the view.
- (void)invalidate {
  [self teardown];
}

/// Only reached on React Native 0.72/0.73, which pool every component view
/// unconditionally.
///
/// `RCTViewComponentView` diffs incoming props against its cached `_props`, not
/// against the `oldProps` argument (which the mounting layer passes as `nullptr`
/// when a view is inserted). `_props` therefore has to keep describing the
/// view's real state, so this only drops the player item — the one genuinely
/// non-reusable resource — and flags the next prop update to rebuild it.
- (void)prepareForRecycle {
  [super prepareForRecycle];

  [_player pause];
  [self detachCurrentItem];
  [_player replaceCurrentItemWithPlayerItem:nil];

  _isBuffering = NO;
  _didEmitReadyForDisplay = NO;
  _needsSourceReload = YES;
}

- (void)updateProps:(Props::Shared const &)props
           oldProps:(Props::Shared const &)oldProps {
  const auto &oldViewProps =
      *std::static_pointer_cast<ReactNativeVideoPlayerViewProps const>(_props);
  const auto &newViewProps =
      *std::static_pointer_cast<ReactNativeVideoPlayerViewProps const>(props);

  BOOL sourceChanged =
      oldViewProps.source.uri != newViewProps.source.uri ||
      oldViewProps.source.headers != newViewProps.source.headers;

  if (sourceChanged || _needsSourceReload) {
    _needsSourceReload = NO;

    NSString *uri =
        [NSString stringWithUTF8String:newViewProps.source.uri.c_str()];

    NSMutableDictionary<NSString *, NSString *> *headers = nil;
    const auto &rawHeaders = newViewProps.source.headers;
    if (rawHeaders.isObject()) {
      headers = [NSMutableDictionary dictionaryWithCapacity:rawHeaders.size()];
      for (const auto &pair : rawHeaders.items()) {
        if (!pair.first.isString() || !pair.second.isString()) {
          continue;
        }
        NSString *key =
            [NSString stringWithUTF8String:pair.first.asString().c_str()];
        NSString *value =
            [NSString stringWithUTF8String:pair.second.asString().c_str()];
        if (key != nil && value != nil) {
          headers[key] = value;
        }
      }
    }

    [self playItem:[RNVideoPlayerUtils playerItemWithURI:uri headers:headers]];
  }

  if (oldViewProps.loop != newViewProps.loop) {
    [self setLoopValue:newViewProps.loop];
  }

  if (oldViewProps.muted != newViewProps.muted) {
    [self setMutedValue:newViewProps.muted];
  }

  if (oldViewProps.volume != newViewProps.volume) {
    [self setVolumeValue:newViewProps.volume];
  }

  if (oldViewProps.speed != newViewProps.speed) {
    [self setSpeedValue:newViewProps.speed];
  }

  if (oldViewProps.resizeMode != newViewProps.resizeMode) {
    [self setResizeModeString:[NSString stringWithUTF8String:
                                            newViewProps.resizeMode.c_str()]];
  }

  if (oldViewProps.progressUpdateInterval !=
      newViewProps.progressUpdateInterval) {
    [self setProgressUpdateIntervalValue:newViewProps.progressUpdateInterval];
  }

  // `paused` is applied after the source so a view mounted with `paused` set
  // never starts playing, and `seek` last so it wins over both.
  if (oldViewProps.paused != newViewProps.paused) {
    [self setPausedValue:newViewProps.paused];
  }

  if (oldViewProps.seek != newViewProps.seek) {
    [self seekTo:newViewProps.seek];
  }

  [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args {
  RCTReactNativeVideoPlayerViewHandleCommand(self, commandName, args);
}

#pragma mark - Fabric events

- (std::shared_ptr<const ReactNativeVideoPlayerViewEventEmitter>)videoEventEmitter {
  if (!_eventEmitter) {
    return nullptr;
  }
  return std::static_pointer_cast<const ReactNativeVideoPlayerViewEventEmitter>(
      _eventEmitter);
}

- (void)emitOnReadyForDisplay {
  if (auto emitter = [self videoEventEmitter]) {
    emitter->onReadyForDisplay({});
  }
}

- (void)emitOnLoad {
  if (auto emitter = [self videoEventEmitter]) {
    emitter->onLoad({});
  }
}

- (void)emitOnEnd {
  if (auto emitter = [self videoEventEmitter]) {
    emitter->onEnd({});
  }
}

- (void)emitOnError:(nullable NSError *)error {
  if (auto emitter = [self videoEventEmitter]) {
    NSString *message =
        error.localizedDescription ?: @"The video failed to play.";
    emitter->onError({.message = message.UTF8String});
  }
}

- (void)emitOnBuffer:(BOOL)isBuffering {
  if (isBuffering == _isBuffering) {
    return;
  }
  _isBuffering = isBuffering;
  if (auto emitter = [self videoEventEmitter]) {
    emitter->onBuffer({.isBuffering = static_cast<bool>(isBuffering)});
  }
}

- (void)emitOnProgress:(Float64)currentTime duration:(Float64)duration {
  if (auto emitter = [self videoEventEmitter]) {
    emitter->onProgress({
        .currentTime = static_cast<Float>(currentTime),
        .duration = static_cast<Float>(duration),
    });
  }
}

Class<RCTComponentViewProtocol> ReactNativeVideoPlayerViewCls(void) {
  return ReactNativeVideoPlayerView.class;
}

#else

#pragma mark - Legacy architecture

- (instancetype)initWithFrame:(CGRect)frame {
  if (self = [super initWithFrame:frame]) {
    [self setUpWithHostView:self];
  }
  return self;
}

- (instancetype)initWithCoder:(NSCoder *)coder {
  if (self = [super initWithCoder:coder]) {
    [self setUpWithHostView:self];
  }
  return self;
}

- (void)setSource:(NSDictionary *)source {
  if (source == nil) {
    [self playItem:nil];
    return;
  }

  NSString *uri = [RCTConvert NSString:source[@"uri"]];
  NSDictionary *rawHeaders = [RCTConvert NSDictionary:source[@"headers"]];

  NSMutableDictionary<NSString *, NSString *> *headers = nil;
  if (rawHeaders.count > 0) {
    headers = [NSMutableDictionary dictionaryWithCapacity:rawHeaders.count];
    [rawHeaders enumerateKeysAndObjectsUsingBlock:^(
                    id key, id value, __unused BOOL *stop) {
      if ([key isKindOfClass:[NSString class]] && value != nil &&
          value != [NSNull null]) {
        headers[key] = [NSString stringWithFormat:@"%@", value];
      }
    }];
  }

  [self playItem:[RNVideoPlayerUtils playerItemWithURI:uri headers:headers]];
}

- (void)setResizeMode:(NSString *)resizeMode {
  [self setResizeModeString:resizeMode];
}

- (void)setPaused:(BOOL)paused {
  [self setPausedValue:paused];
}

- (void)setSeek:(double)seek {
  [self seekTo:seek];
}

- (void)setVolume:(float)volume {
  [self setVolumeValue:volume];
}

- (void)setSpeed:(float)speed {
  [self setSpeedValue:speed];
}

- (void)setMuted:(BOOL)muted {
  [self setMutedValue:muted];
}

- (void)setLoop:(BOOL)loop {
  [self setLoopValue:loop];
}

- (void)setProgressUpdateInterval:(NSInteger)milliseconds {
  [self setProgressUpdateIntervalValue:milliseconds];
}

#pragma mark - Legacy events

- (void)emitOnReadyForDisplay {
  if (self.onReadyForDisplay) {
    self.onReadyForDisplay(@{});
  }
}

- (void)emitOnLoad {
  if (self.onLoad) {
    self.onLoad(@{});
  }
}

- (void)emitOnEnd {
  if (self.onEnd) {
    self.onEnd(@{});
  }
}

- (void)emitOnError:(nullable NSError *)error {
  if (self.onError) {
    self.onError(@{
      @"message" : error.localizedDescription ?: @"The video failed to play.",
    });
  }
}

- (void)emitOnBuffer:(BOOL)isBuffering {
  if (isBuffering == _isBuffering) {
    return;
  }
  _isBuffering = isBuffering;
  if (self.onBuffer) {
    self.onBuffer(@{@"isBuffering" : @(isBuffering)});
  }
}

- (void)emitOnProgress:(Float64)currentTime duration:(Float64)duration {
  if (self.onProgress) {
    self.onProgress(@{
      @"currentTime" : @(currentTime),
      @"duration" : @(duration),
    });
  }
}

#endif

@end
