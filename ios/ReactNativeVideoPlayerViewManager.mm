#import <React/RCTViewManager.h>
#import <React/RCTUIManager.h>
#import "RCTBridge.h"
#import "ReactNativeVideoPlayerView.h"

@interface ReactNativeVideoPlayerViewManager : RCTViewManager
@end

@implementation ReactNativeVideoPlayerViewManager

RCT_EXPORT_MODULE(ReactNativeVideoPlayerView)

- (UIView *)view
{
#ifdef RCT_NEW_ARCH_ENABLED
  return [[ReactNativeVideoPlayerView alloc] init];
#else
  return [[ReactNativeVideoPlayerView alloc] initWithBridge:self.bridge];
#endif
}

RCT_EXPORT_VIEW_PROPERTY(source, NSDictionary);
RCT_EXPORT_VIEW_PROPERTY(paused, BOOL);
RCT_EXPORT_VIEW_PROPERTY(seek, Float64);
RCT_EXPORT_VIEW_PROPERTY(volume, float);
RCT_EXPORT_VIEW_PROPERTY(speed, float);
RCT_EXPORT_VIEW_PROPERTY(muted, BOOL);
RCT_EXPORT_VIEW_PROPERTY(loop, BOOL);
RCT_EXPORT_VIEW_PROPERTY(resizeMode, NSString);
RCT_EXPORT_VIEW_PROPERTY(progressUpdateInterval, int);

RCT_EXPORT_VIEW_PROPERTY(onReadyForDisplay, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onLoad, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onProgress, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onEnd, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onBuffer, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock);

RCT_EXPORT_METHOD(play:(nonnull NSNumber *)reactTag)
{
  [self.bridge.uiManager prependUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
    ReactNativeVideoPlayerView *view = (ReactNativeVideoPlayerView *)viewRegistry[reactTag];
    if (![view isKindOfClass:[ReactNativeVideoPlayerView class]]) {
      RCTLogError(@"Invalid view returned from registry, expecting ReactNativeVideoPlayerView, got: %@", view);
    } else {
      [view play];
    }
  }];
}

RCT_EXPORT_METHOD(pause:(nonnull NSNumber *)reactTag)
{
  [self.bridge.uiManager prependUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
    ReactNativeVideoPlayerView *view = (ReactNativeVideoPlayerView *)viewRegistry[reactTag];
    if (![view isKindOfClass:[ReactNativeVideoPlayerView class]]) {
      RCTLogError(@"Invalid view returned from registry, expecting ReactNativeVideoPlayerView, got: %@", view);
    } else {
      [view pause];
    }
  }];
}

RCT_EXPORT_METHOD(seek:(nonnull NSNumber *)reactTag time:(nonnull NSNumber *)time)
{
  [self.bridge.uiManager prependUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
    ReactNativeVideoPlayerView *view = (ReactNativeVideoPlayerView *)viewRegistry[reactTag];
    if (![view isKindOfClass:[ReactNativeVideoPlayerView class]]) {
      RCTLogError(@"Invalid view returned from registry, expecting ReactNativeVideoPlayerView, got: %@", view);
    } else {
      [view seekTo:[time doubleValue]];
    }
  }];
}

RCT_EXPORT_METHOD(stop:(nonnull NSNumber *)reactTag)
{
  [self.bridge.uiManager prependUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
    ReactNativeVideoPlayerView *view = (ReactNativeVideoPlayerView *)viewRegistry[reactTag];
    if (![view isKindOfClass:[ReactNativeVideoPlayerView class]]) {
      RCTLogError(@"Invalid view returned from registry, expecting ReactNativeVideoPlayerView, got: %@", view);
    } else {
      [view stop];
    }
  }];
}

@end
