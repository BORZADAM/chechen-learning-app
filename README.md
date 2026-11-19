# Trail Launcher

A minimal, one-handed Android home screen. Kotlin, Jetpack Compose, Material 3. Fully local: no ads, analytics or network code.

Latest APK: https://github.com/BORZADAM/chechen-learning-app/releases/latest/download/trail-launcher.apk

## Structure

```
app/src/main/
  AndroidManifest.xml            HOME activity, wallpaper window
  java/app/trail/launcher/
    MainActivity.kt              entry point, package-change callbacks
    data/
      AppEntry.kt                app model, screen modes
      Store.kt                   app list, favourites, hidden apps, prefs, accent colour
      CalendarRepo.kt            next on-device calendar event
    ui/
      LauncherScreen.kt          background layers, scrim and blur, global swipes
      home/
        HomeScreen.kt            header + favourites + scrubber layout, stagger animation
        ClockHeader.kt           clock, date, next event
        AppRow.kt                app row, icon loading, long-press menu
        WaveScrubber.kt          Gaussian wave index with haptics
        MeshBackground.kt        optional AGSL gradient background
      search/SearchScreen.kt     keyboard-first search
      settings/SettingsScreen.kt settings and favourites picker
      theme/Theme.kt             text styles, shared spring
    util/SystemUi.kt             launch apps, open notification shade
.github/workflows/build.yml      builds and publishes the APK on every push
```

## Building

Open in Android Studio, or run `gradle assembleRelease` with Gradle 8.11 and JDK 17. Every push to the `launcher` branch builds an APK and publishes it to Releases.
