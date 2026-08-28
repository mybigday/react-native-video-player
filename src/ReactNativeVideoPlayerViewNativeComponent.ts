import type * as React from 'react';
import { codegenNativeComponent } from 'react-native';
import { codegenNativeCommands } from 'react-native';
import type { ViewProps, HostComponent } from 'react-native';
import type {
  DirectEventHandler,
  Int32,
  Float,
  UnsafeMixed,
  WithDefault,
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

export interface NativeVideoPlayerProps extends ViewProps {
  source?: {
    uri?: string;
    /**
     * Extra HTTP headers sent with the request. Typed as `UnsafeMixed` because
     * codegen has no representation for a string map; the native side reads it
     * as a dictionary of string values.
     */
    headers?: UnsafeMixed;
  };
  loop?: WithDefault<boolean, false>;
  paused?: WithDefault<boolean, false>;
  muted?: WithDefault<boolean, false>;
  volume?: WithDefault<Float, 1.0>;
  seek?: Float;
  resizeMode?: WithDefault<string, 'contain'>;
  speed?: WithDefault<Float, 1.0>;
  progressUpdateInterval?: WithDefault<Int32, 250>;
  /** Android only. Renders into a TextureView instead of a SurfaceView. */
  useTextureView?: WithDefault<boolean, false>;

  onBuffer?: DirectEventHandler<BufferingEvent>;
  onReadyForDisplay?: DirectEventHandler<null>;
  onLoad?: DirectEventHandler<null>;
  onProgress?: DirectEventHandler<ProgressEvent>;
  onEnd?: DirectEventHandler<null>;
  onError?: DirectEventHandler<ErrorEvent>;
}

type ComponentType = HostComponent<NativeVideoPlayerProps>;

interface NativeCommands {
  seek: (viewRef: React.ElementRef<ComponentType>, position: Float) => void;
  play: (viewRef: React.ElementRef<ComponentType>) => void;
  pause: (viewRef: React.ElementRef<ComponentType>) => void;
  stop: (viewRef: React.ElementRef<ComponentType>) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['seek', 'play', 'pause', 'stop'],
});

export default codegenNativeComponent<NativeVideoPlayerProps>(
  'ReactNativeVideoPlayerView'
);
