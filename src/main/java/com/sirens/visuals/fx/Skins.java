package com.sirens.visuals.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.sirens.visuals.Hooks;
import com.sirens.visuals.Net;
import com.sirens.visuals.cfg.Config;
import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Skin / cape overriding for the local player. Everything here is client-side only (others do not see it). */
public final class Skins {
    private Skins() { }

    public static volatile Identifier skinId, capeId;
    public static volatile Boolean slim;
    public static String status = "";
    private static int counter;

    public static Path skinsDir() { Path p = Config.dir().resolve("skins"); try { Files.createDirectories(p); } catch (Exception ignored) { } return p; }
    public static Path capesDir() { Path p = Config.dir().resolve("capes"); try { Files.createDirectories(p); } catch (Exception ignored) { } return p; }

    public static void scan() {
        try { SkyFx.scan(); } catch (Throwable t) { Hooks.error("sky scan", t); }
        Mods.SK_FILE.setOpts(list(skinsDir()));
        Mods.C_FILE.setOpts(list(capesDir()));
    }

    private static String[] list(Path d) {
        List<String> l = new ArrayList<>();
        try (var s = Files.list(d)) { s.filter(f -> f.toString().toLowerCase().endsWith(".png")).forEach(f -> l.add(f.getFileName().toString())); } catch (Exception ignored) { }
        if (l.isEmpty()) l.add("(none)");
        return l.toArray(new String[0]);
    }

    // ------------------------------------------------------------ texture registration
    static Identifier register(byte[] png, String key) throws Exception {
        NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
        DynamicTexture tex = null;
        for (Constructor<?> c : DynamicTexture.class.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 1 && p[0] == NativeImage.class) tex = (DynamicTexture) c.newInstance(img);
            else if (p.length == 2 && p[0] == java.util.function.Supplier.class && p[1] == NativeImage.class)
                tex = (DynamicTexture) c.newInstance((java.util.function.Supplier<String>) () -> key, img);
            if (tex != null) break;
        }
        if (tex == null) throw new IllegalStateException("DynamicTexture constructor not found");
        Identifier id = Identifier.fromNamespaceAndPath("sirensvisuals", key + "_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(id, tex);
        return id;
    }

