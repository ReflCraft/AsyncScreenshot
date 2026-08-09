/*
 * This file is part of AsyncScreenshot (ReflCraft), GPL-3.0.
 */
package org.refcraft.asyncscreenshot.forge;

import org.refcraft.asyncscreenshot.core.VersionProbe;

/**
 * Forge mod entry point. The work is done by the {@code ScreenshotMixin}
 * (Forge ships Mixin and applies {@code asyncscreenshot.mixins.json}); the
 * {@code @Mod} class only exists so the loader recognizes the mod and warms
 * the version probe.
 */
@net.minecraftforge.fml.common.Mod("asyncscreenshot")
public class ForgeMod {

    public ForgeMod() {
        VersionProbe.probe();
    }
}
