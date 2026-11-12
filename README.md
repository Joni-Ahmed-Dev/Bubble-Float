# Bubble Float

Open any app in a floating bubble over whatever you are doing. Two apps on
screen at once, switching between them is one tap.

Tap an app and it floats. Tap its head to bring it back, drag it away to
close it. Long-press any app to pin it at the front of the drawer. A floating
button (optional) opens the drawer from any screen.

## How it works

This app does not draw the floating window itself. It asks Android to open
the app as a system bubble: a long-lived shortcut per app whose intent is the
app's main activity, plus a MessagingStyle notification on that shortcut
carrying BubbleMetadata that expands at once and hides its own notification.
Android draws the floating frame, the row of heads and the close button,
which is why it needs no root and no special permission.

The bubble technique follows the one documented in
[Launchii](https://github.com/deciocrytek/Launchii) (GPL-3.0),
reimplemented here in Java. This app is GPL-3.0 too.

## Setup on the phone

1. Install the APK and open Bubble Float.
2. Tap **Show my apps**. Allow notifications when asked.
3. Tap an app. If bubbles are not set up yet, the app opens the right
   settings page: allow notifications for Bubble Float, then set its bubbles
   to **All conversations can bubble**.
4. Optional: menu > **Floating button** opens the drawer from any screen.
   It needs **Display over other apps**.

## Known limits

- Real bubbles need Android 11 or newer. Older phones open apps full screen.
- Some apps refuse to bubble (they are not resizable) and open full screen
  instead. The app then opens normally and says so.
- Single-task apps already sitting in a bubble are reopened in that bubble
  instead of being pulled full screen.
- On MIUI the system bubble behaviour can differ from stock Android. If a
  bubble does not appear, check Settings > Notifications > Floating
  notifications for Bubble Float.

## Build

Local (no Gradle):

```sh
bash build.sh   # writes bubble-float.apk
```

GitHub Actions builds the same script on every push (see
`.github/workflows/build.yml`) and uploads the APK as an artifact.

## License

GPL-3.0, see `LICENSE`.
