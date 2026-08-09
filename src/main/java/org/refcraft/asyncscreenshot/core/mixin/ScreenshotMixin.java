/*
 * This file is part of AsyncScreenshot (ReflCraft), GPL-3.0.
 *
 * Mixin target resolution (verified — see docs/mappings.md):
 *   - Mojang class: net.minecraft.client.Screenshot (1.18.2 – 1.21.11)
 *   - 26.2 class:   net.minecraft.client.ScreenshotHelper
 *   - Yarn:         net.minecraft.client.util.ScreenshotRecorder
 *   - Intermediary: net.minecraft.class_318
 *   - SRG:          net.minecraft.src.C_3408_
 *
 * The capture+save methods (Mojang / Yarn / Intermediary / SRG):
 *   - grab(File, RenderTarget, Consumer)                      (3-arg, public)
 *   - grab(File, String, RenderTarget, Consumer)              (4-arg, public)
 *   - _grab(...same 4-arg...)                                 (private)
 *   - saveScreenshot / saveScreenshotInner / method_1659 /
 *     method_22690 / method_1662 / m_92289_ / m_92295_ / m_92305_
 *   - 26.2 task-doc: save(...)
 *
 * All names are listed with remap=false so the correct one matches per
 * environment (Mojang runtime / Yarn runtime / Intermediary runtime /
 * SRG runtime). Handlers declare Object parameters (coerced at runtime) so no
 * net.minecraft.* type is needed at compile time. On success the hook cancels
 * the vanilla call; on any probe failure it returns false and vanilla runs
 * untouched (silent degradation).
 */
package org.refcraft.asyncscreenshot.core.mixin;

import org.refcraft.asyncscreenshot.core.ScreenshotHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "net.minecraft.client.Screenshot",
        "net.minecraft.client.ScreenshotHelper",
        "net.minecraft.client.util.ScreenshotRecorder",
        "net.minecraft.class_318",
        "net.minecraft.src.C_3408_"
})
public abstract class ScreenshotMixin {

    /** 3-arg: grab(File, RenderTarget, Consumer<Component>). */
    @Inject(method = {
            "grab",
            "saveScreenshot",
            "save",
            "method_1659",
            "m_92289_"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private static void refcraft$onGrab3(Object gameDirectory, Object renderTarget,
                                         Object feedback, CallbackInfo ci) {
        if (ScreenshotHook.onGrab(new Object[]{gameDirectory, renderTarget, feedback})) {
            ci.cancel();
        }
    }

    /** 4-arg: grab(File, String, RenderTarget, Consumer<Component>) / _grab(...). */
    @Inject(method = {
            "grab",
            "_grab",
            "saveScreenshot",
            "saveScreenshotInner",
            "save",
            "method_22690",
            "method_1662",
            "m_92295_",
            "m_92305_"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private static void refcraft$onGrab4(Object gameDirectory, Object fileName,
                                         Object renderTarget, Object feedback,
                                         CallbackInfo ci) {
        if (ScreenshotHook.onGrab(new Object[]{gameDirectory, fileName, renderTarget, feedback})) {
            ci.cancel();
        }
    }
}
