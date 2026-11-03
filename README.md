# HomeSnap

HomeSnap removes the delay when you swipe home with a third-party launcher on ColorOS 17.

With a launcher like Nova, going home on ColorOS is slow. The app shrinks away, you look at your wallpaper for more than half a second, then your icons appear all at once. Anything you tap during that wait is ignored. With HomeSnap your home screen appears much sooner, and you can tap an icon straight away.

You keep Android's own gesture navigation, including predictive back. HomeSnap needs no root, no gesture app and no change to your navigation mode. It does need [Shizuku](https://shizuku.rikka.app/).

## How much faster

Time from the first visible movement to the finished home screen, from 120 fps screen recordings of scripted swipes. The test phone was an Oppo Find N6 with Nova Launcher and its animation scales at 0.5x.

| Swipe | Screen | Without HomeSnap | With HomeSnap |
|---|---|---|---|
| Normal (180 ms) | Cover | 830-850 ms | 208-224 ms |
| Normal (180 ms) | Inner | 809-841 ms | 209-218 ms |
| Fast flick (40 ms) | Cover | not measured | 74-91 ms |
| Fast flick (40 ms) | Inner | 744-752 ms | 107-108 ms |

The recordings with HomeSnap showed no flicker or black frames when going home. The inner screen figures come from the same helper run over ADB, before it was packaged as an app.

## What you need

- ColorOS 17. The app installs on Android 15 and newer, but ColorOS 17 on the Find N6 is the only setup tested so far.
- [Shizuku](https://github.com/RikkaApps/Shizuku/releases) installed and running. Its [setup guide](https://shizuku.rikka.app/guide/setup/) explains how to start it with wireless debugging.
- A third-party launcher set as your home app. The delay doesn't affect the ColorOS launcher.

## Install

1. Start Shizuku.
2. Download the latest APK from [Releases](https://github.com/mostrowski123/homesnap/releases/latest) and install it.
3. Open HomeSnap and turn on the switch.
4. Allow HomeSnap when Shizuku asks. The status line should change to "On."

Swipe home from any app to try it. To update, install a newer APK over the old one.

## After a reboot

Start Shizuku the way you normally do. HomeSnap checks about once a minute and reconnects by itself, so you don't have to open it. In testing it was back 51 seconds after Shizuku started. Opening HomeSnap makes it reconnect straight away.

## What the status line means

| Status | Meaning |
|---|---|
| Off. | The switch is off, and going home works the way ColorOS normally does. |
| On. | HomeSnap is working. |
| Starting... | The helper is starting. This should only take a moment. |
| Waiting for Shizuku. | Shizuku isn't running. Start it in the Shizuku app. |
| Allow HomeSnap when Shizuku asks. | HomeSnap needs permission. If Shizuku doesn't ask, allow it in the Shizuku app. |

## Troubleshooting

### The old delay came back

Open Shizuku and check that it says it's running. In testing, one start attempt after a reboot looked fine but left Shizuku stopped. Then open HomeSnap to reconnect.

### The status stays on "Starting..."

The helper failed to start, possibly because a ColorOS update changed something it relies on. Turn the switch off and [open an issue](https://github.com/mostrowski123/homesnap/issues) with your phone model, your ColorOS version and the output of `adb logcat -d -s HomeSnap`.

### You end up on an empty screen with only your wallpaper

Swipe up again. This happened once in testing, on the first swipe after using a landscape app on the inner screen and then folding the phone. The log points to ColorOS and HomeSnap took no action, but the same steps haven't been repeated with HomeSnap off.

### It doesn't come back after a reboot

Open HomeSnap once. Force-stopping an app makes Android cancel its background checks until you open it again. If this keeps happening after normal reboots, try allowing HomeSnap to auto-launch in its app settings. The test phone didn't need that.

## Limits and risks

HomeSnap uses an internal Android API in a way it wasn't designed for, so any ColorOS update could break it. If gestures misbehave, turn the switch off. If that doesn't fix them, reboot.

The closing animation of the app you're leaving gets cut short, because your home screen appears before it would normally finish.

It has been tested on one setup: an Oppo Find N6 (CPH2765) on ColorOS 17.0.0 (17.0.0.101) with Nova Launcher 8.9.6 and Shizuku 13.6.0. Other launchers should behave the same way, because the delay comes from ColorOS and HomeSnap detects your home app automatically. Owners of other OPPO, OnePlus and realme phones report the same delay, so it may help there too. If you try a different phone or launcher, please [open an issue](https://github.com/mostrowski123/homesnap/issues) saying how it went.

Picture-in-picture, split screen, long-term use and battery life haven't been tested.

A new release is published automatically whenever the app's code changes, and not every one gets tried on a phone first. To go back to an older version, uninstall HomeSnap and install the older APK from [Releases](https://github.com/mostrowski123/homesnap/releases).

## Privacy and permissions

HomeSnap has no internet permission and collects nothing. It asks for Shizuku access and for permission to run at boot.

Through Shizuku, its helper runs with the same access as ADB shell. That's a high level of access, so you may want to read the code. The helper is one short file, [HelperService.java](app/src/main/java/moe/matthew/homesnap/HelperService.java). It watches which task comes to the front and sends window commands.

Releases are signed with a certificate with this SHA-256 fingerprint:

```
68c50249271d93d6437b154bd1441e336b1a13d9f6d87da375f9b8245198316f
```

## Turning it off or removing it

Turn the switch off and the helper stops right away, and going home works the way ColorOS normally does. Turn the switch off before you uninstall.

## How it works

ColorOS's own launcher handles the home gesture, even when another launcher is your home app. When you lift your finger it starts your launcher, which is ready within a few tens of milliseconds. ColorOS then keeps it hidden until two things finish: its own exit animation (about 680 ms at 0.5x animator scale) and a fixed 500 ms scroll in its hidden Recents screen.

The helper watches for the Recents screen coming to the front, followed directly by your home screen. That pair only happens at the end of a home gesture, so back gestures are left alone. 60 ms later it does three things:

1. It marks Recents as translucent, so that your launcher stays drawn.
2. It starts an empty sleep transition, which makes the window manager cancel the wait and show your launcher.
3. ColorOS then moves Recents on top, so the helper moves your launcher back above it and clears the translucent flag.

The 60 ms delay is there for quick switch, which brings the home screen forward for up to about 30 ms before the app you're switching to.

## Building

You need JDK 21 and the Android SDK.

```
./gradlew testDebugUnitTest assembleDebug
```

The APK ends up in `app/build/outputs/apk/debug/`. Local builds keep the same version number, so if you change the helper, turn the switch off and on to load the new code.

Each push to `main` that changes more than docs builds a signed APK and publishes it to [Releases](https://github.com/mostrowski123/homesnap/releases).

## Licence

MIT. See [LICENSE](LICENSE).

HomeSnap isn't affiliated with OPPO, OnePlus, realme, Nova Launcher or Shizuku.
