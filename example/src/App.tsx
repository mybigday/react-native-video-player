import * as React from 'react';

import { StyleSheet, View } from 'react-native';
import VideoPlayer from '@fugood/react-native-video-player';

export default function App() {
  const [i, setI] = React.useState(0);

  const next = React.useCallback(() => {
    setI((n) => n + 1);
    setTimeout(() => setI((n) => n + 1), 1000);
  }, []);

  return (
    <View style={styles.container}>
      {i % 2 === 0 && (
        <VideoPlayer
          key={i}
          source={{
            uri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
          }}
          style={styles.box}
          progressUpdateInterval={5000}
          onReadyForDisplay={() =>
            console.log(`[ready-${i}] ${performance.now()}`)
          }
          onLoad={() => console.log(`[load-${i}] ${performance.now()}`)}
          onProgress={({ nativeEvent: { position, duration } }) =>
            console.log(`[progress-${i}] ${position} / ${duration}`)
          }
          onEnd={next}
          onError={({ nativeEvent }) => console.error(nativeEvent)}
        />
      )}
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
