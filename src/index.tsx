import React, {
  useImperativeHandle,
  forwardRef,
  useRef,
  useCallback,
} from 'react';
import Player, { Commands } from './ReactNativeVideoPlayerViewNativeComponent';
import type { VideoPlayerProps } from './ReactNativeVideoPlayerViewNativeComponent';
export * from './ReactNativeVideoPlayerViewNativeComponent';

type PlayerRef = InstanceType<typeof Player>;
type Modify<T, R> = Omit<T, keyof R> & R;

export type Props = Modify<
  VideoPlayerProps,
  {
    onReadyForDisplay?: () => void;
    onLoad?: () => void;
    onProgress?: (event: { currentTime: number; duration: number }) => void;
    onEnd?: () => void;
    onError?: (event: { message: string }) => void;
    onBuffer?: (event: { isBuffering: boolean }) => void;
  }
>;

export type VideoPlayerHandle = {
  seek: (position: number) => void;
  play: () => void;
  pause: () => void;
  stop: () => void;
};

export default forwardRef<VideoPlayerHandle, Props>(function VideoPlayer(
  props,
  ref
) {
  const {
    onReadyForDisplay,
    onLoad,
    onProgress,
    onEnd,
    onError,
    onBuffer,
    ...rest
  } = props;
  const nativeRef = useRef<PlayerRef>(null);

  const seek = useCallback((position: number) => {
    nativeRef.current && Commands.seek(nativeRef.current, position);
  }, []);

  const play = useCallback(() => {
    nativeRef.current && Commands.play(nativeRef.current);
  }, []);

  const pause = useCallback(() => {
    nativeRef.current && Commands.pause(nativeRef.current);
  }, []);

  const stop = useCallback(() => {
    nativeRef.current && Commands.stop(nativeRef.current);
  }, []);

  useImperativeHandle(ref, () => ({ seek, play, pause, stop }), [
    seek,
    play,
    pause,
    stop,
  ]);

  return (
    <Player
      {...rest}
      ref={nativeRef}
      onReadyForDisplay={useCallback(() => {
        onReadyForDisplay && onReadyForDisplay();
      }, [onReadyForDisplay])}
      onLoad={useCallback(() => {
        onLoad && onLoad();
      }, [onLoad])}
      onProgress={useCallback(
        (event) => {
          onProgress && onProgress(event.nativeEvent);
        },
        [onProgress]
      )}
      onEnd={useCallback(() => {
        onEnd && onEnd();
      }, [onEnd])}
      onError={useCallback(
        (event) => {
          onError && onError(event.nativeEvent);
        },
        [onError]
      )}
      onBuffer={useCallback(
        (event) => {
          onBuffer && onBuffer(event.nativeEvent);
        },
        [onBuffer]
      )}
    />
  );
});
