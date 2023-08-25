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
  }, []);

  return (
    <View style={styles.container}>
      {new Array(NUM).fill().map((_, i) => (
        <VideoPlayer
          key={i}
          url="http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
          style={styles.box}
          onReady={ready}
          onError={console.error}
          loop
          paused={num < NUM}
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
