import type { ForwardRefExoticComponent, RefAttributes } from 'react';
import type { ViewProps } from 'react-native';

/**
 * How the video is scaled inside the view bounds.
 *
 * - `contain` (default): fit inside the bounds, preserving aspect ratio.
 * - `cover`: fill the bounds, preserving aspect ratio, cropping the overflow.
 * - `stretch`: fill the bounds, ignoring aspect ratio.
 */
export type ResizeMode = 'contain' | 'cover' | 'stretch';

export type VideoPlayerSource = {
  /**
   * Remote URL, or an absolute file system path (a leading `/` is treated as a
   * local file).
   */
  uri?: string;
  /** Extra HTTP headers sent with the request. Ignored for local files. */
  headers?: Record<string, string>;
};

export type ProgressEvent = {
  /** Playback position, in seconds. */
  currentTime: number;
  /** Total duration, in seconds. `0` for streams with unknown duration. */
  duration: number;
};

export type ErrorEvent = {
  message: string;
};

export type BufferingEvent = {
  isBuffering: boolean;
};

export type VideoPlayerProps = ViewProps & {
  source?: VideoPlayerSource;
  /** Restart automatically when playback reaches the end. */
  loop?: boolean;
  paused?: boolean;
  muted?: boolean;
  /** `0.0` – `1.0`. Defaults to `1.0`. */
  volume?: number;
  /** Seek to this position (in seconds) whenever the value changes. */
  seek?: number;
  resizeMode?: ResizeMode;
  /** Playback rate. Defaults to `1.0`. */
  speed?: number;
  /** How often `onProgress` fires, in milliseconds. Defaults to `250`. */
  progressUpdateInterval?: number;
  /**
   * Android only. Render into a `TextureView` instead of a `SurfaceView`.
   * `TextureView` composites like a regular view (so it can be transformed and
   * overlapped) at the cost of extra GPU work; `SurfaceView` is cheaper and is
   * the default.
   */
  useTextureView?: boolean;

  /** The first frame is ready to be shown. */
  onReadyForDisplay?: () => void;
  /** The media has been loaded and playback can start. */
  onLoad?: () => void;
  onProgress?: (event: ProgressEvent) => void;
  onEnd?: () => void;
  onError?: (event: ErrorEvent) => void;
  onBuffer?: (event: BufferingEvent) => void;
};

/** @deprecated Use {@link VideoPlayerProps}. */
export type Props = VideoPlayerProps;

export type VideoPlayerHandle = {
  /** Seek to `position`, in seconds. */
  seek: (position: number) => void;
  play: () => void;
  pause: () => void;
  /** Pause and rewind to the beginning. */
  stop: () => void;
};

/**
 * The type of the exported component. Declared here so the native
 * implementation and the unsupported-platform stub stay structurally
 * identical — TypeScript always resolves the non-platform-specific file.
 */
export type VideoPlayerComponent = ForwardRefExoticComponent<
  VideoPlayerProps & RefAttributes<VideoPlayerHandle>
>;
