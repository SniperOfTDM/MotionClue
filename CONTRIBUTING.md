# Contributing to MotionClue

## Build setup

1. Clone the repository
2. Open in Android Studio Ladybug or later
3. Let Gradle sync (it will download the NDK and CMake automatically if configured in SDK Manager)
4. Connect a physical device (sensors do not work on emulators)
5. Run `./gradlew assembleDebug` (or `./gradlew assembleRelease` for optimized builds)

Required tools:
- Android SDK (API 36 compile SDK, API 26 minimum)
- JDK 17
- Android NDK 28.2.13676358
- CMake 3.22.1
- Gradle 8.9 (wrapper included)

## Recording a demo GIF

To contribute a visual recording for documentation:

1. Enable screen recording on your phone (or run `adb shell screenrecord /sdcard/demo.mp4`).
2. Open MotionClue, start the overlay, and tilt/move the device or ride in a vehicle as a passenger.
3. Keep the clip under 12 seconds.
4. Convert to GIF (e.g., using ffmpeg):
   ```bash
   ffmpeg -i demo.mp4 -vf "fps=20,scale=360:-1:flags=lanczos" -loop 0 docs/media/demo.gif
   ```
5. Place the resulting file at `docs/media/demo.gif`.

## Submitting a sensor inventory

One of the most useful contributions is a sensor inventory from your phone. This helps us understand which sensors are available across different devices.

1. Install MotionClue (build from source or use the debug APK)
2. Open the app and tap "Copy diagnostics"
3. Open a [new issue](https://github.com/SniperOfTDM/MotionClue/issues/new) with the title `[Sensor inventory] <your phone model>`
4. Paste the diagnostics output
5. Include your phone model, ROM name, and Android version

## Code style

- Kotlin: follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- C++: C++17, `snake_case` for variables and functions, `PascalCase` for classes, `-Wall -Wextra -Wpedantic` clean
- No allocations in per-frame or per-sensor-event code paths
- Comments on "why", not "what"

## Manual testing

Before submitting a PR, run through the checklist in [docs/TESTING.md](docs/TESTING.md) on a physical device. There are no automated tests yet.

## Pull requests

- One logical change per PR
- Use [Conventional Commits](https://www.conventionalcommits.org/) for the PR title
- Fill out the PR template
- Describe what you tested and on which device

## License terms

MotionClue is licensed under the GNU General Public License v3.0. All forks, modifications, and derivative works must remain open source under the GPL-3.0 terms.
