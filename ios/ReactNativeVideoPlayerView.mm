#import "ReactNativeVideoPlayerView.h"
#import <AVFoundation/AVFoundation.h>
#import "Utils.h"

#ifdef RCT_NEW_ARCH_ENABLED

#import <React/RCTConversions.h>
#import <React/RCTRootComponentView.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/EventEmitters.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/Props.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@interface ReactNativeVideoPlayerView () <RCTReactNativeVideoPlayerViewViewProtocol>
@end

#else

#import <React/RCTBridgeModule.h>
#import <React/RCTEventDispatcher.h>

#endif

static NSString *const STATUS_KEY = @"status";
static NSString *const CURR_STATUS_KEY = @"currentItem.status";
static NSString *const CURR_BUFF_EMPTY_KEY = @"currentItem.playbackBufferEmpty";
static NSString *const CURR_CONTINUE_PLAY_KEY = @"currentItem.playbackLikelyToKeepUp";

@implementation ReactNativeVideoPlayerView {
#ifdef RCT_NEW_ARCH_ENABLED
  UIView *_view;
#else
  RCTBridge *_bridge;
#endif
  BOOL _loop;
  BOOL _paused;
  AVPlayer *_player;
  AVPlayerLayer *_layer;
  id _timeObserver;
}

#pragma mark - Common

- (instancetype)initWithCoder:(NSCoder *)aDecoder
{
  if ((self = [super initWithCoder:aDecoder])) {
    [self initCommon:self];
  }
  return self;
}

- (void)initCommon:(UIView *)view
{
  _player = [[AVPlayer alloc] init];
  _layer = [AVPlayerLayer playerLayerWithPlayer:_player];
  _layer.videoGravity = AVLayerVideoGravityResizeAspect;
  [view.layer addSublayer:_layer];
  view.layer.needsDisplayOnBoundsChange = YES;

  dispatch_async(dispatch_get_main_queue(), ^{
    [self addPlayerObservers];
    [self setProgressUpdateInterval:250];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                          selector:@selector(playerItemDidPlayToEndTime:)
                                          name:AVPlayerItemDidPlayToEndTimeNotification
                                          object:nil];
  });
}

- (void)addPlayerObservers
{
  [_player addObserver:self forKeyPath:STATUS_KEY options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionInitial context:nil];
  [_player addObserver:self forKeyPath:CURR_STATUS_KEY options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionInitial context:nil];
  [_player addObserver:self forKeyPath:CURR_BUFF_EMPTY_KEY options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionInitial context:nil];
  [_player addObserver:self forKeyPath:CURR_CONTINUE_PLAY_KEY options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionInitial context:nil];
}

- (void)removePlayerObservers
{
  [_player removeObserver:self forKeyPath:STATUS_KEY];
  [_player removeObserver:self forKeyPath:CURR_STATUS_KEY];
  [_player removeObserver:self forKeyPath:CURR_BUFF_EMPTY_KEY];
  [_player removeObserver:self forKeyPath:CURR_CONTINUE_PLAY_KEY];
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  [CATransaction begin];
  [CATransaction setAnimationDuration:0];
  _layer.frame = self.bounds;
  [CATransaction commit];
}

- (void)didMoveToSuperview
{
  if (!self.superview) {
    [_player pause];
  } else {
    if (!_paused) {
      [_player play];
    }
  }
}

- (void)removeFromSuperview
{
  [super removeFromSuperview];
#ifndef RCT_NEW_ARCH_ENABLED
  [self _release];
#endif
}

- (void)_release
{
  [[NSNotificationCenter defaultCenter] removeObserver:self];
  [self removePlayerObservers];
  if (_timeObserver) {
    [_player removeTimeObserver:_timeObserver];
    _timeObserver = nil;
  }
  [_player pause];
  [_player replaceCurrentItemWithPlayerItem:nil];
  _player = nil;
  [_layer removeFromSuperlayer];
  _layer = nil;
}

- (void)playerItemDidPlayToEndTime:(NSNotification *)notification
{
  if (_loop) {
    [self seekTo:0];
  } else {
    [self emitOnEnd];
  }
}

