package com.sirens.visuals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Diagnostics for the mixin hooks. Every hook calls {@link #hit(String)} from inside its handler, so a non-zero
 * count is proof the hook was applied to this Minecraft version and is running.
 */
public final class Hooks {
    private Hooks() { }

    public record Info(String id, String label, String trigger) { }

    public static final Map<String, Info> INFO = new LinkedHashMap<>();
    private static final Map<String, AtomicLong> HITS = new ConcurrentHashMap<>();

    private static void reg(String id, String label, String trigger) { INFO.put(id, new Info(id, label, trigger)); }

    static {
        reg("fire", "Low Fire (screen overlay)", "set yourself on fire");
        reg("hand", "Hand Position", "look at your hand with Hand Position enabled");
        reg("skin", "Skin / Cape override", "enable Skin Changer or Custom Cape");
        reg("freelook", "Freelook / Cinematic turn", "hold the freelook key");
        reg("camrot", "Camera rotation", "hold the freelook key");
        reg("camdist", "Third-person distance", "enable Third Person and press F5");
        reg("fov", "Zoom (FOV)", "hold the zoom key");
        reg("scroll", "Scroll zoom", "scroll while zooming");
        reg("watervision", "Clear underwater vision", "go underwater");
        reg("watercolor", "Water color", "look at water");
        reg("time", "Time lock (getter)", "Custom Sky > Time: Locked");
        reg("timeset", "Time lock (server packets)", "Custom Sky > Time: Locked");
        reg("celestial", "Sun/Moon/Stars brightness", "Custom Sky enabled");
        reg("dome", "Sky dome color", "Custom Sky > Tint Sky");
        reg("skydome", "Sky image (skybox)", "Custom Sky > Show Sky Image with an image selected");
        reg("sun", "Sun size", "Custom Sky enabled");
        reg("moon", "Moon size", "Custom Sky enabled");
    }

    public static final Map<String, String> ERRORS = new ConcurrentHashMap<>();

    public static void error(String where, Throwable t) {
        String m = t.getClass().getSimpleName() + ": " + t.getMessage();
        if (ERRORS.put(where, m) == null) { System.err.println("[Sirens Visuals] error in " + where); t.printStackTrace(); }
    }

    public static void hit(String id) {
        HITS.computeIfAbsent(id, k -> new AtomicLong()).incrementAndGet();
    }

    public static long count(String id) {
        AtomicLong a = HITS.get(id);
        return a == null ? 0 : a.get();
    }

    public static boolean seen(String id) { return count(id) > 0; }

    /** Number of hooks confirmed running. */
    public static int active() {
        int n = 0;
        for (String id : INFO.keySet()) if (seen(id)) n++;
        return n;
    }
}
