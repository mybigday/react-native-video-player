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

export default forwardRef(function VideoPlayer(props: Props, ref) {
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

  useImperativeHandle(ref, () => ({
    seek: (position: number) =>
      nativeRef.current && Commands.seek(nativeRef.current, position),
    play: () => nativeRef.current && Commands.play(nativeRef.current),
    pause: () => nativeRef.current && Commands.pause(nativeRef.current),
    stop: () => nativeRef.current && Commands.stop(nativeRef.current),
  }));

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
