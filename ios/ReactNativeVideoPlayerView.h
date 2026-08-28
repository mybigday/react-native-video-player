#import <UIKit/UIKit.h>

#ifndef ReactNativeVideoPlayerView_h
#define ReactNativeVideoPlayerView_h

#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTViewComponentView.h>
#else
#import <React/RCTComponent.h>
#import <React/RCTView.h>
#endif

NS_ASSUME_NONNULL_BEGIN

#ifdef RCT_NEW_ARCH_ENABLED

@interface ReactNativeVideoPlayerView : RCTViewComponentView

#else

@interface ReactNativeVideoPlayerView : RCTView

@property (nonatomic, copy, nullable) RCTDirectEventBlock onReadyForDisplay;
@property (nonatomic, copy, nullable) RCTDirectEventBlock onLoad;
@property (nonatomic, copy, nullable) RCTDirectEventBlock onProgress;
@property (nonatomic, copy, nullable) RCTDirectEventBlock onEnd;
@property (nonatomic, copy, nullable) RCTDirectEventBlock onBuffer;
@property (nonatomic, copy, nullable) RCTDirectEventBlock onError;

#endif

- (void)play;
- (void)pause;
- (void)stop;
- (void)seekTo:(Float64)seconds;

@end

NS_ASSUME_NONNULL_END

#endif /* ReactNativeVideoPlayerView_h */
