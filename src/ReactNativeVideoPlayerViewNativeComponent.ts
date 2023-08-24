import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { ViewProps } from 'react-native';

interface NativeProps extends ViewProps {
  url?: string;
  loop?: boolean;
  paused?: boolean;
  volume?: number;
  resizeMode?: 'contain' | 'cover' | 'stretch';
  progressUpdateInterval?: number;
}

export default codegenNativeComponent<NativeProps>('ReactNativeVideoPlayerView');
