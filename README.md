# Daemon launcher

Daemon launcher is an ultra-minimalist Android home screen designed to let you launch apps using swipe gestures and button presses. It is built to be *efficient, fast, and completely free of distraction*.

Daemon launcher only displays the date, time, and your wallpaper. There are no grids of app icons or complex widgets cluttering your home screen by default. Instead, everything is hidden behind intuitive gestures.

## Features

Pressing back or swiping up (fully configurable) opens a list of all installed apps, which can be searched efficiently with a single tap.

**The following gestures are available:**
 - Volume up / down
 - Swipe up / down / left / right
 - Swipe with two fingers
 - Swipe on the left / right resp. top / bottom edge
 - Tap, then swipe up / down / left / right
 - Draw < / > / V / Λ
 - Click on date / time
 - Double click
 - Long click
 - Back button

**To every gesture you can bind one of the following actions:**
 - Launch a specific app
 - Open a list of all / favorite / private apps
 - Open Daemon launcher settings
 - Toggle private space lock
 - Lock the screen
 - Toggle the flashlight
 - Volume up / down
 - Go to previous / next audio track
 - **Flow-Inspired Focus Mode:** Set apps as "distracting" to trigger a mindful pause screen before launching, helping you break the habit of mindless scrolling.

Daemon launcher is also compatible with the [Android Work Profile](https://www.android.com/enterprise/work-profile/).

## Installation

You can clone this repository and build the project using Android Studio or Gradle.

```bash
git clone https://github.com/catamsp/IDK-launcher.git
cd IDK-launcher
./gradlew assembleDebug
```

## Contributing

If you find a bug or have an idea for a new feature, feel free to open an issue or submit a pull request!
- Create a fork of this repository.
- Create a new branch named `feature/<your-feature>` or `fix/<your-fix>`.
- Commit your changes and open a pull request.

## License

This project is open-source and licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---
*Daemon launcher is a fork and evolution of the original µLauncher project, rebranded and customized by catamsp.*
