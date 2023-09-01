# @fugood/react-native-video-player

Video player for React Native.
Uses platform owned player API, and pay attention to the performance.

## Installation

```sh
npm install @fugood/react-native-video-player
```

## Usage

```js
import VideoPlayer from "@fugood/react-native-video-player";

// ...

<VideoPlayer
  source={{ uri: "https://www.w3schools.com/html/mov_bbb.mp4" }}
  onReadyForDisplay={() => console.log("ready")}
  onLoad={() => console.log("load")}
  onProgress={() => console.log("progress")}
  onEnd={() => console.log("end")}
  onBuffer={() => console.log("buffering")}
  style={{ width: 300, height: 300 }}
/>;
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

---

<p align="center">
  <a href="https://bricks.tools">
    <img width="90px" src="https://avatars.githubusercontent.com/u/17320237?s=200&v=4">
  </a>
  <p align="center">
    Built and maintained by <a href="https://bricks.tools">BRICKS</a>.
  </p>
</p>
