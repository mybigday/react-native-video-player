import type * as React from 'react';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';
import type { ViewProps, HostComponent } from 'react-native';
import type {
  DirectEventHandler,
  Int32,
  Float,
  // @ts-ignore
  UnsafeMixed,
} from 'react-native/Libraries/Types/CodegenTypes';

export type ProgressEvent = Readonly<{
  currentTime: Float;
  duration: Float;
}>;

export type ErrorEvent = Readonly<{
  message: string;
}>;

export type BufferingEvent = Readonly<{
  isBuffering: boolean;
}>;

export interface VideoPlayerProps extends ViewProps {
  source?: {
    uri?: string;
    headers?: UnsafeMixed;
  };
  loop?: boolean;
  paused?: boolean;
  muted?: boolean;
  volume?: Float;
  seek?: Float;
  resizeMode?: string;
  speed?: Float;
  progressUpdateInterval?: Int32;

  onBuffer?: DirectEventHandler<BufferingEvent>;
  onReadyForDisplay?: DirectEventHandler<null>;
  onLoad?: DirectEventHandler<null>;
  onProgress?: DirectEventHandler<ProgressEvent>;
  onEnd?: DirectEventHandler<null>;
  onError?: DirectEventHandler<ErrorEvent>;
}

type ComponentType = HostComponent<VideoPlayerProps>;

interface NativeCommands {
  seek: (viewRef: React.ElementRef<ComponentType>, position: Float) => void;
  play: (viewRef: React.ElementRef<ComponentType>) => void;
  pause: (viewRef: React.ElementRef<ComponentType>) => void;
  stop: (viewRef: React.ElementRef<ComponentType>) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['seek', 'play', 'pause', 'stop'],
});

export default codegenNativeComponent<VideoPlayerProps>(
  'ReactNativeVideoPlayerView'
);
