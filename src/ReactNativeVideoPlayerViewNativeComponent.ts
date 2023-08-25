import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { ViewProps } from 'react-native';
import type { DirectEventHandler } from 'react-native/Libraries/Types/CodegenTypes';

export type ProgressEvent = Readonly<{
  position: number;
}>;

export type ErrorEvent = Readonly<{
  message: number;
}>;

export type VideoSizeEvent = Readonly<{
  width: number;
  height: number;
}>;

interface NativeProps extends ViewProps {
  url?: string;
  loop?: boolean;
  paused?: boolean;
  volume?: number;
  resizeMode?: 'contain' | 'cover' | 'stretch';
  progressUpdateInterval?: number;

  onSeekTo?: DirectEventHandler<ProgressEvent>;
  onStartBuffering?: DirectEventHandler<null>;
  onEndBuffering?: DirectEventHandler<null>;
  onReady?: DirectEventHandler<null>;
  onPlay?: DirectEventHandler<null>;
  onProgress?: DirectEventHandler<ProgressEvent>;
  onEnd?: DirectEventHandler<null>;
  onError?: DirectEventHandler<ErrorEvent>;
  onVideoSize?: DirectEventHandler<VideoSizeEvent>;
}

export default codegenNativeComponent<NativeProps>(
  'ReactNativeVideoPlayerView'
);
