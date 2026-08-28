#import "RNVideoPlayerUtils.h"

// AVFoundation has never made this key public, but it is the only supported way
// to attach headers to an AVURLAsset and is used by every RN video library.
static NSString *const kAVURLAssetHTTPHeaderFieldsKey =
    @"AVURLAssetHTTPHeaderFieldsKey";

@implementation RNVideoPlayerUtils

+ (nullable AVPlayerItem *)playerItemWithURI:(nullable NSString *)uri
                                     headers:
                                         (nullable NSDictionary<NSString *, NSString *> *)headers {
  if (uri.length == 0) {
    return nil;
  }

  if ([uri hasPrefix:@"/"]) {
    return [AVPlayerItem
        playerItemWithURL:[NSURL fileURLWithPath:uri isDirectory:NO]];
  }

  NSURL *url = [NSURL URLWithString:uri];
  if (url == nil) {
    return nil;
  }

  NSDictionary *options =
      headers.count > 0 ? @{kAVURLAssetHTTPHeaderFieldsKey : headers} : nil;
  AVURLAsset *asset = [AVURLAsset URLAssetWithURL:url options:options];
  return [AVPlayerItem playerItemWithAsset:asset];
}

@end
