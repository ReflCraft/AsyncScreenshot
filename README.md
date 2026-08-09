# Async Screenshot

Taking a screenshot can freeze the game for a noticeable moment. This mod
moves the heavy work — PNG encoding and writing the file to disk — off the
render thread, so pressing F2 no longer causes a frame hitch.

Client-side only. Works on Fabric, Forge, and NeoForge. No config needed.

## Why you might want this

When you press the screenshot key, vanilla Minecraft encodes the image to
PNG and writes it to disk right on the render thread. That freezes the frame
for tens of milliseconds. Taking screenshots in quick succession — burst
capture, recording, or just mashing F2 — makes it worse, especially at high
resolutions or with large resource packs.

## How it works

The render thread grabs the pixels from the GPU (that part has to stay
there), then hands everything else off to a background thread: generating a
unique filename, encoding the PNG, and writing the file. A semaphore makes
sure concurrent screenshots don't collide on the same filename. When the
write finishes, the "Saved screenshot" message is delivered back on the main
thread, just like vanilla.

If the mod can't hook into your game version, it disables itself and
screenshots behave exactly like vanilla.

## Install

1. Install Fabric, Forge, or NeoForge for your Minecraft version.
2. Put the jar file in your `mods` folder.
3. Launch the game. Nothing else to configure.

Classic Forge users: you also need [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixinbooter)
installed, since classic Forge doesn't ship with a Mixin loader.

Produces one jar per loader in `build/libs/`. Requires Java 17 or later.

## Compatibility

Tested on Minecraft 1.18 through 26.2 across Fabric, Forge, and NeoForge.
If the mod can't hook into your specific version, it disables itself
quietly — the game runs as if the mod weren't there.

## Limitations

- The render thread still grabs the pixels from the GPU (that's required by
  OpenGL), so a tiny hitch can remain at very high resolutions.
- The "Saved screenshot" message appears after the background encoding
  finishes, so it may be slightly delayed.
- If a screenshot is still being saved when a new one is taken, the new one
  is dropped to prevent file collisions.
