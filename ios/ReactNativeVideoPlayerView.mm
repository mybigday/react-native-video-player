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
  AVPlayerLooper *_looper;
  id _timeObserver;
}

#pragma mark - Common

- (void)initCommon:(UIView*) view
{
  _player = [[AVPlayer alloc] init];
  [_player addObserver:self forKeyPath:@"status"
           options:(NSKeyValueObservingOptionNew | NSKeyValueObservingOptionInitial)
           context:nil];
  [_player addObserver:self forKeyPath:@"currentItem.status"
           options:(NSKeyValueObservingOptionNew | NSKeyValueObservingOptionInitial)
           context:nil];
  [_player addObserver:self forKeyPath:@"currentItem.playbackBufferEmpty"
           options:NSKeyValueObservingOptionNew|NSKeyValueObservingOptionInitial
           context:nil];
  [_player addObserver:self forKeyPath:@"currentItem.playbackLikelyToKeepUp"
           options:NSKeyValueObservingOptionNew|NSKeyValueObservingOptionInitial
           context:nil];
  _layer = [AVPlayerLayer playerLayerWithPlayer:_player];
  _layer.contentsGravity = kCAGravityResizeAspect;
  [view.layer addSublayer:_layer];
  _layer.frame = view.bounds;

  [[NSNotificationCenter defaultCenter] addObserver:self
                                        selector:@selector(playerItemDidPlayToEndTime:)
                                        name:AVPlayerItemDidPlayToEndTimeNotification
                                        object:nil];

  [self setupProgressUpdate:250];
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  _layer.frame = self.bounds;
}

-(void)dealloc
{
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)playerItemDidPlayToEndTime:(NSNotification *)notification
{
  if (_loop) {
    [_player seekToTime:kCMTimeZero];
    [_player play];
  } else {
    [self emitOnEnd];
  }
}

- (void)playItem:(AVPlayerItem *)item
{
  [_player replaceCurrentItemWithPlayerItem:item];
}

- (void)setupProgressUpdate:(int)ms
{
  if (_timeObserver) {
    [_player removeTimeObserver:_timeObserver];
  }
  CMTime interval = CMTimeMakeWithSeconds(ms / 1000.0, NSEC_PER_MSEC);
  _timeObserver = [_player addPeriodicTimeObserverForInterval:interval queue:NULL usingBlock:^(CMTime time) {
    CMTime duration = _player.currentItem.duration;
    if (CMTIME_IS_INVALID(duration)) {
      return;
    }
    CMTime currentTime = _player.currentTime;
    float currentTimeSec = CMTimeGetSeconds(currentTime);
    float durationSec = CMTimeGetSeconds(duration);
    [self emitOnProgress:currentTimeSec duration:durationSec];
  }];
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context
{
  if ([object isKindOfClass:[AVPlayer class]]) {
    AVPlayer *player = (AVPlayer *)object;
    if ([keyPath isEqualToString:@"status"]) {
      switch ([player status]) {
      case AVPlayerStatusReadyToPlay:
        NSLog(@"AVPlayerStatusReadyToPlay");
        [self emitOnReady];
        break;
      case AVPlayerStatusFailed:
        NSLog(@"AVPlayerStatusFailed: %@", player.error);
        [self emitOnError:player.error];
        break;
      }
    } else if ([keyPath isEqualToString:@"currentItem.status"]) {
      switch ([player.currentItem status]) {
      case AVPlayerItemStatusReadyToPlay:
        NSLog(@"AVPlayerItemStatusReadyToPlay");
        [self emitOnLoad];
        if (!_paused) {
          [_player play];
        }
        break;
      case AVPlayerItemStatusFailed:
        NSLog(@"AVPlayerItemStatusFailed: %@", player.currentItem.error);
        [self emitOnError:player.currentItem.error];
        break;
      }
    } else if ([keyPath isEqualToString:@"currentItem.playbackBufferEmpty"]) {
      if (player.currentItem.playbackBufferEmpty) {
        [self emitOnBuffer:YES];
      }
    } else if ([keyPath isEqualToString:@"currentItem.playbackLikelyToKeepUp"]) {
      if (player.currentItem.playbackLikelyToKeepUp) {
        [self emitOnBuffer:NO];
      }
    }
    return;
  } else if ([object isKindOfClass:[AVPlayerItem class]]) {
    AVPlayerItem *item = (AVPlayerItem *)object;
  } else {
    [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
  }
}

- (void)play
{
  [_player play];
}

- (void)pause
{
  [_player pause];
}

- (void)seek:(float)time
{
  [_player seekToTime:CMTimeMakeWithSeconds(time, NSEC_PER_MSEC)];
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

    [self initCommon: _view];
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
      _paused = newViewProps.paused;
      if (newViewProps.paused) {
        [_player pause];
      } else {
        [_player play];
      }
    }

    if (oldViewProps.seek != newViewProps.seek) {
      [_player seekToTime:CMTimeMakeWithSeconds(newViewProps.seek, NSEC_PER_SEC)];
    }

    if (oldViewProps.volume != newViewProps.volume) {
      [_player setVolume:newViewProps.volume];
    }

    if (oldViewProps.speed != newViewProps.speed) {
      [_player setRate:newViewProps.speed];
    }

    if (oldViewProps.muted != newViewProps.muted) {
      [_player setMuted:newViewProps.muted];
    }

    if (oldViewProps.loop != newViewProps.loop) {
      _loop = newViewProps.loop;
    }

    if (oldViewProps.resizeMode != newViewProps.resizeMode) {
      if (newViewProps.resizeMode == "stretch") {
        _layer.contentsGravity = kCAGravityResize;
      } else if (newViewProps.resizeMode == "cover") {
        _layer.contentsGravity = kCAGravityResizeAspectFill;
      } else {
        _layer.contentsGravity = kCAGravityResizeAspect;
      }
    }

    if (oldViewProps.progressUpdateInterval != newViewProps.progressUpdateInterval) {
      [self setupProgressUpdate:newViewProps.progressUpdateInterval];
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
    [self seek:[args[0] floatValue]];
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

- (void)emitOnProgress:(float)position duration:(float)duration
{
  if (_eventEmitter) {
    std::dynamic_pointer_cast<const ReactNativeVideoPlayerViewEventEmitter>(_eventEmitter)
      ->onProgress(ReactNativeVideoPlayerViewEventEmitter::OnProgress{
        .position = position,
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

- (void)setPaused:(BOOL)paused
{
  _paused = paused;
  if (paused) {
    [_player pause];
  } else {
    [_player play];
  }
}

- (void)setSeek:(float)seek
{
  [_player seekToTime:CMTimeMakeWithSeconds(seek, NSEC_PER_SEC)];
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

- (void)setResizeMode:(NSString *)resizeMode
{
  if ([resizeMode isEqualToString:@"stretch"]) {
    _layer.contentsGravity = kCAGravityResize;
  } else if ([resizeMode isEqualToString:@"cover"]) {
    _layer.contentsGravity = kCAGravityResizeAspectFill;
  } else {
    _layer.contentsGravity = kCAGravityResizeAspect;
  }
}

- (void)setProgressUpdateInterval:(int)progressUpdateInterval
{
  [self setupProgressUpdate:progressUpdateInterval];
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

- (void)emitOnProgress:(float)position duration:(float)duration
{
  if (self.onProgress) {
    self.onProgress(@{
      @"position": @(position),
      @"duration": @(duration),
    });
  }
}

#endif

@end
