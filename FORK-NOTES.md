# Personal fork of Grace Launcher

This branch is a modified copy of [Grace Launcher](https://github.com/Galaxy-rio/GraceLauncher)
by Galaxy-rio, taken from upstream commit `3930bfc7915c4abe09fec6285a37bc724ec2a1d0` (19 Nov 2026).
It remains licensed under the GNU General Public License v3.0; see `LICENSE`.

## Changes from upstream

- Removed the accessibility service and the notification listener service from
  `AndroidManifest.xml`, so the APK can be sideloaded without Google Play Protect
  blocking it. Notification previews, media controls and accessibility-based
  actions (such as locking the screen) therefore do not work in this build.
- Release builds are signed with a personal sideloading key in `keystore/`.
- Added `.github/workflows/build.yml` to build and publish the APK.

Download: https://github.com/BORZADAM/chechen-learning-app/releases/download/grace-latest/grace-launcher.apk
