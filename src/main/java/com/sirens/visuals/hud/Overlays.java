package com.sirens.visuals.hud;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.MusicBridge;
import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.fx.SkyFx;
import com.sirens.visuals.fx.Visuals;
import com.sirens.visuals.gui.UI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Full-screen effects and crosshair feedback. */
public final class Overlays {
    private Overlays() { }

    static final Identifier FIRE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/fire_1.png");
    static float uw, aur, fireK;

    static float time() { return (System.currentTimeMillis() % 3_600_000L) / 1000f; }

    // ===================================================================== underlay
    public static void under(GuiGraphics g, Minecraft mc) {
        LocalPlayer p = mc.player;
        int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
        if (Hud.cinematicHidden()) return;
        wet(g, W, H);
        underwater(g, mc, p, W, H);
        lowFire(g, mc, p, W, H);
        vignette(g, p, W, H);
        sky(g, mc, p, W, H);
        aurora(g, mc, p, W, H);
        skyLyrics(g, mc, p, W, H);
    }

    static void wet(GuiGraphics g, int W, int H) {
        if (!Mods.WET.on() || !Mods.WET_GRADE.on() || Visuals.wet < 0.02f) return;
        int a = (int) (Visuals.wet * Mods.WET_STR.f() * 120);
        g.fill(0, 0, W, H, UI.argb(a, Mods.WET_COL.rgb()));
        // slightly cooler, softer edges
        int e = H / 4, a2 = (int) (Visuals.wet * Mods.WET_STR.f() * 90);
        g.fillGradient(0, H - e, W, H, UI.argb(0, 0x0A1426), UI.argb(a2, 0x0A1426));
    }

    /** The sky image is a real skybox now (SkyDome + SkyRendererMixin); this only keeps the image loader in sync. */
    static void sky(GuiGraphics g, Minecraft mc, LocalPlayer p, int W, int H) {
        if (Mods.SKY.on() && Mods.S_IMG_ON.on()) SkyFx.sync();
    }

