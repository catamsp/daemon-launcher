+++
title = 'Differences to the original Daemon launcher'
+++

# Notable changes compared to Finn's Daemon launcher

Daemon launcher is a fork of [finnmglas's app Daemon launcher](https://github.com/finnmglas/Daemon launcher).
Here is an incomplete list of changes:
<!--The last commit of the original project is [340ee731](https://github.com/catamsp/launcher/commit/340ee7315293b028c060638e058516435bca296a)
The first commit of Daemon launcher is [cc2e7710](https://github.com/catamsp/launcher/commit/cc2e7710c824549c367d97a81a1646d27c6c8993),
which at the time was still intended as a patch for the launcher.
The decision to create a hard fork was made two years later.-->

- Additional gestures:
  - Back
  - V, Λ, <, >
  - Edge gestures: There is a setting to allow distinguishing swiping at the edges of the screen from swiping in the center.

- Compatible with [work profile](https://www.android.com/enterprise/work-profile/), so apps like [Shelter](https://gitea.angry.im/PeterCxy/Shelter) can be used.
- Compatible with [private space](https://source.android.com/docs/security/features/private-space)
- Support for [app widgets](https://developer.android.com/develop/ui/views/appwidgets/overview)
- Support for [pinned shortcuts](https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts)
- Option to rename apps
- Option to hide apps
- Favorite apps
- New actions:
  - Toggle Torch
  - Lock screen
  - Open a widget panel
- The home button now works as expected.
- Improved gesture detection.

## Visual

- This app uses the system wallpaper instead of a custom solution.
- The font has been changed to [Hack][hack-font], other fonts can be selected.
- Font Awesome Icons were replaced by Material icons.
- The gear button on the home screen was removed. A smaller button is shown at the top right when necessary.

## Search

- The search algorithm was modified to prefer matches at the beginning of the app name, i.e., when searching for `"te"`, `"termux"` is sorted before `"notes"`.
- The search bar was moved to the bottom of the screen.

## Technical

- Improved gesture detection.
- Different apps are set as the defaults.
- Package name was changed to `com.catamsp.Daemon` to avoid clashing with the original app.
- Dropped support for API < 21 (i.e., pre Lollypop).
- Fixed some bugs.
- Some refactoring.

The complete list of changes can be viewed [here](https://github.com/catamsp/launcher/compare/340ee731...master).

---

[original-repo]: https://github.com/finnmglas/Daemon launcher
[hack-font]: https://sourcefoundry.org/hack/
