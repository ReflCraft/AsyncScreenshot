/*
 * This file is part of AsyncScreenshot (ReflCraft), GPL-3.0.
 */
package io.github.reflcraft.asyncscreenshot.core;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Background executor + main-thread throwback for screenshot work.
 *
 * <p>Design (see docs/mappings.md):
 * <ul>
 *   <li><b>Sync (render thread):</b> pixel snapshot only — {@code takeScreenshot}
 *       (glReadPixels) is GL-bound and must stay on the render thread.</li>
 *   <li><b>Background:</b> PNG encode + file write + unique filename + feedback
 *       building — submitted to a single-thread executor so concurrent F2
 *       presses are serialized (no two threads writing the same filename).</li>
 *   <li><b>Main thread:</b> the {@code Consumer&lt;Component&gt;} feedback is
 *       delivered via {@code Minecraft.getInstance().execute(...)} so HUD /
 *       chat updates stay on the correct thread.</li>
 * </ul>
 *
 * <p>The main-thread {@code execute} and the client singleton are reached
 * reflectively (no {@code net.minecraft.*} import at compile time).
 */
public final class AsyncScreenshotExecutor {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AsyncScreenshot-IO");
        t.setDaemon(true);
        return t;
    });

    /** Serializes disk writes across bursts of screenshots. */
    private static final Semaphore WRITE_LOCK = new Semaphore(1);

    // Lazily-resolved Minecraft client access.
    private static volatile Method minecraftGetInstance;
    private static volatile Method minecraftExecute;
    private static volatile boolean clientProbed;
    private static volatile boolean executeMissing;

    private AsyncScreenshotExecutor() {}

    /**
     * Submit a heavy task to the background IO executor.
     *
     * @param task the PNG encode / write / filename work (never touches GL or
     *             render-thread state; operates on a pixel snapshot only)
     */
    public static void submit(Runnable task) {
        IO.execute(() -> {
            try {
                if (!WRITE_LOCK.tryAcquire(5, TimeUnit.SECONDS)) {
                    return; // still busy with a previous screenshot; drop this one
                }
                try {
                    task.run();
                } finally {
                    WRITE_LOCK.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Run {@code action} back on the Minecraft main (render) thread.
     *
     * @param action main-thread work, e.g. delivering the screenshot message
     *               {@code Consumer}. Never blocks the caller.
     */
    public static void onMainThread(Runnable action) {
        Object client = minecraft();
        Method exec = executeMethod(client);
        if (client != null && exec != null) {
            try {
                exec.invoke(client, (Runnable) action);
                return;
            } catch (Throwable ignored) {
                // fall through to inline execution
            }
        }
        // No client reachable or execute() failed — run inline (rare).
        action.run();
    }

    private static Object minecraft() {
        probeClient();
        if (minecraftGetInstance == null) {
            return null;
        }
        try {
            return minecraftGetInstance.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method executeMethod(Object client) {
        probeClient();
        if (minecraftExecute != null) {
            return minecraftExecute;
        }
        if (executeMissing || client == null) {
            return null;
        }
        // Second-chance lookup on the concrete client class.
        try {
            minecraftExecute = client.getClass().getMethod("execute", Runnable.class);
            return minecraftExecute;
        } catch (Throwable t) {
            executeMissing = true;
            return null;
        }
    }

    private static synchronized void probeClient() {
        if (clientProbed) {
            return;
        }
        try {
            Class<?> mc = Class.forName("net.minecraft.client.Minecraft", false,
                    AsyncScreenshotExecutor.class.getClassLoader());
            minecraftGetInstance = mc.getMethod("getInstance");
            try {
                minecraftExecute = mc.getMethod("execute", Runnable.class);
            } catch (Throwable ignored) {
                executeMissing = true;
            }
        } catch (Throwable ignored) {
            minecraftGetInstance = null;
            executeMissing = true;
        }
        clientProbed = true;
    }
}
