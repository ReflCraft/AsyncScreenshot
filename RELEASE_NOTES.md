# Async Screenshot 1.0.0

Initial release.

Taking a screenshot normally freezes the game for a visible moment because
vanilla encodes the PNG and writes it to disk right on the render thread.
Async Screenshot moves that heavy work to a background thread, so pressing F2
no longer causes a frame hitch — even in burst capture or while recording.

Client-side only. No config needed. Works on Fabric, Forge, and NeoForge.

## What's new in 1.0.0

- **Async PNG encode + disk write.** The render thread only grabs the pixel
  snapshot from the GPU (required by OpenGL); generating the unique filename,
  encoding the PNG, and writing the file all happen on a dedicated background
  IO thread.
- **Serialized writes.** A semaphore ensures concurrent screenshots don't
  collide on the same filename. If a previous screenshot is still being saved,
  a new one is dropped instead of corrupting an in-flight write.
- **Main-thread feedback.** The "Saved screenshot" message is delivered back
  on the main thread via `Minecraft.execute(...)`, just like vanilla.
- **Single jar for all loaders and versions.** One shared mixin targets every
  candidate runtime name (Mojang / Yarn / Intermediary / SRG) with
  `remap=false`, so a single jar covers Fabric, Forge, and NeoForge across
  Minecraft 1.18 through 26.2.
- **Silent degradation.** If the game version can't be recognized, the hook
  disables itself and screenshots behave exactly like vanilla — no crash, no
  config to flip.

## Compatibility

- Minecraft 1.18 through 26.2
- Fabric, Forge, NeoForge (one jar per loader)
- Java 17 or later
- GPL-3.0

## Install

1. Install Fabric, Forge, or NeoForge for your Minecraft version.
2. Put the jar in your `mods` folder.
3. Launch the game. Nothing else to configure.

Classic Forge users also need
[MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixinbooter),
since classic Forge doesn't ship with a Mixin loader.

## Downloads

- `AsyncScreenshot-1.0.0+fabric.jar`
- `AsyncScreenshot-1.0.0+forge.jar`
- `AsyncScreenshot-1.0.0+neoforge.jar`

## Known limitations

- The render thread still grabs the pixels from the GPU, so a tiny hitch can
  remain at very high resolutions.
- The "Saved screenshot" message appears after background encoding finishes,
  so it may be slightly delayed.
- If a screenshot is still being saved when a new one is taken, the new one is
  dropped to prevent file collisions.
