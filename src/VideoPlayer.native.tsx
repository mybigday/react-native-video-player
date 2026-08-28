import {
  forwardRef,
  useCallback,
  useImperativeHandle,
  useMemo,
  useRef,
} from 'react';
import NativeVideoPlayerView, {
  Commands,
} from './ReactNativeVideoPlayerViewNativeComponent';
import type {
  VideoPlayerComponent,
  VideoPlayerHandle,
  VideoPlayerProps,
} from './types';

type NativeRef = React.ComponentRef<typeof NativeVideoPlayerView>;

const VideoPlayer: VideoPlayerComponent = forwardRef<
  VideoPlayerHandle,
  VideoPlayerProps
>(function VideoPlayerImpl(props, ref) {
  const {
    source,
    onReadyForDisplay,
    onLoad,
    onProgress,
    onEnd,
    onError,
    onBuffer,
    ...rest
  } = props;

  const nativeRef = useRef<NativeRef | null>(null);

  useImperativeHandle(
    ref,
    () => ({
      seek: (position: number) => {
        if (nativeRef.current) {
          Commands.seek(nativeRef.current, position);
        }
      },
      play: () => {
        if (nativeRef.current) {
          Commands.play(nativeRef.current);
        }
      },
      pause: () => {
        if (nativeRef.current) {
          Commands.pause(nativeRef.current);
        }
      },
      stop: () => {
        if (nativeRef.current) {
          Commands.stop(nativeRef.current);
        }
      },
    }),
    []
  );

  // `headers` is declared as `UnsafeMixed` in the codegen spec, so it crosses
  // the bridge untyped. Normalising here keeps the native side able to assume
  // a flat string map.
  const nativeSource = useMemo(() => {
    if (!source) {
      return undefined;
    }
    const { uri, headers } = source;
    if (!headers) {
      return { uri };
    }
    const normalized: Record<string, string> = {};
    for (const [key, value] of Object.entries(headers)) {
      if (value != null) {
        normalized[key] = String(value);
      }
    }
    return { uri, headers: normalized };
  }, [source]);

  const handleReadyForDisplay = useCallback(() => {
    onReadyForDisplay?.();
  }, [onReadyForDisplay]);

  const handleLoad = useCallback(() => {
    onLoad?.();
  }, [onLoad]);

  const handleEnd = useCallback(() => {
    onEnd?.();
  }, [onEnd]);

  const handleProgress = useCallback(
    (event: { nativeEvent: { currentTime: number; duration: number } }) => {
      const { currentTime, duration } = event.nativeEvent;
      onProgress?.({ currentTime, duration });
    },
    [onProgress]
  );

  const handleError = useCallback(
    (event: { nativeEvent: { message: string } }) => {
      onError?.({ message: event.nativeEvent.message });
    },
    [onError]
  );

  const handleBuffer = useCallback(
    (event: { nativeEvent: { isBuffering: boolean } }) => {
      onBuffer?.({ isBuffering: event.nativeEvent.isBuffering });
    },
    [onBuffer]
  );

  return (
    <NativeVideoPlayerView
      {...rest}
      source={nativeSource}
      ref={nativeRef}
      onReadyForDisplay={handleReadyForDisplay}
      onLoad={handleLoad}
      onProgress={handleProgress}
      onEnd={handleEnd}
      onError={handleError}
      onBuffer={handleBuffer}
    />
  );
});

export default VideoPlayer;
