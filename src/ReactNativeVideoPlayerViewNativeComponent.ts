import type { ElementRef } from 'react';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';
import type { ViewProps, HostComponent } from 'react-native';
import type { DirectEventHandler } from 'react-native/Libraries/Types/CodegenTypes';

export type ProgressEvent = Readonly<{
  position: number;
  duration: number;
}>;

export type ErrorEvent = Readonly<{
  message: number;
}>;

export type BufferingEvent = Readonly<{
  isBuffering: boolean;
}>;

export interface VideoPlayerProps extends ViewProps {
  source?: {
    uri?: string;
    headers?: { [key: string]: string };
  };
  loop?: boolean;
  paused?: boolean;
  muted?: boolean;
  volume?: number;
  resizeMode?: 'contain' | 'cover' | 'stretch';
  speed?: number;
  progressUpdateInterval?: number;

  onBuffer?: DirectEventHandler<BufferingEvent>;
  onReadyForDisplay?: DirectEventHandler<null>;
  onLoad?: DirectEventHandler<null>;
  onProgress?: DirectEventHandler<ProgressEvent>;
  onEnd?: DirectEventHandler<null>;
  onError?: DirectEventHandler<ErrorEvent>;
}

type ComponentType = HostComponent<VideoPlayerProps>;

interface NativeCommands {
  seek: (viewRef: ElementRef<ComponentType>, position: number) => void;
  play: (viewRef: ElementRef<ComponentType>) => void;
  pause: (viewRef: ElementRef<ComponentType>) => void;
  stop: (viewRef: ElementRef<ComponentType>) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['seek', 'play', 'pause', 'stop'],
});

export default codegenNativeComponent<VideoPlayerProps>(
  'ReactNativeVideoPlayerView'
);
