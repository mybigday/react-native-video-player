import { useCallback, useRef, useState } from 'react';
import {
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import VideoPlayer from '@fugood/react-native-video-player';
import type {
  ResizeMode,
  VideoPlayerHandle,
} from '@fugood/react-native-video-player';

const SOURCES = [
  {
    label: 'Big Buck Bunny',
    uri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
  },
  {
    label: 'Elephants Dream',
    uri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
  },
];

const RESIZE_MODES: ResizeMode[] = ['contain', 'cover', 'stretch'];

// `focused` only exists on react-native-tvos, where it drives the TV focus ring.
type PressableState = { pressed: boolean; focused?: boolean };

function Button({ title, onPress }: { title: string; onPress: () => void }) {
  return (
    <Pressable
      style={(state) => {
        const { pressed, focused } = state as PressableState;
        return [styles.button, (pressed || focused) && styles.buttonActive];
      }}
      onPress={onPress}
    >
      <Text style={styles.buttonLabel}>{title}</Text>
    </Pressable>
  );
}

function formatTime(seconds: number) {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return '0:00';
  }
  const minutes = Math.floor(seconds / 60);
  const rest = Math.floor(seconds % 60);
  return `${minutes}:${String(rest).padStart(2, '0')}`;
}

export default function App() {
  const playerRef = useRef<VideoPlayerHandle>(null);

  const [sourceIndex, setSourceIndex] = useState(0);
  const [resizeMode, setResizeMode] = useState<ResizeMode>('contain');
  const [paused, setPaused] = useState(false);
  const [muted, setMuted] = useState(false);
  const [loop, setLoop] = useState(false);
  const [speed, setSpeed] = useState(1);
  const [progress, setProgress] = useState({ currentTime: 0, duration: 0 });
  const [status, setStatus] = useState('idle');
  const [mounted, setMounted] = useState(true);

  const source = SOURCES[sourceIndex]!;

  const cycle = useCallback(<T,>(values: readonly T[], current: T): T => {
    const next = (values.indexOf(current) + 1) % values.length;
    return values[next]!;
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.stage}>
          {mounted ? (
            <VideoPlayer
              ref={playerRef}
              style={StyleSheet.absoluteFill}
              source={{ uri: source.uri }}
              resizeMode={resizeMode}
              paused={paused}
              muted={muted}
              loop={loop}
              speed={speed}
              progressUpdateInterval={250}
              onReadyForDisplay={() => setStatus('ready')}
              onLoad={() => setStatus('loaded')}
              onProgress={setProgress}
              onBuffer={({ isBuffering }) =>
                setStatus(isBuffering ? 'buffering' : 'playing')
              }
              onEnd={() => setStatus('ended')}
              onError={({ message }) => setStatus(`error: ${message}`)}
            />
          ) : (
            <Text style={styles.placeholder}>player unmounted</Text>
          )}
        </View>

        <Text style={styles.status}>
          {source.label} · {status}
        </Text>
        <Text style={styles.status}>
          {formatTime(progress.currentTime)} / {formatTime(progress.duration)}
        </Text>

        <View style={styles.row}>
          <Button title="Play" onPress={() => playerRef.current?.play()} />
          <Button title="Pause" onPress={() => playerRef.current?.pause()} />
          <Button title="Stop" onPress={() => playerRef.current?.stop()} />
          <Button title="Seek 0" onPress={() => playerRef.current?.seek(0)} />
          <Button
            title="Seek +10s"
            onPress={() => playerRef.current?.seek(progress.currentTime + 10)}
          />
        </View>

        <View style={styles.row}>
          <Button
            title={paused ? 'paused: on' : 'paused: off'}
            onPress={() => setPaused((value) => !value)}
          />
          <Button
            title={muted ? 'muted: on' : 'muted: off'}
            onPress={() => setMuted((value) => !value)}
          />
          <Button
            title={loop ? 'loop: on' : 'loop: off'}
            onPress={() => setLoop((value) => !value)}
          />
          <Button
            title={`speed: ${speed}x`}
            onPress={() => setSpeed((value) => cycle([1, 1.5, 2, 0.5], value))}
          />
          <Button
            title={`resize: ${resizeMode}`}
            onPress={() => setResizeMode((value) => cycle(RESIZE_MODES, value))}
          />
        </View>

        <View style={styles.row}>
          <Button
            title="Next source"
            onPress={() =>
              setSourceIndex((index) => (index + 1) % SOURCES.length)
            }
          />
          {/* Unmounting is the interesting case: it must release the player. */}
          <Button
            title={mounted ? 'Unmount' : 'Mount'}
            onPress={() => setMounted((value) => !value)}
          />
        </View>

        <Text style={styles.footer}>{Platform.OS}</Text>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#101014' },
  content: { padding: 16, gap: 12 },
  stage: {
    aspectRatio: 16 / 9,
    backgroundColor: '#000',
    borderRadius: 8,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
  },
  placeholder: { color: '#6b6b76' },
  status: { color: '#d7d7de', fontSize: 15 },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  button: {
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 6,
    backgroundColor: '#25252e',
  },
  buttonActive: { backgroundColor: '#3d6df0' },
  buttonLabel: { color: '#fff', fontSize: 15 },
  footer: { color: '#6b6b76', fontSize: 13 },
});
