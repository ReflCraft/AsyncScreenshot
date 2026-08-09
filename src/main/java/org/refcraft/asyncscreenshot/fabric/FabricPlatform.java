/*
 * This file is part of AsyncScreenshot (ReflCraft), GPL-3.0.
 */
package org.refcraft.asyncscreenshot.fabric;

import net.fabricmc.api.ClientModInitializer;
import org.refcraft.asyncscreenshot.core.VersionProbe;

/**
 * Fabric client entry point. The actual work is done by the {@code ScreenshotMixin}
 * (applied by the loader); this entry only warms up the version probe so the
 * first F2 press is not delayed by lazy class loading.
 */
public class FabricPlatform implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Warm the reflective probe off the render path.
        VersionProbe.probe();
    }
}
