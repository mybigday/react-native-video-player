# @fugood/react-native-video-player

Video player for React Native, built directly on each platform's own player API
— `AVPlayer` on iOS and Apple TV, `MediaPlayer` on Android and Android TV. No
bundled media engine, so it adds no meaningful size to your app; in exchange,
codec and streaming support is whatever the OS provides.

| | Supported |
| --- | --- |
| React Native | 0.72 – 0.87 |
| Architecture | New (Fabric) and legacy (Paper) |
| Platforms | iOS, Android, tvOS and Android TV (via [`react-native-tvos`](https://github.com/react-native-tvos/react-native-tvos)) |
| Minimum OS | iOS/tvOS per your React Native version, Android API 24 |

## Installation

```sh
npm install @fugood/react-native-video-player
# or
yarn add @fugood/react-native-video-player
```

iOS and tvOS:

```sh
cd ios && pod install
```

Nothing else is required — the library is autolinked, and the podspec follows
whichever React Native fork your app uses, so an app built on
`react-native-tvos` picks up the Apple TV platform automatically.

## Usage

```tsx
import { useRef } from 'react';
import VideoPlayer from '@fugood/react-native-video-player';
import type { VideoPlayerHandle } from '@fugood/react-native-video-player';

function Player() {
  const ref = useRef<VideoPlayerHandle>(null);

  return (
    <VideoPlayer
      ref={ref}
      style={{ width: 320, height: 180 }}
      source={{ uri: 'https://www.w3schools.com/html/mov_bbb.mp4' }}
      resizeMode="contain"
      onLoad={() => ref.current?.play()}
      onProgress={({ currentTime, duration }) =>
        console.log(currentTime, duration)
      }
      onError={({ message }) => console.warn(message)}
    />
  );
}
```

## Props

All [`View` props](https://reactnative.dev/docs/view#props) are supported, plus:

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `source` | `{ uri?: string; headers?: Record<string, string> }` | – | Remote URL, or an absolute file path (a leading `/` is read as a local file). `headers` is ignored for local files. |
| `paused` | `boolean` | `false` | Pauses playback without unloading the media. A view mounted with `paused` set still renders the first frame, so it doubles as a poster. |
| `muted` | `boolean` | `false` | |
| `volume` | `number` | `1.0` | `0.0` – `1.0`. Independent of `muted`. |
| `loop` | `boolean` | `false` | Restarts on reaching the end; `onEnd` does not fire while looping. |
| `speed` | `number` | `1.0` | Playback rate. |
| `resizeMode` | `'contain' \| 'cover' \| 'stretch'` | `'contain'` | |
| `seek` | `number` | – | Seeks to this position, in seconds, whenever the value changes. Prefer the imperative `seek()`. |
| `progressUpdateInterval` | `number` | `250` | `onProgress` interval in milliseconds. Clamped to a minimum of 16ms. |
| `useTextureView` | `boolean` | `false` | **Android only.** Render into a `TextureView` instead of a `SurfaceView`. A `TextureView` composites like a normal view, so it can be transformed, animated and overlapped, at the cost of extra GPU work. |

## Events

| Prop | Payload | Fires when |
| --- | --- | --- |
| `onLoad` | – | The media is loaded and playback can start. Nothing is on screen yet. |
| `onReadyForDisplay` | – | The first video frame is on screen. Always after `onLoad`. |
| `onProgress` | `{ currentTime, duration }` | Every `progressUpdateInterval` while playing. Both values are in seconds; `duration` is `0` for live streams. |
| `onBuffer` | `{ isBuffering }` | Playback stalls waiting for data, and again when it recovers. Momentary stalls the platform expects to recover from on its own are not reported. |
| `onEnd` | – | Playback reaches the end (not while `loop` is set). |
| `onError` | `{ message }` | Loading or playback fails. |

## Imperative API

Attach a `ref` to get a `VideoPlayerHandle`:

| Method | Description |
| --- | --- |
| `play()` | |
| `pause()` | |
| `stop()` | Pauses and rewinds to the beginning. |
| `seek(seconds)` | |

## Notes and limitations

- **Formats** are whatever the platform decodes. `AVPlayer` handles HLS out of
  the box; Android's `MediaPlayer` supports progressive MP4 and HLS but not
  DASH. If you need DASH, widevine or precise adaptive streaming control, use a
  player built on ExoPlayer instead.
- **Backgrounding.** Both platforms suspend playback when the app is
  backgrounded, and the view resumes it on return unless `paused` is set.
- **Recovering from `onError`.** A failed item cannot be retried in place on
  either platform; set a new `source` (a different object, even with the same
  `uri`) to rebuild the player.
- **Android surfaces.** The default `SurfaceView` is considerably cheaper, but
  it keeps showing the previous video's last frame when you change `source`,
  until the new one renders. Switch to `useTextureView` if that matters, or if
  you need to animate, transform or overlap the video.

## Contributing

See the [contributing guide](CONTRIBUTING.md) for the development workflow.

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
