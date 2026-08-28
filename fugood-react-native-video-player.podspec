require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "fugood-react-native-video-player"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  # `min_supported_versions` comes from React Native's CocoaPods helpers, which
  # the app's Podfile has already loaded (available since react-native 0.71).
  # It reports `{ ios: }` for react-native and `{ ios:, tvos: }` for
  # react-native-tvos, so this pod follows whichever fork the app installs.
  #
  # Do not guard this with `respond_to?`: the helpers are top-level Ruby
  # methods, which are *private* on Object, so `respond_to?` reports false and
  # the pod would silently lose its tvOS platform — and with it, linkage into
  # Apple TV apps.
  s.platforms    = min_supported_versions

  s.source       = {
    :git => "https://github.com/mybigday/react-native-video-player.git",
    :tag => "v#{s.version}"
  }

  s.source_files         = "ios/**/*.{h,m,mm}"
  s.private_header_files = "ios/**/*.h"
  s.frameworks           = "AVFoundation", "CoreMedia"

  s.resource_bundles = {
    "fugood-react-native-video-player-privacy" => ["ios/PrivacyInfo.xcprivacy"]
  }

  # Wires up the React Native dependencies (and defines RCT_NEW_ARCH_ENABLED on
  # the new architecture). Available since react-native 0.71.
  install_modules_dependencies(s)
end
