#ifdef RCT_NEW_ARCH_ENABLED
#import "ReactNativeVideoPlayerView.h"

#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/EventEmitters.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/Props.h>
#import <react/renderer/components/RNReactNativeVideoPlayerViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"
#import "Utils.h"

using namespace facebook::react;

@interface ReactNativeVideoPlayerView () <RCTReactNativeVideoPlayerViewViewProtocol>

@end

@implementation ReactNativeVideoPlayerView {
    UIView * _view;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<ReactNativeVideoPlayerViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ReactNativeVideoPlayerViewProps>();
    _props = defaultProps;

    _view = [[UIView alloc] init];

    self.contentView = _view;
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<ReactNativeVideoPlayerViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<ReactNativeVideoPlayerViewProps const>(props);

    if (oldViewProps.color != newViewProps.color) {
        NSString * colorToConvert = [[NSString alloc] initWithUTF8String: newViewProps.color.c_str()];
        [_view setBackgroundColor: [Utils hexStringToColor:colorToConvert]];
    }

    [super updateProps:props oldProps:oldProps];
}

Class<RCTComponentViewProtocol> ReactNativeVideoPlayerViewCls(void)
{
    return ReactNativeVideoPlayerView.class;
}

@end
#endif
