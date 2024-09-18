import * as React from 'react';

import { StyleSheet, View, Text, Pressable } from 'react-native';
import VideoPlayer from '@fugood/react-native-video-player';
import type { VideoPlayerHandle } from '@fugood/react-native-video-player';

type ButtonProps = React.ComponentProps<typeof Pressable> & {
  title: string;
};

function Button({ title, onPress }: ButtonProps) {
  return (
    <Pressable
      style={({ pressed }) => [
        {
          backgroundColor: pressed ? 'rgb(210, 230, 255)' : 'white',
        },
        styles.wrapperCustom,
      ]}
      onPress={onPress}
    >
      <Text style={styles.text}>{title}</Text>
    </Pressable>
  );
}

export default function App() {
  const videoRef = React.useRef<VideoPlayerHandle>(null);
  const [info, setInfo] = React.useState({
    currentTime: 0,
    duration: 0,
  });
  const [showVideo, setShowVideo] = React.useState(false);

  return (
    <View style={styles.container}>
      {showVideo && (
        <VideoPlayer
          ref={videoRef}
          source={{
            uri: 'https://www.w3schools.com/html/mov_bbb.mp4',
          }}
          style={styles.box}
          onReadyForDisplay={() => console.log(`[ready] ${performance.now()}`)}
          onLoad={() => console.log(`[load] ${performance.now()}`)}
          onProgress={setInfo}
          onEnd={() => console.log(`[end] ${performance.now()}`)}
          onError={(err) => console.error(err)}
        />
      )}
      <Text style={styles.info}>currentTime: {info.currentTime}</Text>
      <Text style={styles.info}>duration: {info.duration}</Text>
      <Button
        title="Play"
        onPress={() => {
          videoRef.current?.play();
        }}
      />
      <Button
        title="Pause"
        onPress={() => {
          videoRef.current?.pause();
        }}
      />
      <Button
        title="Stop"
        onPress={() => {
          videoRef.current?.stop();
        }}
      />
      <Button
        title="Seek to 0"
        onPress={() => {
          videoRef.current?.seek(0);
        }}
      />
      <Button
        title="Show Video"
        onPress={() => {
          setShowVideo(true);
        }}
      />
      <Button
        title="Hide Video"
        onPress={() => {
          setShowVideo(false);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'white',
  },
  box: {
    width: 200,
    height: 200,
  },
  wrapperCustom: {
    borderRadius: 8,
    padding: 6,
    marginBottom: 3,
  },
  text: {
    fontSize: 16,
  },
  info: {
    fontSize: 16,
    marginBottom: 3,
  },
});