- (void)playItem:(AVPlayerItem *)item
{
  [_player replaceCurrentItemWithPlayerItem:item];
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context
{
  dispatch_async(dispatch_get_main_queue(), ^{
    if ([object isKindOfClass:[AVPlayer class]]) {
      AVPlayer *player = (AVPlayer *)object;
      if ([keyPath isEqualToString:STATUS_KEY]) {
        switch ([player status]) {
        case AVPlayerStatusReadyToPlay:
          [self emitOnReady];
          break;
        case AVPlayerStatusFailed:
          [self emitOnError:player.error];
          break;
        case AVPlayerStatusUnknown:
          break;
        }
      } else if ([keyPath isEqualToString:CURR_STATUS_KEY]) {
        switch ([player.currentItem status]) {
        case AVPlayerItemStatusReadyToPlay:
          [self emitOnLoad];
          if (!_paused) {
            [_player play];
          }
          break;
        case AVPlayerItemStatusFailed:
          [self emitOnError:player.currentItem.error];
          break;
        }
      } else if ([keyPath isEqualToString:CURR_BUFF_EMPTY_KEY]) {
        if (player.currentItem.playbackBufferEmpty) {
          [self emitOnBuffer:YES];
        }
      } else if ([keyPath isEqualToString:CURR_CONTINUE_PLAY_KEY]) {
        if (player.currentItem.playbackLikelyToKeepUp) {
          [self emitOnBuffer:NO];
        }
      }
    }
  });
}

#pragma mark - params

- (void)setLayerGravity:(CALayerContentsGravity)gravity
{
  _layer.videoGravity = gravity;
}

- (void)setPaused:(BOOL)paused
{
  _paused = paused;
  if (paused) {
    [_player pause];
  } else {
    [_player play];
  }
}

- (void)setSeek:(Float64)seek
{
  [self seekTo:seek];
}

- (void)setVolume:(float)volume
{
  [_player setVolume:volume];
}

- (void)setSpeed:(float)speed
{
  [_player setRate:speed];
}

- (void)setMuted:(BOOL)muted
{
  [_player setMuted:muted];
}

- (void)setLoop:(BOOL)loop
{
  _loop = loop;
}

- (void)setProgressUpdateInterval:(int)ms
{
  if (_timeObserver) {
    [_player removeTimeObserver:_timeObserver];
  }
  CMTime interval = CMTimeMakeWithSeconds((Float64)ms / 1000.0, NSEC_PER_SEC);
  _timeObserver = [_player addPeriodicTimeObserverForInterval:interval queue:NULL usingBlock:^(CMTime time) {
    CMTime duration = _player.currentItem.duration;
    if (CMTIME_IS_INVALID(duration)) {
      return;
    }
    CMTime currentTime = _player.currentTime;
    Float64 currentTimeSec = CMTimeGetSeconds(currentTime);
    Float64 durationSec = CMTimeGetSeconds(duration);
    [self emitOnProgress:currentTimeSec duration:durationSec];
  }];
}

#pragma mark - commands

- (void)play
{
  [_player play];
}

- (void)pause
{
  [_player pause];
}

- (void)seekTo:(Float64)time
{
  [_player seekToTime:CMTimeMakeWithSeconds(time, NSEC_PER_SEC) completionHandler:^(BOOL finished) {
    if (finished) {
      if (!_paused) {
        [_player play];
      }
    }
  }];
}

- (void)stop
{
  [_player pause];
  [_player seekToTime:kCMTimeZero];
}

#ifdef RCT_NEW_ARCH_ENABLED

#pragma mark - New Arch Impl

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<ReactNativeVideoPlayerViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ReactNativeVideoPlayerViewProps>();
    _props = defaultProps;

    _view = [[UIView alloc] initWithFrame:frame];

    self.contentView = _view;

    [self initCommon:_view];
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<ReactNativeVideoPlayerViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<ReactNativeVideoPlayerViewProps const>(props);

    if (oldViewProps.source.uri != newViewProps.source.uri) {
      NSString * uri = [[NSString alloc] initWithUTF8String: newViewProps.source.uri.c_str()];
      NSDictionary *headers = nil;
      if (newViewProps.source.headers.isObject()) {
        headers = [NSMutableDictionary new];
        for (auto &pair : newViewProps.source.headers.items()) {
          NSString *key = [[NSString alloc] initWithUTF8String: pair.first.c_str()];
          NSString *value = [[NSString alloc] initWithUTF8String: pair.second.c_str()];
          [headers setValue:value forKey:key];
        }
      }
      [self playItem:[Utils sourceToPlayItem:uri headers:headers]];
    }

    if (oldViewProps.paused != newViewProps.paused) {
      [self setPaused:newViewProps.paused];
    }

    if (oldViewProps.seek != newViewProps.seek) {
      [self setSeek:newViewProps.seek];
    }

    if (oldViewProps.volume != newViewProps.volume) {
      [self setVolume:newViewProps.volume];
    }

    if (oldViewProps.speed != newViewProps.speed) {
      [self setSpeed:newViewProps.speed];
    }

    if (oldViewProps.muted != newViewProps.muted) {
      [self setMuted:newViewProps.muted];
    }

    if (oldViewProps.loop != newViewProps.loop) {
      [self setLoop:newViewProps.loop];
    }

    if (oldViewProps.resizeMode != newViewProps.resizeMode) {
      if (newViewProps.resizeMode == "stretch") {
        [self setLayerGravity:AVLayerVideoGravityResize];
      } else if (newViewProps.resizeMode == "cover") {
        [self setLayerGravity:AVLayerVideoGravityResizeAspectFill];
      } else {
        [self setLayerGravity:AVLayerVideoGravityResizeAspect];
      }
    }

    if (oldViewProps.progressUpdateInterval != newViewProps.progressUpdateInterval) {
      [self setProgressUpdateInterval:newViewProps.progressUpdateInterval];
    }

    [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args
{
  if ([commandName isEqualToString:@"play"]) {
    [self play];
  } else if ([commandName isEqualToString:@"pause"]) {
    [self pause];
  } else if ([commandName isEqualToString:@"seek"]) {
    [self seekTo:[args[0] doubleValue]];
  } else if ([commandName isEqualToString:@"stop"]) {
    [self stop];
  }
}

- (void)emitOnReady
{
  if (_eventEmitter) {
    std::dynamic_pointer_cast<const ReactNativeVideoPlayerViewEventEmitter>(_eventEmitter)
      ->onReadyForDisplay(ReactNativeVideoPlayerViewEventEmitter::OnReadyForDisplay{});
  }
}

- (void)emitOnLoad
{
  if (_eventEmitter) {
    std::dynamic_pointer_cast<const ReactNativeVideoPlayerViewEventEmitter>(_eventEmitter)
      ->onLoad(ReactNativeVideoPlayerViewEventEmitter::OnLoad{});
  }
}

- (void)emitOnError:(NSError *)error
{
  if (_eventEmitter) {
    std::dynamic_pointer_cast<const ReactNativeVideoPlayerViewEventEmitter>(_eventEmitter)
      ->onError(ReactNativeVideoPlayerViewEventEmitter::OnError{
        .message = error.localizedDescription.UTF8String,
      });
  }
}

- (void)emitOnEnd
{
  if (_eventEmitter) {
    std::dynamic_pointer_cast<const ReactNativeVideoPlayerViewEventEmitter>(_eventEmitter)
      ->onEnd(ReactNativeVideoPlayerViewEventEmitter::OnEnd{});
  }
}

- (void)emitOnProgress:(Float64)currentTime duration:(Float64)duration
{
  if (_eventEmitter) {
    std::dynamic_pointer_cast<const ReactNativeVideoPlayerViewEventEmitter>(_eventEmitter)
      ->onProgress(ReactNativeVideoPlayerViewEventEmitter::OnProgress{
        .currentTime = currentTime,
        .duration = duration,
      });
  }
}

- (void)emitOnBuffer:(BOOL)buffering
{
  if (_eventEmitter) {
    std::dynamic_pointer_cast<const ReactNativeVideoPlayerViewEventEmitter>(_eventEmitter)
      ->onBuffer(ReactNativeVideoPlayerViewEventEmitter::OnBuffer{
        .isBuffering = buffering,
      });
  }
}

Class<RCTComponentViewProtocol> ReactNativeVideoPlayerViewCls(void)
{
    return ReactNativeVideoPlayerView.class;
}

#else

#pragma mark - Old Arch Impl

- (instancetype)initWithBridge:(RCTBridge *)bridge
{
  if ((self = [super init])) {
    _bridge = bridge;
    [self initCommon:self];
  }
  return self;
}

- (void)setSource:(NSDictionary *)source
{
  if (source == nil) {
    return;
  }
  NSString * uri = [[NSString alloc] initWithUTF8String: [source[@"uri"] UTF8String]];
  NSDictionary *headers = source[@"headers"];
  [self playItem:[Utils sourceToPlayItem:uri headers:headers]];
}

- (void)setResizeMode:(NSString *)resizeMode
{
  if ([resizeMode isEqualToString:@"stretch"]) {
    [self setLayerGravity:AVLayerVideoGravityResize];
  } else if ([resizeMode isEqualToString:@"cover"]) {
    [self setLayerGravity:AVLayerVideoGravityResizeAspectFill];
  } else {
    [self setLayerGravity:AVLayerVideoGravityResizeAspect];
  }
}

- (void)emitOnReady
{
  if (self.onReadyForDisplay) {
    self.onReadyForDisplay(nil);
  }
}

- (void)emitOnLoad
{
  if (self.onLoad) {
    self.onLoad(nil);
  }
}

- (void)emitOnBuffer:(BOOL)buffering
{
  if (self.onBuffer) {
    self.onBuffer(@{
      @"isBuffering": @(buffering),
    });
  }
}

- (void)emitOnError:(NSError *)error
{
  if (self.onError) {
    self.onError(@{
      @"message": error.localizedDescription,
    });
  }
}

- (void)emitOnEnd
{
  if (self.onEnd) {
    self.onEnd(nil);
  }
}

- (void)emitOnProgress:(Float64)currentTime duration:(Float64)duration
{
  if (self.onProgress) {
    self.onProgress(@{
      @"currentTime": @(currentTime),
      @"duration": @(duration),
    });
  }
}

#endif

@end
