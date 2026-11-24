package com.sirens.visuals.fx;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/** Generates procedural 64x32 cape textures (the cape back face occupies x 1..10, y 1..16 of the sheet). */
public final class Capes {
    private Capes() { }

    public static byte[] generate(int pat, int c1, int c2) throws Exception {
        BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        int W = 64, H = 32;
        Random rnd = new Random(1234);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                float u = x / (float) (W - 1), v = y / (float) (H - 1);
                int c = switch (pat) {
                    case 0 -> c1;
                    case 1 -> (x / 2) % 2 == 0 ? c1 : c2;
                    case 2 -> (y / 2) % 2 == 0 ? c1 : c2;
                    case 3 -> Fx.lerpRgb(c1, c2, v);
                    case 4 -> ((x + y) / 3) % 2 == 0 ? c1 : c2;
                    case 5 -> ((x / 2) + (y / 2)) % 2 == 0 ? c1 : c2;
                    case 6 -> { // galaxy
                        int base = Fx.lerpRgb(c1, 0x05030F, 0.55f + 0.3f * (float) Math.sin(u * 5 + v * 3));
                        if (rnd.nextInt(28) == 0) base = Fx.lerpRgb(base, c2, 0.9f);
                        yield Fx.lerpRgb(base, c2, 0.25f * (float) Math.max(0, Math.sin(u * 8 - v * 6)));
                    }
                    case 7 -> { // flames
                        float f = v + 0.18f * (float) Math.sin(u * 40) * (1 - v) ;
                        yield Fx.lerpRgb(c2, c1, Math.max(0, Math.min(1, f * 1.3f)));
                    }
                    default -> Fx.lerpRgb(c1, c2, 0.5f + 0.5f * (float) Math.sin(u * 30 + v * 10));
                };
                img.setRGB(x, y, 0xFF000000 | c);
            }
        // darker border on the cape face for definition
        for (int x = 1; x <= 10; x++) { img.setRGB(x, 1, shade(img.getRGB(x, 1))); img.setRGB(x, 16, shade(img.getRGB(x, 16))); }
        for (int y = 1; y <= 16; y++) { img.setRGB(1, y, shade(img.getRGB(1, y))); img.setRGB(10, y, shade(img.getRGB(10, y))); }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", o);
        return o.toByteArray();
    }

    static int shade(int argb) {
        int r = ((argb >> 16) & 255) * 3 / 4, g = ((argb >> 8) & 255) * 3 / 4, b = (argb & 255) * 3 / 4;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------------------------------------ import fitting
    /**
     * Converts any imported image to a valid cape sheet (64k x 32k, k = 1..8) that also carries the elytra wing.
     * Handles: full sheets (64x32, 128x64, HD), cropped 22x17 capes (and multiples), and arbitrary artwork of any
     * aspect ratio (centre-cropped to the cape / wing faces).
     */
    public static byte[] fit(byte[] png) throws Exception {
        BufferedImage src = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        if (src == null) throw new IllegalStateException("not a readable image");
        int w = src.getWidth(), h = src.getHeight();
        double r = w / (double) h;
        BufferedImage out;
        if (Math.abs(r - 2.0) < 0.06 && w >= 32) {                       // full sheet at any resolution
            int k = Math.max(1, Math.min(8, (int) Math.round(w / 64.0)));
            if (w == 64 * k && h == 32 * k) return png;
            out = new BufferedImage(64 * k, 32 * k, BufferedImage.TYPE_INT_ARGB);
            draw(out, src, 0, 0, w, h, 0, 0, 64 * k, 32 * k);
        } else if (Math.abs(r - 22.0 / 17.0) < 0.05) {                   // cropped cape-only image
            int k = Math.max(1, Math.min(8, (int) Math.round(w / 22.0)));
            out = new BufferedImage(64 * k, 32 * k, BufferedImage.TYPE_INT_ARGB);
            draw(out, src, 0, 0, w, h, 0, 0, 22 * k, 17 * k);
            wingFromCape(out, k);
        } else {                                                         // artwork: wrap onto cape + wing
            int k = Math.max(1, Math.min(8, (int) Math.round(Math.max(w / 64.0, h / 32.0) / 4)));
            out = new BufferedImage(64 * k, 32 * k, BufferedImage.TYPE_INT_ARGB);
            int avg = average(src);
            fill(out, 0, 0, 22 * k, 17 * k, shade(avg));
            fill(out, 22 * k, 0, 24 * k, 22 * k, shade(avg));
            cover(out, src, 12 * k, 1 * k, 10 * k, 16 * k);
            cover(out, src, 1 * k, 1 * k, 10 * k, 16 * k);
            cover(out, src, 24 * k, 2 * k, 10 * k, 20 * k);
            cover(out, src, 36 * k, 2 * k, 10 * k, 20 * k);
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(out, "png", o);
        return o.toByteArray();
    }

    /** Stretches the cape back face over both wing faces so the elytra matches the cape. */
    private static void wingFromCape(BufferedImage out, int k) {
        BufferedImage face = out.getSubimage(12 * k, 1 * k, 10 * k, 16 * k);
        fill(out, 22 * k, 0, 24 * k, 22 * k, shade(average(face)));
        draw(out, face, 0, 0, face.getWidth(), face.getHeight(), 24 * k, 2 * k, 10 * k, 20 * k);
        draw(out, face, 0, 0, face.getWidth(), face.getHeight(), 36 * k, 2 * k, 10 * k, 20 * k);
    }

    private static void draw(BufferedImage dst, BufferedImage src, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
        BufferedImage s = src;
        if (sw > dw * 2 || sh > dh * 2) {      // area-average first so big images do not alias
            int tw = Math.max(dw, 1), th = Math.max(dh, 1);
            java.awt.Image sc = src.getSubimage(sx, sy, sw, sh).getScaledInstance(tw, th, java.awt.Image.SCALE_AREA_AVERAGING);
            BufferedImage b = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = b.createGraphics(); gg.drawImage(sc, 0, 0, null); gg.dispose();
            s = b; sx = 0; sy = 0; sw = tw; sh = th;
        }
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(s, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);
        g.dispose();
    }

    /** Centre-crops src to the aspect of the target rect, then scales it in. */
    private static void cover(BufferedImage dst, BufferedImage src, int dx, int dy, int dw, int dh) {
        double ta = dw / (double) dh, sa = src.getWidth() / (double) src.getHeight();
        int cw = src.getWidth(), ch = src.getHeight();
        if (sa > ta) cw = (int) Math.round(ch * ta); else ch = (int) Math.round(cw / ta);
        cw = Math.max(1, cw); ch = Math.max(1, ch);
        draw(dst, src, (src.getWidth() - cw) / 2, (src.getHeight() - ch) / 2, cw, ch, dx, dy, dw, dh);
    }

    private static void fill(BufferedImage img, int x, int y, int w, int h, int argb) {
        Graphics2D g = img.createGraphics(); g.setColor(new Color(argb, true)); g.fillRect(x, y, w, h); g.dispose();
    }

    private static int average(BufferedImage img) {
        long r = 0, g = 0, b = 0, n = 0;
        int sx = Math.max(1, img.getWidth() / 32), sy = Math.max(1, img.getHeight() / 32);
        for (int y = 0; y < img.getHeight(); y += sy) for (int x = 0; x < img.getWidth(); x += sx) {
            int p = img.getRGB(x, y); if ((p >>> 24) < 16) continue;
            r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255; n++;
        }
        if (n == 0) return 0xFF202030;
        return 0xFF000000 | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
    }
}
