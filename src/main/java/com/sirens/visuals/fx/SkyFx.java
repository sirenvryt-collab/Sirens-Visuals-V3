package com.sirens.visuals.fx;

import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.Hooks;
import com.sirens.visuals.cfg.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SkyFx {
    private SkyFx() { }

    public static Long time(long vanilla) {
        if (!Mods.SKY.on()) return null;
        long day = Math.floorDiv(vanilla, 24000L);
        int ph = Mods.S_PHASE.idx();
        if (ph > 0) day = ph - 1; // vanilla moon phase = (dayTime / 24000) % 8
        return switch (Mods.S_MODE.idx()) {
            case 1 -> day * 24000L + Mods.S_TIME.i();
            case 2 -> {
                long t = (long) (vanilla * (double) Mods.S_SPEED.f());
                yield ph > 0 ? (day * 24000L + Math.floorMod(t, 24000L)) : t;
            }
            default -> ph > 0 ? day * 24000L + Math.floorMod(vanilla, 24000L) : null;
        };
    }

    // ------------------------------------------------------------------------------------------ sky image
    public static String status = "No sky image. Use Choose Sky Image... or drop files in the sky folder.";
    /** Texture of the loaded image (null = none / failed) and its size; avg = average colour for dome tinting. */
    public static volatile Identifier imgId;
    public static volatile int imgW, imgH, imgAvg = -1;
    private static volatile String loadedName = "", loading = "", failed = "";

    public static Path dir() { Path p = Config.dir().resolve("sky"); try { Files.createDirectories(p); } catch (Exception ignored) { } return p; }

    public static void scan() {
        List<String> l = new ArrayList<>();
        l.add("(none)");
        try (var st = Files.list(dir())) {
            st.map(f -> f.getFileName().toString()).filter(n -> { String x = n.toLowerCase(); return x.endsWith(".png") || x.endsWith(".jpg") || x.endsWith(".jpeg"); }).sorted().forEach(l::add);
        } catch (Exception ignored) { }
        Mods.S_IMG.setOpts(l.toArray(new String[0]));
        int at = l.indexOf(Mods.S_IMGFILE.get());
        Mods.S_IMG.set(Math.max(0, at));
        if (at < 0) Mods.S_IMGFILE.set("");
    }

    /** Copies a picked file into the sky folder and selects it. */
    public static void useFile(Path src) {
        try {
            Path dst = dir().resolve(src.getFileName().toString());
            if (!src.toAbsolutePath().normalize().equals(dst.toAbsolutePath().normalize())) Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            failed = ""; loadedName = "";
            Mods.S_IMGFILE.set(dst.getFileName().toString());
            scan();
            Mods.SKY.enabled.set(true);
        } catch (Exception e) { status = "Could not use file: " + e.getMessage(); }
    }

    /** Called every frame from the overlay: follows the dropdown and (re)loads the image on a worker thread. */
    public static void sync() {
        String sel = Mods.S_IMG.value();
        if (!sel.equals(Mods.S_IMGFILE.get()) && Mods.S_IMG.opts.length > 0) { Mods.S_IMGFILE.set("(none)".equals(sel) ? "" : sel); }
        String want = Mods.S_IMGFILE.get();
        if (want.isEmpty()) { if (imgId != null) { imgId = null; imgAvg = -1; } loadedName = ""; return; }
        if (want.equals(loadedName) || want.equals(loading) || want.equals(failed)) return;
        loading = want;
        status = "Loading " + want + "...";
        Thread t = new Thread(() -> {
            try {
                BufferedImage src = javax.imageio.ImageIO.read(dir().resolve(want).toFile());
                if (src == null) throw new IllegalStateException("unreadable image");
                int w = src.getWidth(), h = src.getHeight();
                double k = Math.min(1.0, 2048.0 / Math.max(w, h));
                int nw = Math.max(16, (int) (w * k)), nh = Math.max(16, (int) (h * k));
                BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = out.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(k < 0.5 ? src.getScaledInstance(nw, nh, java.awt.Image.SCALE_AREA_AVERAGING) : src, 0, 0, nw, nh, null);
                g.dispose();
                long r = 0, gg = 0, b = 0, c = 0;
                for (int y = 0; y < nh; y += Math.max(1, nh / 24)) for (int x = 0; x < nw; x += Math.max(1, nw / 24)) {
                    int p = out.getRGB(x, y); r += (p >> 16) & 255; gg += (p >> 8) & 255; b += p & 255; c++;
                }
                int avg = c == 0 ? -1 : (int) (r / c) << 16 | (int) (gg / c) << 8 | (int) (b / c);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(out, "png", bo);
                byte[] png = bo.toByteArray();
                Minecraft.getInstance().execute(() -> {
                    try {
                        Skins.registerAs(png, SkyDome.TEX); imgId = SkyDome.TEX;
                        imgW = nw; imgH = nh; imgAvg = avg; loadedName = want; loading = "";
                        status = "Sky image: " + want + " (" + nw + "x" + nh + ")";
                    } catch (Throwable e) { fail(want, e); }
                });
            } catch (Throwable e) { Minecraft.getInstance().execute(() -> fail(want, e)); }
        }, "sirens-sky");
        t.setDaemon(true);
        t.start();
    }

    private static void fail(String name, Throwable e) {
        failed = name; loading = ""; imgId = null; imgAvg = -1;
        status = "Sky image failed (" + name + "): " + e.getMessage();
        Hooks.error("sky image", e);
    }

    public static Vec3 skyColor(Vec3 v) {
        if (Mods.SKY.on() && Mods.S_IMG_ON.on() && Mods.S_IMG_TINT.on() && imgAvg >= 0 && imgId != null) {
            double r = ((imgAvg >> 16) & 255) / 255.0, g = ((imgAvg >> 8) & 255) / 255.0, b = (imgAvg & 255) / 255.0;
            double lum = Math.min(1.2, Math.max(0.15, (v.x + v.y + v.z) / 3.0 / 0.6));
            return new Vec3(v.x + (r * lum - v.x) * 0.5, v.y + (g * lum - v.y) * 0.5, v.z + (b * lum - v.z) * 0.5);
        }
        if (!Mods.SKY.on() || !Mods.S_TINT.on()) return null;
        int c = Mods.S_TINTC.rgb();
        float s = Mods.S_TINTS.f();
        double r = ((c >> 16) & 255) / 255.0, g = ((c >> 8) & 255) / 255.0, b = (c & 255) / 255.0;
        double lum = Math.max(0.15, (v.x + v.y + v.z) / 3.0 / 0.6);
        lum = Math.min(1.2, lum);
        return new Vec3(v.x + (r * lum - v.x) * s, v.y + (g * lum - v.y) * s, v.z + (b * lum - v.z) * s);
    }

    public static Float stars(float vanilla) {
        if (!Mods.SKY.on() || Mods.S_STARS.f() <= 0f) return null;
        return Math.max(vanilla, Mods.S_STARS.f());
    }
}