    // ===================================================================== lyrics in the sky
    static float lyrVis;
    static String lyrSeenState = ""; static long lyrStateAt;

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    /**
     * Big glowing lyric line in the upper sky area of your view, with a dimmer preview of the next line. It sits at a
     * fixed screen position (no world anchoring, so nothing swings around) and drifts toward the centre as you look up.
     */
    static void skyLyrics(GuiGraphics g, Minecraft mc, LocalPlayer p, int W, int H) {
        if (!Mods.MUSIC.on() || !Mods.MU_SKY.on() || !Mods.MU_LYR.on()) return;
        if (!MusicBridge.isActive() || MusicBridge.ad) return;
        var lines = MusicBridge.lines;
        float t = time();
        float look = Mth.clamp(-p.getXRot(), 0f, 70f);
        float vis = Mods.MU_SKY_PITCH.f() <= 0f ? 1f : Mth.clamp((look - Mods.MU_SKY_PITCH.f()) / 20f, 0f, 1f);
        lyrVis = UI.ease(lyrVis, vis, 6f);
        if (lyrVis < 0.02f) return;
        float cx = W / 2f, cy = H * 0.16f + (look / 70f) * H * 0.26f;

        // status line while lyrics are loading / missing, so you can tell the bridge is working
        String st = MusicBridge.lyricState;
        if (!st.equals(lyrSeenState)) { lyrSeenState = st; lyrStateAt = System.currentTimeMillis(); }
        if (lines.isEmpty()) {
            if (!st.isEmpty() && System.currentTimeMillis() - lyrStateAt < 6000)
                skyLine(g, "\u266a lyrics: " + st, cx, cy, 1.0f, 0.55f * lyrVis, t, false);
            return;
        }

        double pos = MusicBridge.pos();
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) { if (lines.get(i).t() <= pos) idx = i; else break; }
        boolean intro = idx < 0;
        if (intro) { // before the first line: preview it
            String first = lines.get(0).s() == null ? "" : lines.get(0).s().trim();
            double lead = lines.get(0).t() - pos;
            if (!first.isEmpty() && lead < 6.0 && Mods.MU_SKY_NEXT.on())
                skyLine(g, first, cx, cy, Math.min(Mods.MU_SKY_SIZE.f() * 0.55f, W * 0.8f / Math.max(1, UI.tw(first))), 0.45f * lyrVis * smooth((float) (1.5 - lead / 4)), t, false);
            return;
        }
        var cur = lines.get(idx);
        boolean hasNext = idx + 1 < lines.size();
        double nextT = hasNext ? lines.get(idx + 1).t() : Double.MAX_VALUE;
        double age = pos - cur.t();
        float in = smooth((float) (age / 0.35)), out = hasNext ? 1f : smooth((float) ((8.0 - age) / 0.9));
        if (hasNext && nextT - pos < 0.25) out = smooth((float) ((nextT - pos) / 0.25));   // quick crossfade only right at the change
        String text = cur.s() == null ? "" : cur.s().trim();
        float a = Math.min(in, out);
        float sc = Mods.MU_SKY_SIZE.f();
        if (!text.isEmpty() && a > 0.02f) {
            int tw = UI.tw(text);
            if (tw * sc > W * 0.9f) sc = W * 0.9f / tw;
            skyLine(g, text, cx, cy + (1f - in) * 6f, sc, a * lyrVis, t, true);
        }
        if (Mods.MU_SKY_NEXT.on() && hasNext) {
            String nx = lines.get(idx + 1).s() == null ? "" : lines.get(idx + 1).s().trim();
            if (!nx.isEmpty()) {
                float nsc = Math.min(Mods.MU_SKY_SIZE.f() * 0.5f, W * 0.8f / Math.max(1, UI.tw(nx)));
                float near = 0.32f + 0.2f * smooth((float) (1.0 - (nextT - pos) / 2.0));
                skyLine(g, nx, cx, cy + 10 * Math.max(sc, 1f) + 8, nsc, near * lyrVis * smooth((float) (age / 0.5)), t, false);
            }
        }
    }

    /** One glowing, accent-gradient line of text centred on (cx, cy). */
    private static void skyLine(GuiGraphics g, String s, float cx, float cy, float sc, float alpha, float t, boolean main) {
        if (alpha <= 0.02f) return;
        var font = UI.font();
        int w = font.width(s);
        UI.pushScale(g, cx - w * sc / 2f, cy - 4f * sc, sc);
        int n = s.length(), x = 0;
        // bloom: three offset passes, widest first, very low alpha
        if (main) {
            for (int pass = 3; pass >= 1; pass--) {
                int ga = (int) (alpha * (40f / pass));
                if (ga < 4) continue;
                int xx = 0;
                for (int i = 0; i < n; i++) {
                    String ch = String.valueOf(s.charAt(i));
                    int gc = UI.argb(ga, UI.mix(UI.accent(), UI.accent2(), i / (float) Math.max(1, n - 1)) & 0xFFFFFF);
                    g.drawString(font, ch, xx - pass, 0, gc, false); g.drawString(font, ch, xx + pass, 0, gc, false);
                    g.drawString(font, ch, xx, -pass, gc, false); g.drawString(font, ch, xx, pass, gc, false);
                    xx += font.width(ch);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            String ch = String.valueOf(s.charAt(i));
            float k = i / (float) Math.max(1, n - 1);
            int base = UI.mix(UI.accent(), UI.accent2(), (float) (0.5 + 0.5 * Math.sin(t * 0.8 + k * 3.0)));
            int c = UI.mix(base, 0xFFFFFFFF, main ? 0.72f : 0.35f);
            int ca = Mth.clamp((int) (255 * alpha), 0, 255);
            if (ca >= 8) g.drawString(font, ch, x, 0, (ca << 24) | (c & 0xFFFFFF), main);
            x += font.width(ch);
        }
        UI.pop(g);
    }

    static float depthK;

    /** Soft underwater look: gentle tint, slow smooth caustics, faint layered light shafts. Nothing flashes or flickers. */
    static void underwater(GuiGraphics g, Minecraft mc, LocalPlayer p, int W, int H) {
        boolean in = Mods.WATER.on() && p.isUnderWater();
        uw = UI.ease(uw, in ? 1f : 0f, 2.5f);
        if (uw < 0.01f) return;
        depthK = UI.ease(depthK, Mth.clamp((63f - (float) p.getEyeY()) / 70f, 0f, 1f), 1.5f);
        float depth = depthK;
        float t = time() * 0.5f;                       // everything below moves slowly
        int tint = Mods.W_OVERC.rgb();
        if (Mods.W_OVER.on()) {
            // top-light to bottom-dark gradient instead of a flat slab
            int a = (int) (uw * Mods.W_OVERS.f() * 160);
            g.fillGradient(0, 0, W, H, UI.argb((int) (a * 0.75f), tint), UI.argb(a, UI.mix(0xFF000000 | tint, 0xFF000814, 0.45f) & 0xFFFFFF));
        }
        if (Mods.W_DEPTH.on()) g.fillGradient(0, 0, W, H, UI.argb((int) (uw * depth * 40), 0x00101E), UI.argb((int) (uw * depth * 95), 0x00060F));
        int e = H / 3;
        g.fillGradient(0, H - e, W, H, UI.argb(0, 0x021428), UI.argb((int) (uw * 38), 0x021428));
        float light = (1f - depth * 0.75f) * uw;
        if (Mods.W_RAYS.on() && light > 0.02f) {
            int rays = 5, step = 8;
            for (int i = 0; i < rays; i++) {
                float x0 = W * (i + 0.5f) / rays + (float) Math.sin(t * 0.3 + i * 1.7) * 22f;
                float wid = 26 + 10 * (float) Math.sin(t * 0.25 + i * 2.1);
                float slope = 0.28f + 0.05f * (float) Math.sin(t * 0.15 + i);
                float pw = 0.75f + 0.25f * (float) Math.sin(t * 0.4 + i * 1.3);   // slow breathing, never off
                for (int y = 0; y < H; y += step) {
                    float k = 1f - (float) y / H;
                    float base = 13f * light * k * k * pw;
                    if (base < 0.6f) continue;
                    float cx = x0 + y * slope, ww = wid + y * 0.1f;
                    for (int layer = 0; layer < 3; layer++) {      // 3 nested widths = soft edges
                        float lw = ww * (1.0f - layer * 0.28f);
                        g.fill((int) (cx - lw / 2), y, (int) (cx + lw / 2), y + step, UI.argb((int) (base * (0.55f + layer * 0.25f)), 0xCFEFFF));
                    }
                }
            }
        }
        if (Mods.W_CAUS.on() && light > 0.02f) {
            int cell = 10;
            for (int y = 0; y < H; y += cell) {
                float fy = 1f - 0.6f * (float) y / H;          // stronger toward the top, like light from the surface
                for (int x = 0; x < W; x += cell) {
                    double v = Math.sin(x * 0.035 + t * 0.8) + Math.sin(y * 0.045 - t * 0.6) + Math.sin((x + y) * 0.022 + t * 0.5) + Math.sin((x - y) * 0.03 - t * 0.7);
                    double ridge = Math.max(0.0, 1.0 - Math.abs(v) * 0.85);     // thin bright ridges where waves cancel
                    ridge = ridge * ridge * ridge;
                    int a = (int) (ridge * 26 * light * fy);
                    if (a > 1) g.fill(x, y, x + cell, y + cell, UI.argb(a, 0xE4F8FF));
                }
            }
        }
    }

    static void lowFire(GuiGraphics g, Minecraft mc, LocalPlayer p, int W, int H) {
        boolean show = Mods.LOWFIRE.on() && p.isOnFire() && Visuals.tickN - Visuals.fireTick < 8;
        fireK = UI.ease(fireK, show ? 1f : 0f, 10f);
        if (fireK < 0.02f) return;
        int T = Math.max(16, (int) (H * Mods.LF_HEIGHT.f()));
        if (Mods.LF_GLOW.on())
            g.fillGradient(0, H - (int) (T * 1.5f), W, H, UI.argb(0, 0xFF5A00), UI.argb((int) (110 * fireK), 0xFF5A00));
        float sx = T / 16f * (4f / Mods.LF_TILE.f()), sy = T / 16f * fireK;
        int colW = Math.max(8, (int) (16 * sx));
        int cols = W / colW + 2;
        int tick = Visuals.tickN;
        for (int c = 0; c < cols; c++) {
            int frame = Math.floorMod(tick / 2 + c * 7, 32);
            UI.pushScale2(g, c * colW - colW / 2f, H - T * fireK + (1f - fireK) * 0, sx, sy);
            g.blit(RenderPipelines.GUI_TEXTURED, FIRE, 0, 0, 0f, (float) (frame * 16), 16, 16, 16, 512);
            UI.pop(g);
        }
    }

    static void vignette(GuiGraphics g, LocalPlayer p, int W, int H) {
        if (!Mods.LOWHP.on()) return;
        float lim = Mods.LOWHP_AT.f(), hp = p.getHealth();
        if (hp > lim) return;
        float k = Mth.clamp((lim - hp) / lim, 0f, 1f);
        // heartbeat: two quick beats then a rest, faster when lower
        double ph = (System.currentTimeMillis() / 1000.0) * (1.1 + k * 1.2) % 1.0;
        float beat = (float) (Math.exp(-Math.pow((ph - 0.08) * 14, 2)) + 0.65 * Math.exp(-Math.pow((ph - 0.30) * 14, 2)));
        float pulse = 0.55f + 0.45f * Mth.clamp(beat, 0f, 1f);
        int c = Mods.LOWHP_COL.rgb();
        float str = Mods.LOWHP_STR.f() * k * pulse;
        int depth = (int) (Math.min(W, H) * (0.22f + 0.10f * k));
        // soft edge falloff: thin strips with quadratic alpha, all four sides
        for (int i = 0; i < depth; i += 2) {
            float f = 1f - i / (float) depth;
            int al = (int) (150 * str * f * f);
            if (al < 2) continue;
            int col = UI.argb(al, c);
            g.fill(0, i, W, i + 2, col);
            g.fill(0, H - i - 2, W, H - i, col);
            g.fill(0, i + 2, 2 + i, H - i - 2, col);
            g.fill(W - i - 2, i + 2, W, H - i - 2, col);
        }
        // faint bright rim so it reads as neon rather than mud
        UI.goutline(g, 1, 1, W - 2, H - 2, 0, UI.argb((int) (90 * str), c), UI.argb((int) (90 * str), c));
    }

    static void aurora(GuiGraphics g, Minecraft mc, LocalPlayer p, int W, int H) {
        if (!Mods.AURORA.on()) return;
        boolean night = true;
        if (Mods.A_NIGHT.on() && mc.level != null) { long t = ((mc.level.getDayTime() % 24000L) + 24000L) % 24000L; night = t > 12500 && t < 23500; }
        float up = night ? Mth.clamp((-p.getXRot() - 8f) / 40f, 0, 1) : 0;
        aur = UI.ease(aur, up, 6f);
        if (aur <= 0.01f) return;
        float t = time() * Mods.A_SPEED.f();
        int step = 4, mode = Mods.A_COLOR.idx();
        for (int x = 0; x < W; x += step) {
            float w1 = (float) Math.sin(x * 0.02 + t * 0.8) * 0.5f + 0.5f, w2 = (float) Math.sin(x * 0.05 - t * 1.3) * 0.5f + 0.5f;
            int h = (int) (H * (0.15f + 0.3f * w1 * aur)), top = (int) (H * 0.05f + w2 * H * 0.1f);
            int al = (int) (Mods.A_STR.f() * aur * (70 + 110 * w2));
            float hue = switch (mode) {
                case 1 -> 0.75f + 0.08f * w1;
                case 2 -> 0.5f + 0.05f * w1;
                case 3 -> (t * 0.03f + x / (float) W * 0.8f) % 1f;
                default -> 0.33f + 0.2f * w1;
            };
            int c = Mth.hsvToRgb(hue, 0.7f, 1f);
            g.fillGradient(x, top, x + step, top + h / 2, UI.argb(0, c), UI.argb(al, c));
            g.fillGradient(x, top + h / 2, x + step, top + h, UI.argb(al, c), UI.argb(0, c));
        }
    }

    // ===================================================================== crosshair feedback
    public static void over(GuiGraphics g, Minecraft mc) {
        int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
        if (Mods.DMG.on()) {
            float s = Mods.DMG_SCALE.f();
            for (Visuals.Pop q : Visuals.pops) {
                float k = q.age / 25f;
                UI.pushScale(g, W / 2f + 14 + q.dx, H / 2f - 6 - q.age * 0.9f * Mods.DMG_RISE.f(), s);
                g.drawString(mc.font, q.s, 0, 0, UI.argb((int) (255 * (1 - k)), Mods.DMG_COL.rgb()), true);
                UI.pop(g);
            }
        }
        if (Mods.HITMARK.on() && Visuals.marker > 0) {
            int a = 255 * Visuals.marker / 6, c = UI.argb(a, Mods.HM_COL.rgb()), cx = W / 2, cy = H / 2;
            int gap = 3, len = Math.max(2, (int) (5 * Mods.HM_SIZE.f()));
            int gr = gap + len + 3;
            UI.glow(g, cx - gr, cy - gr, gr * 2, gr * 2, gr, Mods.HM_COL.rgb(), 0.55f * Visuals.marker / 6f);
            switch (Mods.HM_STYLE.idx()) {
                case 1 -> {
                    g.fill(cx - gap - len, cy, cx - gap, cy + 1, c); g.fill(cx + gap + 1, cy, cx + gap + len + 1, cy + 1, c);
                    g.fill(cx, cy - gap - len, cx + 1, cy - gap, c); g.fill(cx, cy + gap + 1, cx + 1, cy + gap + len + 1, c);
                }
                case 2 -> {
                    int r = (int) (7 * Mods.HM_SIZE.f()) + (6 - Visuals.marker);
                    for (int i = 0; i < 36; i++) {
                        double an = i * Math.PI / 18;
                        int px = cx + (int) Math.round(Math.cos(an) * r), py = cy + (int) Math.round(Math.sin(an) * r);
                        g.fill(px, py, px + 1, py + 1, c);
                    }
                }
                default -> {
                    for (int i = 0; i < len; i++) {
                        int o = gap + i;
                        g.fill(cx - o, cy - o, cx - o + 1, cy - o + 1, c); g.fill(cx + o, cy - o, cx + o + 1, cy - o + 1, c);
                        g.fill(cx - o, cy + o, cx - o + 1, cy + o + 1, c); g.fill(cx + o, cy + o, cx + o + 1, cy + o + 1, c);
                    }
                }
            }
        }
    }
}
