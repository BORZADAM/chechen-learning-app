# Trail Launcher

Trail Launcher is a personal launcher built on [Grace Launcher](https://github.com/Galaxy-rio/GraceLauncher)
by Galaxy-rio, starting from upstream commit `3930bfc7915c4abe09fec6285a37bc724ec2a1d0` (19 Nov 2026).
Grace Launcher is licensed under the GNU General Public License v3.0, and so is this project; see `LICENSE`.

The earlier, from-scratch version of Trail is kept on the `trail-classic` branch.

## Changes from upstream Grace Launcher

- Renamed to Trail Launcher, with application ID `app.trail.launcher` and its own launcher icon.
- Removed the accessibility service and the notification listener service from
  `AndroidManifest.xml`, so the APK can be sideloaded without Google Play Protect
  blocking it. Notification previews, media controls and accessibility-based
  actions (such as locking the screen) therefore do not work in this build.
- Release builds are signed with a personal sideloading key in `keystore/`.
- Added `.github/workflows/build.yml` to build and publish the APK.

Download: https://github.com/BORZADAM/chechen-learning-app/releases/latest/download/trail-launcher.apk
