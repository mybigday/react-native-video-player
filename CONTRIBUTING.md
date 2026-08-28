# Contributing

Contributions are always welcome, no matter how large or small!

We want this community to be friendly and respectful to each other. Please follow it in all your interactions with the project. Before contributing, please read the [code of conduct](./CODE_OF_CONDUCT.md).

## Development workflow

To get started with the project, run `yarn` in the root directory to install the required dependencies for each package:

```sh
yarn
```

This is a Yarn 4 workspace, managed through [Corepack](https://nodejs.org/api/corepack.html) — the `packageManager` field in `package.json` pins the exact version, so `yarn` uses the right release without a global install. Node 22 or newer is required (see `.nvmrc`).

### The two example apps

React Native's CocoaPods setup only supports one platform per app, so Apple TV cannot live in the same project as iPhone. There are therefore two examples:

| Workspace | React Native | Architecture | Platforms |
| --- | --- | --- | --- |
| [`example/`](/example/) | `react-native` 0.87 | New (Fabric) | iOS, Android |
| [`example-tv/`](/example-tv/) | `react-native-tvos` 0.87 | New (Fabric) | tvOS, Android TV |
| [`example-legacy/`](/example-legacy/) | `react-native` 0.81 | Legacy (Paper) | iOS, Android |

All three render the same screen — `example-tv/src/App.tsx` and `example-legacy/src/App.tsx` re-export `example/src/App.tsx` — so a change to the demo shows up everywhere.

`example-legacy` is pinned to 0.81 on purpose: React Native 0.82's Gradle plugin force-enables the New Architecture and logs an error for `newArchEnabled=false`, so 0.81 is the last release in which the legacy renderer can be built at all. It is the only coverage the `android/src/oldarch/` source set and the `#ifndef RCT_NEW_ARCH_ENABLED` half of `ios/` get.

Changes to the library's JavaScript are picked up without a rebuild; native changes need a rebuild.

```sh
yarn example start          # Metro, for the mobile example
yarn example android
yarn example ios            # run `yarn example pods` first

yarn example-tv start       # Metro, for the TV example
yarn example-tv android     # Android TV emulator
yarn example-tv tvos        # Apple TV simulator, after `yarn example-tv pods`

yarn example-legacy start   # Metro, for the legacy-renderer example
yarn example-legacy android
yarn example-legacy ios     # run `yarn example-legacy pods` first
```

`yarn example-legacy pods` sets `RCT_NEW_ARCH_ENABLED=0`, and `example-legacy/android/gradle.properties` sets `newArchEnabled=false`; both are what select the legacy code paths.

If you are switching architectures or React Native versions, clear the build folders first:

```sh
yarn clean
```

Make sure your code passes TypeScript and ESLint. Run the following to verify:

```sh
yarn typecheck
yarn lint
```

To fix formatting errors, run the following:

```sh
yarn lint --fix
```

Remember to add tests for your change if possible. Run the unit tests by:

```sh
yarn test
```

To edit the Objective-C files, open `example/ios/ReactNativeVideoPlayerExample.xcworkspace` in Xcode and find the sources under `Pods > Development Pods > fugood-react-native-video-player`.

To edit the Kotlin files, open `example/android` in Android Studio and find the sources under `fugood-react-native-video-player`.


### Commit message convention

We follow the [conventional commits specification](https://www.conventionalcommits.org/en) for our commit messages:

- `fix`: bug fixes, e.g. fix crash due to deprecated method.
- `feat`: new features, e.g. add new method to the module.
- `refactor`: code refactor, e.g. migrate from class components to hooks.
- `docs`: changes into documentation, e.g. add usage example for the module..
- `test`: adding or updating tests, e.g. add integration tests using detox.
- `chore`: tooling changes, e.g. change CI config.

Our pre-commit hooks verify that your commit message matches this format when committing.

### Linting and tests

[ESLint](https://eslint.org/), [Prettier](https://prettier.io/), [TypeScript](https://www.typescriptlang.org/)

We use [TypeScript](https://www.typescriptlang.org/) for type checking, [ESLint](https://eslint.org/) with [Prettier](https://prettier.io/) for linting and formatting the code, and [Jest](https://jestjs.io/) for testing.

Our pre-commit hooks verify that the linter and tests pass when committing.

### Publishing to npm

We use [release-it](https://github.com/release-it/release-it) to make it easier to publish new versions. It handles common tasks like bumping version based on semver, creating tags and releases etc.

To publish new versions, run the following:

```sh
yarn release
```

### Scripts

The `package.json` file contains various scripts for common tasks:

- `yarn typecheck`: type-check files with TypeScript.
- `yarn lint`: lint files with ESLint.
- `yarn test`: run unit tests with Jest.
- `yarn prepare`: build the publishable package with `react-native-builder-bob`.
- `yarn clean`: remove all build folders.
- `yarn example <script>` / `yarn example-tv <script>`: run a script in an example workspace.
- `yarn turbo run build:android` (and `build:ios`, `build:androidtv`, `build:tvos`, `build:android-legacy`, `build:ios-legacy`): the compile-only builds CI runs. Each task name is owned by exactly one example workspace — `turbo run` fans a name out across every workspace, so a duplicated name would start two Gradle builds at once. That matters because React Native's autolinking requires the library's Gradle output to stay at `android/build`, which every example shares; **do not build two examples concurrently**.

### Sending a pull request

> **Working on your first pull request?** You can learn how from this _free_ series: [How to Contribute to an Open Source Project on GitHub](https://app.egghead.io/playlists/how-to-contribute-to-an-open-source-project-on-github).

When you're sending a pull request:

- Prefer small pull requests focused on one change.
- Verify that linters and tests are passing.
- Review the documentation to make sure it looks good.
- Follow the pull request template when opening a pull request.
- For pull requests that change the API or implementation, discuss with maintainers first by opening an issue.
