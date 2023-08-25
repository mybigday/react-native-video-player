import * as React from 'react';

import { StyleSheet, View } from 'react-native';
import { VideoPlayer } from '@fugood/react-native-video-player';

const NUM = 5;

export default function App() {
  const [num, setNum] = React.useState(0);

  const ready = React.useCallback(() => {
    setNum((n) => n + 1);
    console.log(`[ready] ${performance.now()}`);
  }, []);

  React.useEffect(() => {
    console.log(`[render] ${performance.now()}`);

    return () => setNum(0);
  }, []);

  const paused = React.useMemo(() => num < NUM, [num]);

  return (
    <View style={styles.container}>
      {new Array(NUM).fill(null).map((_, i) => (
        <VideoPlayer
          key={i}
          url="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
          style={styles.box}
          onReady={ready}
          progressUpdateInterval={10000}
          onPlay={() => console.log(`[play-${i}] ${performance.now()}`)}
          onProgress={({ nativeEvent: { position } }) =>
            console.log(`[progress-${i}] ${position}`)
          }
          onEnd={() => console.log(`[end-${i}] ${performance.now()}`)}
          onError={({ nativeEvent }) => console.error(nativeEvent)}
          loop
          paused={paused}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 200,
    height: 200,
  },
});
