import * as React from 'react';

import { StyleSheet, View } from 'react-native';
import { VideoPlayer } from '@fugood/react-native-video-player';

export default function App() {
  return (
    <View style={styles.container}>
      {new Array(5).fill(0).map((_, i) => (
        <VideoPlayer
          key={i}
          url="http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
          style={styles.box}
          loop={true}
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
