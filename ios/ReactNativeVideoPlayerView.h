// This guard prevent this file to be compiled in the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTViewComponentView.h>
#import <UIKit/UIKit.h>

#ifndef ReactNativeVideoPlayerViewNativeComponent_h
#define ReactNativeVideoPlayerViewNativeComponent_h

NS_ASSUME_NONNULL_BEGIN

@interface ReactNativeVideoPlayerView : RCTViewComponentView

- (void)play;
- (void)pause;
- (void)stop;
- (void)seekTo:(Float64)time;

@end

NS_ASSUME_NONNULL_END

#endif /* ReactNativeVideoPlayerViewNativeComponent_h */

#else

#import <React/RCTView.h>

#ifndef ReactNativeVideoPlayerViewNativeComponent_h
#define ReactNativeVideoPlayerViewNativeComponent_h

@class RCTBridge;

@interface ReactNativeVideoPlayerView : RCTView

@property(nonatomic, copy) RCTDirectEventBlock onReadyForDisplay;
@property(nonatomic, copy) RCTDirectEventBlock onLoad;
@property(nonatomic, copy) RCTDirectEventBlock onProgress;
@property(nonatomic, copy) RCTDirectEventBlock onEnd;
@property(nonatomic, copy) RCTDirectEventBlock onBuffer;
@property(nonatomic, copy) RCTDirectEventBlock onError;

- (instancetype)initWithBridge:(RCTBridge *)bridge NS_DESIGNATED_INITIALIZER;

- (void)play;
- (void)pause;
- (void)stop;
- (void)seekTo:(Float64)time;

@end

#endif /* ReactNativeVideoPlayerViewNativeComponent_h */
#endif /* RCT_NEW_ARCH_ENABLED */
