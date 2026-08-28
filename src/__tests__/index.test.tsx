import { createRef } from 'react';
import { render, fireEvent } from '@testing-library/react-native';

jest.mock('../ReactNativeVideoPlayerViewNativeComponent', () => {
  const { View } = require('react-native');
  return {
    __esModule: true,
    default: View,
    Commands: {
      seek: jest.fn(),
      play: jest.fn(),
      pause: jest.fn(),
      stop: jest.fn(),
    },
  };
});

import VideoPlayer from '../index';
import type { VideoPlayerHandle } from '../index';

const { Commands } = jest.requireMock<{
  Commands: Record<'seek' | 'play' | 'pause' | 'stop', jest.Mock>;
}>('../ReactNativeVideoPlayerViewNativeComponent');

describe('VideoPlayer', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('passes the source through to the native view', async () => {
    const { getByTestId } = await render(
      <VideoPlayer
        testID="player"
        source={{ uri: 'https://example.com/video.mp4' }}
      />
    );

    expect(getByTestId('player').props.source).toEqual({
      uri: 'https://example.com/video.mp4',
    });
  });

  it('normalizes header values to strings', async () => {
    const { getByTestId } = await render(
      <VideoPlayer
        testID="player"
        source={{
          uri: 'https://example.com/video.mp4',
          // Consumers routinely hand us numbers here.
          headers: { Authorization: 'Bearer x', Range: 42 as never },
        }}
      />
    );

    expect(getByTestId('player').props.source.headers).toEqual({
      Authorization: 'Bearer x',
      Range: '42',
    });
  });

  it('omits the headers key when no headers are given', async () => {
    const { getByTestId } = await render(
      <VideoPlayer testID="player" source={{ uri: 'file:///a.mp4' }} />
    );

    expect(getByTestId('player').props.source).not.toHaveProperty('headers');
  });

  it('exposes play/pause/stop/seek through the ref', async () => {
    const ref = createRef<VideoPlayerHandle>();
    await render(<VideoPlayer ref={ref} source={{ uri: 'file:///a.mp4' }} />);

    ref.current?.play();
    ref.current?.pause();
    ref.current?.stop();
    ref.current?.seek(12.5);

    expect(Commands.play).toHaveBeenCalledTimes(1);
    expect(Commands.pause).toHaveBeenCalledTimes(1);
    expect(Commands.stop).toHaveBeenCalledTimes(1);
    expect(Commands.seek).toHaveBeenCalledWith(expect.anything(), 12.5);
  });

  it('unwraps nativeEvent payloads for progress, error and buffer', async () => {
    const onProgress = jest.fn();
    const onError = jest.fn();
    const onBuffer = jest.fn();
    const onLoad = jest.fn();
    const onEnd = jest.fn();
    const onReadyForDisplay = jest.fn();

    const { getByTestId } = await render(
      <VideoPlayer
        testID="player"
        source={{ uri: 'file:///a.mp4' }}
        onProgress={onProgress}
        onError={onError}
        onBuffer={onBuffer}
        onLoad={onLoad}
        onEnd={onEnd}
        onReadyForDisplay={onReadyForDisplay}
      />
    );

    const native = getByTestId('player');

    await fireEvent(native, 'progress', {
      nativeEvent: { currentTime: 1.5, duration: 10 },
    });
    await fireEvent(native, 'error', { nativeEvent: { message: 'boom' } });
    await fireEvent(native, 'buffer', { nativeEvent: { isBuffering: true } });
    await fireEvent(native, 'load', { nativeEvent: {} });
    await fireEvent(native, 'end', { nativeEvent: {} });
    await fireEvent(native, 'readyForDisplay', { nativeEvent: {} });

    expect(onProgress).toHaveBeenCalledWith({ currentTime: 1.5, duration: 10 });
    expect(onError).toHaveBeenCalledWith({ message: 'boom' });
    expect(onBuffer).toHaveBeenCalledWith({ isBuffering: true });
    expect(onLoad).toHaveBeenCalledTimes(1);
    expect(onEnd).toHaveBeenCalledTimes(1);
    expect(onReadyForDisplay).toHaveBeenCalledTimes(1);
  });
});
