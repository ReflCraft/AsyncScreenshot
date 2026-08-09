/*
 * This file is part of AsyncScreenshot (ReflCraft), GPL-3.0.
 */
package org.refcraft.asyncscreenshot.neoforge;

import org.refcraft.asyncscreenshot.core.VersionProbe;

/**
 * NeoForge mod entry point. The work is done by the {@code ScreenshotMixin}
 * (NeoForge ships Mixin and applies {@code asyncscreenshot.mixins.json}); the
 * {@code @Mod} class only exists so the loader recognizes the mod and warms
 * the version probe.
 */
@net.neoforged.fml.common.Mod("asyncscreenshot")
public class NeoForgeMod {

    public NeoForgeMod() {
        VersionProbe.probe();
    }
}
