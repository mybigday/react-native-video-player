import type { ElementRef } from 'react';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';
import type { ViewProps, HostComponent } from 'react-native';
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

  onSeekTo?: DirectEventHandler<ProgressEvent>;
  onStartBuffering?: DirectEventHandler<null>;
  onEndBuffering?: DirectEventHandler<null>;
  onReady?: DirectEventHandler<null>;
  onLoad?: DirectEventHandler<null>;
  onProgress?: DirectEventHandler<ProgressEvent>;
  onEnd?: DirectEventHandler<null>;
  onError?: DirectEventHandler<ErrorEvent>;
  onVideoSize?: DirectEventHandler<VideoSizeEvent>;
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
