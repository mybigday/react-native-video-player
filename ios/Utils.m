#import "Utils.h"
#import <AVFoundation/AVFoundation.h>

@implementation Utils

+ sourceToPlayItem:(NSString *)uri headers:(NSDictionary *)headers
{
  if (uri == nil) {
    return nil;
  }
  NSURL *url = [NSURL URLWithString:uri];
  if (url == nil) {
    return nil;
  }
  AVURLAsset *asset = headers == nil
    ? [AVURLAsset URLAssetWithURL:url options:nil]
    : [AVURLAsset URLAssetWithURL:url options:@{@"AVURLAssetHTTPHeaderFieldsKey": headers}];
  return [AVPlayerItem playerItemWithAsset:asset];
}

+ (id)alloc {
  [NSException raise:@"Cannot be instantiated!" format:@"Static class 'Utils' cannot be instantiated!"];
  return nil;
}

@end
