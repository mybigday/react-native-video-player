// The legacy view manager only exists for apps still on the old architecture.
// On Fabric the component is registered through `codegenConfig.ios.components`
// (and `ReactNativeVideoPlayerViewCls` for React Native < 0.74).
#ifndef RCT_NEW_ARCH_ENABLED

#import "ReactNativeVideoPlayerView.h"

#import <React/RCTLog.h>
#import <React/RCTUIManager.h>
#import <React/RCTViewManager.h>

@interface ReactNativeVideoPlayerViewManager : RCTViewManager
@end

@implementation ReactNativeVideoPlayerViewManager

RCT_EXPORT_MODULE(ReactNativeVideoPlayerView)

- (UIView *)view {
  return [[ReactNativeVideoPlayerView alloc] initWithFrame:CGRectZero];
}

RCT_EXPORT_VIEW_PROPERTY(source, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(paused, BOOL)
RCT_EXPORT_VIEW_PROPERTY(seek, double)
RCT_EXPORT_VIEW_PROPERTY(volume, float)
RCT_EXPORT_VIEW_PROPERTY(speed, float)
RCT_EXPORT_VIEW_PROPERTY(muted, BOOL)
RCT_EXPORT_VIEW_PROPERTY(loop, BOOL)
RCT_EXPORT_VIEW_PROPERTY(resizeMode, NSString)
RCT_EXPORT_VIEW_PROPERTY(progressUpdateInterval, NSInteger)

RCT_EXPORT_VIEW_PROPERTY(onReadyForDisplay, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onLoad, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onProgress, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onEnd, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onBuffer, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)

/// Resolves `reactTag` on the UI queue and hands the view to `block`.
- (void)withVideoView:(nonnull NSNumber *)reactTag
                block:(void (^)(ReactNativeVideoPlayerView *view))block {
  [self.bridge.uiManager
      addUIBlock:^(__unused RCTUIManager *uiManager,
                   NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        UIView *view = viewRegistry[reactTag];
        if (![view isKindOfClass:[ReactNativeVideoPlayerView class]]) {
          RCTLogError(@"Invalid view returned from registry, expecting "
                      @"ReactNativeVideoPlayerView, got: %@",
                      view);
          return;
        }
        block((ReactNativeVideoPlayerView *)view);
      }];
}

RCT_EXPORT_METHOD(play : (nonnull NSNumber *)reactTag) {
  [self withVideoView:reactTag
                block:^(ReactNativeVideoPlayerView *view) {
                  [view play];
                }];
}

RCT_EXPORT_METHOD(pause : (nonnull NSNumber *)reactTag) {
  [self withVideoView:reactTag
                block:^(ReactNativeVideoPlayerView *view) {
                  [view pause];
                }];
}

RCT_EXPORT_METHOD(stop : (nonnull NSNumber *)reactTag) {
  [self withVideoView:reactTag
                block:^(ReactNativeVideoPlayerView *view) {
                  [view stop];
                }];
}

RCT_EXPORT_METHOD(seek
                  : (nonnull NSNumber *)reactTag time
                  : (nonnull NSNumber *)time) {
  Float64 seconds = time.doubleValue;
  [self withVideoView:reactTag
                block:^(ReactNativeVideoPlayerView *view) {
                  [view seekTo:seconds];
                }];
}

@end

#endif /* !RCT_NEW_ARCH_ENABLED */
