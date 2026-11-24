package com.sirens.visuals.fx;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Replaces the enchantment glint textures with a recoloured version. Best effort. */
public final class Glint {
    private Glint() { }
    private static int lastKey = Integer.MIN_VALUE;
    private static boolean failed;
    private static final String[] IDS = {"textures/misc/enchanted_glint_item.png", "textures/misc/enchanted_glint_armor.png"};

    public static void tick(Minecraft mc) {
        if (failed || mc.level == null) return;
        int key = Mods.GLINT.on() ? java.util.Objects.hash(Mods.G_STYLE.idx(), Mods.G_C1.rgb(), Mods.G_C2.rgb(), Mods.G_OPA.f(), Mods.G_SCALE.f()) : 0;
        if (key == lastKey) return;
        lastKey = key;
        try {
            for (String path : IDS) {
                Identifier id = Identifier.fromNamespaceAndPath("minecraft", path);
                if (key == 0) { mc.getTextureManager().release(id); continue; }
                BufferedImage src;
                try (InputStream in = mc.getResourceManager().getResource(id).orElseThrow().open()) { src = ImageIO.read(in); }
                Skins.registerAs(Skins.toPng(recolor(src)), id);
            }
        } catch (Throwable t) { failed = true; }
    }

    static BufferedImage recolor(BufferedImage src) {
        int s = Math.max(1, Math.round(Mods.G_SCALE.f()));
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int style = Mods.G_STYLE.idx();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int p = src.getRGB(x, y);
                float lum = (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / 765f;
                float a = ((p >>> 24) & 255) / 255f;
                float u = x / (float) w, v = y / (float) h;
                float m = lum;
                if (style == 1) m = ((x * s + y * s) / 4) % 2 == 0 ? 0.9f : 0.15f;
                else if (style == 2) m = (float) Math.pow(lum, 3) * 2.5f + (((x * 7 + y * 13) % 11) == 0 ? 0.8f : 0f);
                int col = switch (style) {
                    case 3 -> Fx.rainbow(u + v);
                    case 4 -> Fx.lerpRgb(Mods.G_C1.rgb(), Mods.G_C2.rgb(), (u + v) / 2f);
                    default -> Mods.G_C1.rgb();
                };
                m = Math.min(1f, m * Mods.G_OPA.f());
                int r = (int) (((col >> 16) & 255) * m), g = (int) (((col >> 8) & 255) * m), b = (int) ((col & 255) * m);
                o.setRGB(x, y, ((int) (a * 255) << 24) | (r << 16) | (g << 8) | b);
            }
        return o;
    }
}
