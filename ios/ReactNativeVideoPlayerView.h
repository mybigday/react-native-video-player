// This guard prevent this file to be compiled in the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTViewComponentView.h>
#import <UIKit/UIKit.h>

#ifndef ReactNativeVideoPlayerViewNativeComponent_h
#define ReactNativeVideoPlayerViewNativeComponent_h

NS_ASSUME_NONNULL_BEGIN

// Dummy block type for interop compatibility
typedef void (^RCTDirectEventBlock)(NSDictionary *_Nullable body);

@interface ReactNativeVideoPlayerView : RCTViewComponentView

// Props for Legacy View Manager Interop compatibility
// These are handled via updateProps in New Architecture but must be declared
// for the interop layer which expects individual setters
@property(nonatomic, copy, nullable) NSDictionary *source;
@property(nonatomic, assign) BOOL paused;
@property(nonatomic, assign) Float64 seek;
@property(nonatomic, assign) float volume;
@property(nonatomic, assign) float speed;
@property(nonatomic, assign) BOOL muted;
@property(nonatomic, assign) BOOL loop;
@property(nonatomic, copy, nullable) NSString *resizeMode;
@property(nonatomic, assign) int progressUpdateInterval;

// Event callbacks for Legacy View Manager Interop compatibility
// These are not used in New Architecture (event emitters are used instead)
// but must be declared to avoid crashes when interop layer tries to set them
@property(nonatomic, copy, nullable) RCTDirectEventBlock onReadyForDisplay;
@property(nonatomic, copy, nullable) RCTDirectEventBlock onLoad;
@property(nonatomic, copy, nullable) RCTDirectEventBlock onProgress;
@property(nonatomic, copy, nullable) RCTDirectEventBlock onEnd;
@property(nonatomic, copy, nullable) RCTDirectEventBlock onBuffer;
@property(nonatomic, copy, nullable) RCTDirectEventBlock onError;

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
