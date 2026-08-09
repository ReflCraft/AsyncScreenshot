# Async Screenshot

A client-side mod that stops screenshots from freezing the game. PNG encoding
and disk writes are moved off the render thread.

- Minecraft 1.18 – 26.2
- Fabric / Forge / NeoForge
- No configuration, no server side
- [GPL-3.0](LICENSE)

## Why

Pressing the screenshot key runs PNG encoding and disk writes synchronously on
the render thread. That freezes the frame for tens of milliseconds, and bursts
of screenshots (video / burst capture) make it worse — especially at high
resolutions or with large resource packs.

## How it works

- A Mixin intercepts `Screenshot.grab()` (Mojang name; the class is
  `Screenshot` on 1.18.2 – 1.21.11 and `ScreenshotHelper` on 26.2). The class
  is targeted by all runtime names (Mojang / intermediary `class_318` / SRG
  `C_3408_` / Yarn `ScreenshotRecorder`), so one codebase covers every loader.
- The render thread only does the pixel snapshot (`glReadPixels`), which has
  to stay there. Everything else — unique filename, PNG encode, disk write —
  runs on a single background IO thread (`AsyncScreenshotExecutor`). A
  semaphore serializes writes so concurrent screenshots can't collide on a
  filename.
- When the write is done, the vanilla feedback message ("Saved screenshot
  as ...") is delivered back on the main thread via
  `Minecraft.getInstance().execute(...)`.
- All Minecraft access is resolved at runtime by reflection (`VersionProbe` /
  `FastReflection`); the mod never compiles against the Minecraft JAR. If a
  probe fails on some version, the hook disables itself and vanilla behavior
  is unchanged.

## Install

1. Install the loader and game version you want (Fabric / Forge / NeoForge).
2. Drop `AsyncScreenshot-<version>+<loader>.jar` into `.minecraft/mods`.
3. Start the game. That's it — no config to touch.

## Build

```bash
./gradlew shadowJar
```

Outputs `build/libs/AsyncScreenshot-1.0.0+<loader>.jar`, one per loader.
Requires Java 17+.

## Compatibility

| Minecraft | Fabric | Forge | NeoForge |
|---|---|---|---|
| 1.18 – 26.2 | yes | yes | yes |

If the hook can't be probed on a given version, the mod silently disables
itself — it never crashes the game.

## Limitations

- The render thread still does one in-memory pixel grab (required by GL), so
  a tiny hitch can remain at extreme resolutions.
- The "Saved screenshot" message appears after background encoding finishes,
  so it can be slightly delayed.
- If a screenshot is still being written when a new one arrives, the new one
  is dropped (writes are serialized by design).

## License

[GPL-3.0](LICENSE). Reflection approach based on
[MinerTrack](https://github.com/At87668/MinerTrack).
