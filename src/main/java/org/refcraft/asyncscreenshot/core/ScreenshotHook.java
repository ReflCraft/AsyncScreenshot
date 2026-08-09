/*
 * This file is part of AsyncScreenshot (ReflCraft), GPL-3.0.
 */
package org.refcraft.asyncscreenshot.core;

import org.refcraft.asyncscreenshot.common.FastReflection;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/**
 * Core screenshot hook — invoked by the mixin (see fabric/mixins/).
 *
 * <p>The vanilla flow blocks the render thread on PNG encoding + disk writes
 * (and on the unique-filename disk scan). This hook replaces it with:
 * <ol>
 *   <li><b>Sync (render thread):</b> {@code takeScreenshot(renderTarget)} —
 *       the GL readback must stay on the render thread.</li>
 *   <li><b>Background:</b> unique filename + {@code NativeImage.writeFile}
 *       (PNG encode + disk write) on {@link AsyncScreenshotExecutor}.</li>
 *   <li><b>Main thread:</b> the screenshot {@code Consumer} feedback is
 *       delivered via {@code Minecraft.execute(...)}.</li>
 * </ol>
 *
 * <p>All MC access is reflective ({@link VersionProbe} + {@link FastReflection});
 * if any probe fails the hook returns {@code false} and the mixin lets vanilla
 * run untouched (silent degradation, no crash).
 */
public final class ScreenshotHook {

    private ScreenshotHook() {}

    /** Time-based fallback filename pattern (mirrors vanilla style). */
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    private static volatile Method nativeImageWriteFile;
    private static volatile Method nativeImageClose;
    private static volatile Method componentLiteral;
    private static volatile boolean extraProbed;

    /**
     * Handle a {@code grab(...)} invocation. Must be called on the render
     * thread with the original method arguments.
     *
     * @param args raw arguments of the intercepted {@code grab} method:
     *             {@code [File gameDirectory, (String name,)? RenderTarget rt, Consumer cb]}
     * @return {@code true} if the hook took over (caller must cancel the
     *         original call), {@code false} to leave vanilla behaviour intact
     */
    public static boolean onGrab(Object[] args) {
        if (!VersionProbe.isUsable() || args == null || args.length < 3) {
            return false;
        }

        File gameDirectory = asFile(args[0]);
        Object renderTarget = args[args.length - 2]; // last-but-one is RenderTarget
        Object feedback = args[args.length - 1];     // last is Consumer<Component>
        if (gameDirectory == null || renderTarget == null || feedback == null) {
            return false;
        }

        // ---- Sync: pixel snapshot (GL readback, render thread only) ----
        Object image = takeScreenshot(renderTarget);
        if (image == null) {
            return false; // could not grab pixels; leave vanilla to handle it
        }

        // ---- Async: unique filename + PNG encode + disk write ----
        AsyncScreenshotExecutor.submit(() -> writeAndNotify(gameDirectory, image, feedback));

        return true;
    }

    // ==================================================================
    // Background work
    // ==================================================================

    private static void writeAndNotify(File gameDirectory, Object image, Object feedback) {
        File file = null;
        String message = null;
        boolean saved = false;
        try {
            file = uniqueFile(gameDirectory);
            Method write = writeFileMethod(image);
            if (write == null) {
                throw new IllegalStateException("NativeImage.writeFile not found");
            }
            FastReflection.invoke(write, image, new Object[]{file});
            saved = true;
            message = "Saved screenshot as " + file.getName();
        } catch (Throwable t) {
            message = "Couldn't save screenshot";
        } finally {
            Method close = closeMethod(image);
            if (close != null) {
                FastReflection.invoke(close, image, new Object[0]);
            }
        }

        final String text = message;
        final boolean ok = saved;
        AsyncScreenshotExecutor.onMainThread(() ->
                deliver(feedback, text, ok));
    }

    private static void deliver(Object feedback, String text, boolean ok) {
        try {
            Object component = buildComponent(text);
            Method accept = feedback.getClass().getMethod("accept", Object.class);
            accept.invoke(feedback, component);
        } catch (Throwable ignored) {
            // Feedback delivery is best-effort.
        }
    }

    // ==================================================================
    // Reflective helpers
    // ==================================================================

    private static Object takeScreenshot(Object renderTarget) {
        Method m = VersionProbe.takeScreenshotMethod();
        if (m == null) {
            return null;
        }
        // try (RenderTarget) then (RenderTarget, int)
        if (m.getParameterCount() == 1) {
            return FastReflection.invokeStatic(m, new Object[]{renderTarget});
        }
        if (m.getParameterCount() == 2) {
            return FastReflection.invokeStatic(m, new Object[]{renderTarget, 1});
        }
        return null;
    }

    private static File uniqueFile(File gameDirectory) {
        // Prefer vanilla's private getFile(...) when reachable, else fall back
        // to our own timestamp + collision loop.
        File vanilla = vanillaGetFile(gameDirectory);
        if (vanilla != null) {
            return vanilla;
        }
        for (int i = 0; i < 100; i++) {
            String name = DATE_FORMAT.format(new Date());
            if (i > 0) {
                name += "_" + i;
            }
            File f = new File(gameDirectory, name + ".png");
            if (!f.exists()) {
                return f;
            }
        }
        return new File(gameDirectory, "screenshot-" + System.currentTimeMillis() + ".png");
    }

    private static File vanillaGetFile(File gameDirectory) {
        try {
            Class<?> cls = VersionProbe.screenshotClass();
            if (cls == null) {
                return null;
            }
            for (Method m : cls.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                String n = m.getName();
                if (n.equals("getFile") || n.equals("getScreenshotFilename")
                        || n.equals("method_1660") || n.equals("m_92287_")) {
                    m.setAccessible(true);
                    Object r = FastReflection.invokeStatic(m, new Object[]{gameDirectory});
                    return r instanceof File ? (File) r : null;
                }
            }
        } catch (Throwable ignored) {
            // fall back to own name
        }
        return null;
    }

    private static Method writeFileMethod(Object image) {
        probeExtras();
        return nativeImageWriteFile;
    }

    private static Method closeMethod(Object image) {
        probeExtras();
        return nativeImageClose;
    }

    private static void probeExtras() {
        if (extraProbed) {
            return;
        }
        try {
            Class<?> ni = Class.forName("com.mojang.blaze3d.platform.NativeImage", false,
                    ScreenshotHook.class.getClassLoader());
            for (Method m : ni.getMethods()) {
                if (m.getName().equals("writeFile") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == File.class) {
                    nativeImageWriteFile = m;
                }
            }
            for (Method m : ni.getMethods()) {
                if (m.getName().equals("close") && m.getParameterCount() == 0) {
                    nativeImageClose = m;
                }
            }
        } catch (Throwable ignored) {
            // leave null → degrade gracefully
        }
        extraProbed = true;
    }

    private static Object buildComponent(String text) {
        try {
            if (componentLiteral == null) {
                Class<?> c = Class.forName("net.minecraft.network.chat.Component", false,
                        ScreenshotHook.class.getClassLoader());
                for (Method m : c.getMethods()) {
                    if (m.getName().equals("literal") && m.getParameterCount() == 1
                            && m.getParameterTypes()[0] == String.class) {
                        componentLiteral = m;
                        break;
                    }
                }
            }
            if (componentLiteral != null) {
                return componentLiteral.invoke(null, text);
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return text; // plain String fallback (Consumer<Object> tolerant)
    }

    private static File asFile(Object o) {
        return o instanceof File ? (File) o : null;
    }
}
