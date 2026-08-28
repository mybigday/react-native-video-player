#import <AVFoundation/AVFoundation.h>
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// Helpers shared by the Fabric and legacy implementations of
/// `ReactNativeVideoPlayerView`.
@interface RNVideoPlayerUtils : NSObject

/// Builds a player item for `uri`. A leading `/` is treated as a local file
/// path, anything else is parsed as a URL. Returns `nil` when `uri` is empty or
/// cannot be parsed.
///
/// `headers` is an optional map of HTTP header fields; it is ignored for local
/// files.
+ (nullable AVPlayerItem *)playerItemWithURI:(nullable NSString *)uri
                                     headers:
                                         (nullable NSDictionary<NSString *, NSString *> *)headers;

@end

NS_ASSUME_NONNULL_END
