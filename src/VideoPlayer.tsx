import { forwardRef } from 'react';
import type { VideoPlayerComponent } from './types';

const UNSUPPORTED_MESSAGE =
  "'@fugood/react-native-video-player' is only supported on native platforms.";

/**
 * Web / unsupported-platform stub. Metro resolves `VideoPlayer.native.tsx` on
 * iOS, tvOS and Android, so this file is only ever reached elsewhere.
 */
const VideoPlayer: VideoPlayerComponent = forwardRef(() => {
  throw new Error(UNSUPPORTED_MESSAGE);
});

export default VideoPlayer;
