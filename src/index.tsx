import React, { useImperativeHandle, forwardRef, useRef } from 'react';
import Player, { Commands } from './ReactNativeVideoPlayerViewNativeComponent';
import type { VideoPlayerProps } from './ReactNativeVideoPlayerViewNativeComponent';
export * from './ReactNativeVideoPlayerViewNativeComponent';

type PlayerRef = InstanceType<typeof Player>;

export default forwardRef(function VideoPlayer(props: VideoPlayerProps, ref) {
  const nativeRef = useRef<PlayerRef>(null);

  useImperativeHandle(ref, () => ({
    seek: (position: number) =>
      nativeRef.current && Commands.seek(nativeRef.current, position),
    play: () => nativeRef.current && Commands.play(nativeRef.current),
    pause: () => nativeRef.current && Commands.pause(nativeRef.current),
    stop: () => nativeRef.current && Commands.stop(nativeRef.current),
  }));

  return <Player {...props} ref={nativeRef} />;
});
