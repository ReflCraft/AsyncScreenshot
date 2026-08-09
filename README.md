# Async Screenshot

![MC](https://img.shields.io/badge/MC-1.18--26.2-blue)
![Fabric](https://img.shields.io/badge/Fabric-✓-green)
![Forge](https://img.shields.io/badge/Forge-✓-green)
![NeoForge](https://img.shields.io/badge/NeoForge-✓-green)
![License](https://img.shields.io/badge/License-GPL--3.0-blue)

> Take screenshots without freezing — PNG encoding & disk writes move off the render thread.

---

## ✨ Features

- ✅ Async screenshot saving — no more render-thread hitches
- ✅ PNG encoding & file writes run on a background thread
- ✅ System message (`Screenshot saved to ...`) is shown on the main thread when done
- ✅ Concurrent screenshots are serialized — no filename collisions
- ✅ Works out of the box; Fabric / Forge / NeoForge, MC 1.18 – 26.2

## 🎯 The Problem

Pressing F2 runs PNG encoding and disk writes synchronously on the render thread,
freezing the frame for tens of milliseconds; continuous screenshots (video / burst
capture) cause severe frame drops. Worse at high resolutions / with large resource packs.

## ⚙️ How It Works

- Hook point: `ScreenshotHelper.save` (Mojang name, verified on mappings.dev)
- Mechanism: Mixin interception → the synchronous part only grabs a pixel-data
  snapshot (fast in-memory copy) → a background thread does PNG encoding +
  file write + unique filename generation → `Minecraft.getInstance().execute(...)`
  returns to the main thread to show the system message.
- Multi-version: reflection capability probing via `VersionProbe` + `docs/mappings.md`
  lookup table covers 1.18 – 26.2; if a hook fails to probe on some version, the
  feature is silently disabled.

## 📦 Installation

1. Install the matching loader (Fabric / Forge / NeoForge) and game version.
2. Drop `AsyncScreenshot-<version>+<loader>.jar` into `.minecraft/mods`.
3. Launch the game — done (client-side mod, no server needed).

## 🕹️ Usage

No configuration required — just press F2 (or your screenshot key). If the hook
fails to probe on some extreme version, the mod silently falls back to vanilla
behavior (does nothing).

## 🔨 Build from Source

```bash
./gradlew shadowJar   # outputs build/libs/AsyncScreenshot-<version>+<loader>.jar
```

## 📄 Compatibility

| MC | Fabric | Forge | NeoForge |
|---|---|---|---|
| 1.18 – 26.2 | ✅ | ✅ | ✅ |

> Note: if a hook fails to probe on a given version, the feature is silently
> disabled (no crash).

## 📜 License

[GPL-3.0](LICENSE) — architecture based on [MinerTrack](https://github.com/At87668/MinerTrack) (FastReflection).

## 💬 Known Limitations

- The synchronous part still performs one pixel grab (in-memory copy); a tiny
  hitch may remain at extreme resolutions.
- The screenshot filename toast may appear slightly delayed at extreme
  resolutions (posted back only after background encoding finishes).