    static void registerAs(byte[] png, Identifier id) throws Exception {
        NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
        DynamicTexture tex = null;
        for (Constructor<?> c : DynamicTexture.class.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 1 && p[0] == NativeImage.class) tex = (DynamicTexture) c.newInstance(img);
            else if (p.length == 2 && p[0] == java.util.function.Supplier.class && p[1] == NativeImage.class)
                tex = (DynamicTexture) c.newInstance((java.util.function.Supplier<String>) () -> id.toString(), img);
            if (tex != null) break;
        }
        if (tex == null) throw new IllegalStateException("DynamicTexture constructor not found");
        Minecraft.getInstance().getTextureManager().register(id, tex);
    }

    // ------------------------------------------------------------ sources
    record Profile(String skinUrl, boolean slim, String capeUrl) { }

    static <T> T need(T v) { if (v == null) throw new IllegalStateException("download failed - check the username/URL"); return v; }

    static Profile profile(String name) throws Exception {
        JsonObject o = JsonParser.parseString(need(Net.text("https://api.mojang.com/users/profiles/minecraft/" + Net.enc(name)))).getAsJsonObject();
        String id = o.get("id").getAsString();
        JsonObject pr = JsonParser.parseString(need(Net.text("https://sessionserver.mojang.com/session/minecraft/profile/" + id))).getAsJsonObject();
        JsonArray props = pr.getAsJsonArray("properties");
        for (int i = 0; i < props.size(); i++) {
            JsonObject p = props.get(i).getAsJsonObject();
            if (!p.get("name").getAsString().equals("textures")) continue;
            JsonObject t = JsonParser.parseString(new String(Base64.getDecoder().decode(p.get("value").getAsString()))).getAsJsonObject().getAsJsonObject("textures");
            String s = null, c = null; boolean sl = false;
            if (t.has("SKIN")) { JsonObject sk = t.getAsJsonObject("SKIN"); s = sk.get("url").getAsString(); sl = sk.has("metadata") && sk.getAsJsonObject("metadata").has("model"); }
            if (t.has("CAPE")) c = t.getAsJsonObject("CAPE").get("url").getAsString();
            return new Profile(s, sl, c);
        }
        throw new IllegalStateException("no textures");
    }

    private static Path copyIn(Path src, Path dir) throws Exception {
        Path dst = dir.resolve(src.getFileName().toString());
        if (!src.toAbsolutePath().normalize().equals(dst.toAbsolutePath().normalize())) Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return dst;
    }

    public static void useSkinFile(Path src) {
        try {
            Path d = copyIn(src, skinsDir()); scan();
            Mods.SK_SRC.set(0);
            String[] o = Mods.SK_FILE.opts;
            for (int i = 0; i < o.length; i++) if (o[i].equals(d.getFileName().toString())) Mods.SK_FILE.set(i);
            applySkin();
        } catch (Exception e) { status = "Could not use file: " + e; }
    }

    public static void useCapeFile(Path src) {
        try {
            Path d = copyIn(src, capesDir()); scan();
            Mods.C_SRC.set(1);
            String[] o = Mods.C_FILE.opts;
            for (int i = 0; i < o.length; i++) if (o[i].equals(d.getFileName().toString())) Mods.C_FILE.set(i);
            applyCape();
        } catch (Exception e) { status = "Could not use file: " + e; }
    }

    public static void applySkin() {
        status = "Loading skin...";
        Thread t = new Thread(() -> {
            try {
                byte[] png; Boolean sl = null;
                switch (Mods.SK_SRC.idx()) {
                    case 0 -> png = Files.readAllBytes(skinsDir().resolve(Mods.SK_FILE.value()));
                    case 1 -> { Profile p = profile(Mods.SK_NAME.get().trim()); if (p.skinUrl() == null) throw new IllegalStateException("player has no skin"); png = need(Net.bytes(p.skinUrl())); sl = p.slim(); }
                    default -> png = need(Net.bytes(Mods.SK_URL.get().trim()));
                }
                png = fixSkin(png);
                byte[] fin = Mods.C_SRC.idx() == 0 ? png : Capes.fit(png); Boolean fsl = sl;
                Minecraft.getInstance().execute(() -> {
                    try {
                        skinId = register(fin, "skin");
                        int m = Mods.SK_MODEL.idx();
                        slim = m == 1 ? Boolean.FALSE : m == 2 ? Boolean.TRUE : fsl;
                        Mods.SKIN.enabled.set(true);
                        status = "Skin applied.";
                    } catch (Exception e) { status = "Skin failed: " + e.getMessage(); }
                });
            } catch (Exception e) { status = "Skin failed: " + e; }
        }, "sirens-skin");
        t.setDaemon(true); t.start();
    }

    /** Converts legacy 64x32 skins to 64x64 so the model renders correctly. */
    static byte[] fixSkin(byte[] png) throws Exception {
        BufferedImage in = ImageIO.read(new ByteArrayInputStream(png));
        if (in == null) throw new IllegalStateException("not a PNG");
        if (in.getWidth() == 64 && in.getHeight() == 64) return png;
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(in, 0, 0, null);
        if (in.getHeight() == 32) { // mirror legs/arms to the left side
            g.drawImage(in.getSubimage(0, 16, 16, 16), 16, 48, null);
            g.drawImage(in.getSubimage(40, 16, 16, 16), 32, 48, null);
        }
        g.dispose();
        return toPng(out);
    }

    static byte[] toPng(BufferedImage b) throws Exception { ByteArrayOutputStream o = new ByteArrayOutputStream(); ImageIO.write(b, "png", o); return o.toByteArray(); }

    public static void applyCape() {
        status = "Loading cape...";
        Thread t = new Thread(() -> {
            try {
                byte[] png;
                switch (Mods.C_SRC.idx()) {
                    case 0 -> png = Capes.generate(Mods.C_PAT.idx(), Mods.C_C1.rgb(), Mods.C_C2.rgb());
                    case 1 -> png = Files.readAllBytes(capesDir().resolve(Mods.C_FILE.value()));
                    case 2 -> { Profile p = profile(Mods.C_NAME.get().trim()); if (p.capeUrl() == null) throw new IllegalStateException("player has no cape"); png = need(Net.bytes(p.capeUrl())); }
                    default -> png = need(Net.bytes(Mods.C_URL.get().trim()));
                }
                byte[] fin = Mods.C_SRC.idx() == 0 ? png : Capes.fit(png);
                Minecraft.getInstance().execute(() -> {
                    try { capeId = register(fin, "cape"); Mods.CAPE.enabled.set(true); status = "Cape applied."; }
                    catch (Exception e) { status = "Cape failed: " + e.getMessage(); }
                });
            } catch (Exception e) { status = "Cape failed: " + e; }
        }, "sirens-cape");
        t.setDaemon(true); t.start();
    }

    // ------------------------------------------------------------ mixin entry point
    // Reflection must not rely on member names: at runtime they are obfuscated. We go by position/type instead.
    public static PlayerSkin modify(PlayerSkin base) {
        try {
            boolean s = Mods.SKIN.on() && skinId != null, c = Mods.CAPE.on() && capeId != null;
            if (!s && !c) return base;
            Hooks.hit("skin");
            RecordComponent[] rc = PlayerSkin.class.getRecordComponents();
            Object[] args = new Object[rc.length];
            Class<?>[] types = new Class<?>[rc.length];
            int slot = 0;
            for (int i = 0; i < rc.length; i++) {
                Class<?> t = rc[i].getType();
                types[i] = t;
                rc[i].getAccessor().setAccessible(true);
                Object v = rc[i].getAccessor().invoke(base);
                if (t.isEnum()) { if (s && slim != null) v = model(t, slim, v); }
                else if (t != boolean.class) {
                    int k = slot++;
                    if (k == 0 && s) v = make(t, skinId, v);
                    else if (k == 1 && c) v = make(t, capeId, v);
                    else if (k == 2 && c && Mods.C_ELYTRA.on()) v = make(t, capeId, v);
                }
                args[i] = v;
            }
            Constructor<PlayerSkin> k = PlayerSkin.class.getDeclaredConstructor(types);
            k.setAccessible(true);
            return k.newInstance(args);
        } catch (Throwable t) {
            Hooks.error("skin override", t);
            status = "Skin override failed: " + t;
            return base;
        }
    }

    private static Object make(Class<?> type, Identifier id, Object old) throws Exception {
        if (type == Identifier.class) return id;
        java.util.List<Class<?>> cands = new ArrayList<>();
        if (old != null) cands.add(old.getClass());
        if (type.getEnclosingClass() != null) cands.addAll(java.util.Arrays.asList(type.getEnclosingClass().getDeclaredClasses()));
        cands.addAll(java.util.Arrays.asList(type.getDeclaredClasses()));
        cands.add(type);
        for (Class<?> k : cands) {
            if (!type.isAssignableFrom(k) || k.isInterface() || java.lang.reflect.Modifier.isAbstract(k.getModifiers())) continue;
            for (Constructor<?> c : k.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                c.setAccessible(true);
                if (p.length == 2 && p[0] == Identifier.class && p[1] == Identifier.class) return c.newInstance(id, id);
            }
            for (Constructor<?> c : k.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 1 && p[0] == Identifier.class) { c.setAccessible(true); return c.newInstance(id); }
            }
        }
        throw new IllegalStateException("no texture class for " + type.getName());
    }

    private static Object model(Class<?> type, boolean slim, Object old) {
        try {
            for (Object o : type.getEnumConstants()) {
                java.util.List<String> names = new ArrayList<>();
                names.add(o.toString()); names.add(((Enum<?>) o).name());
                for (var m : type.getMethods())
                    if (m.getParameterCount() == 0 && m.getReturnType() == String.class) { try { names.add(String.valueOf(m.invoke(o))); } catch (Throwable ignored) { } }
                for (String n : names) {
                    String l = n.toLowerCase();
                    if (slim ? l.equals("slim") : (l.equals("default") || l.equals("wide"))) return o;
                }
            }
        } catch (Throwable ignored) { }
        return old;
    }
}
