/*
 * This file is part of AsyncScreenshot (ReflCraft), GPL-3.0.
 */
package io.github.reflcraft.asyncscreenshot.core;

import java.lang.reflect.Method;

/**
 * Runtime capability probe for the screenshot helper class.
 *
 * <p>The screenshot class and method names changed across versions
 * (verified on mappings.dev / mcsrc.dev — see docs/mappings.md):
 * <ul>
 *   <li>1.18.2 – 1.21.11: Mojang {@code net.minecraft.client.Screenshot},
 *       method {@code grab} / {@code _grab} (Yarn {@code saveScreenshot} /
 *       {@code saveScreenshotInner});</li>
 *   <li>26.2: class renamed to {@code net.minecraft.client.ScreenshotHelper}
 *       (verified via mcsrc.dev).</li>
 * </ul>
 *
 * <p>This probe tries each candidate class name and method name/signature at
 * runtime and caches the first hit. If nothing matches, {@link #isUsable()}
 * returns {@code false} and the hook is silently disabled (no crash).
 */
public final class VersionProbe {

    private static volatile Class<?> screenshotClass;
    private static volatile Method grabMethod;
    private static volatile Method takeScreenshotMethod;
    private static volatile boolean probed;

    private VersionProbe() {}

    /** Candidate Mojang/Yarn/Intermediary/SRG class names for the helper. */
    private static final String[] CLASS_CANDIDATES = {
            "net.minecraft.client.Screenshot",       // Mojang 1.18.2 – 1.21.11
            "net.minecraft.client.ScreenshotHelper", // 26.2 (mcsrc.dev)
            "net.minecraft.client.util.ScreenshotRecorder", // Yarn
            "net.minecraft.class_318",               // Intermediary
            "net.minecraft.src.C_3408_"              // SRG (Forge pre-1.20.5)
    };

    /** Candidate names for the public "capture + save" entry point. */
    private static final String[] GRAB_NAMES = {
            "grab",              // Mojang
            "saveScreenshot",    // Yarn
            "save",              // 26.2 task-doc claim (verified at runtime)
            "method_1659",       // Intermediary (3-arg)
            "method_22690",      // Intermediary (4-arg)
            "m_92289_",          // SRG (3-arg)
            "m_92295_"           // SRG (4-arg)
    };

    /** Candidate names for the pixel snapshot method. */
    private static final String[] TAKE_SCREENSHOT_NAMES = {
            "takeScreenshot",    // Mojang / Yarn
            "method_1663",       // Intermediary
            "m_92279_",          // SRG
            "method_71641"       // Intermediary (1.21.11 downscale overload)
    };

    /**
     * Probe the runtime class/methods once (lazily, thread-safe).
     */
    public static void probe() {
        if (probed) return;
        synchronized (VersionProbe.class) {
            if (probed) return;
            Class<?> cls = null;
            for (String name : CLASS_CANDIDATES) {
                try {
                    cls = Class.forName(name, false, VersionProbe.class.getClassLoader());
                    break;
                } catch (Throwable ignored) {
                    // try next candidate
                }
            }
            if (cls != null) {
                screenshotClass = cls;
                grabMethod = findStaticMethod(cls, GRAB_NAMES);
                takeScreenshotMethod = findStaticMethod(cls, TAKE_SCREENSHOT_NAMES);
            }
            probed = true;
        }
    }

    private static Method findStaticMethod(Class<?> cls, String[] names) {
        for (Method m : cls.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            String n = m.getName();
            for (String candidate : names) {
                if (n.equals(candidate)) {
                    return m;
                }
            }
        }
        return null;
    }

    /** @return the resolved screenshot helper class, or {@code null}. */
    public static Class<?> screenshotClass() {
        probe();
        return screenshotClass;
    }

    /** @return the resolved capture+save method, or {@code null}. */
    public static Method grabMethod() {
        probe();
        return grabMethod;
    }

    /** @return the resolved pixel-snapshot method, or {@code null}. */
    public static Method takeScreenshotMethod() {
        probe();
        return takeScreenshotMethod;
    }

    /** @return whether a usable hook target was resolved. */
    public static boolean isUsable() {
        probe();
        return screenshotClass != null && grabMethod != null;
    }
}
