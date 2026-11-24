package com.sirens.visuals.hud;

import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.fx.Visuals;
import com.sirens.visuals.gui.UI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Draws the draggable widgets and positions them. */
public final class Hud {
    private Hud() { }

    public static int fps, frames;
    static long fpsT = System.currentTimeMillis();
    public static float dt;
    static long lastNs = System.nanoTime();
    /** Set while the HUD editor is open so widgets are not drawn twice. */
    public static boolean suppress;

    public static void frame() {
        frames++;
        long now = System.currentTimeMillis();
        if (now - fpsT >= 1000) { fps = frames; frames = 0; fpsT = now; }
        long n = System.nanoTime();
        dt = Mth.clamp((n - lastNs) / 1e9f, 0.0005f, 0.1f);
        lastNs = n;
    }

    public static boolean cinematicHidden() {
        return Mods.CINE.on() && Mods.CI_HUD.on() && Visuals.cinematicOn;
    }

    /** Draws underlay screen effects (before vanilla HUD). */
    public static void under(GuiGraphics g, Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        frame();
        Overlays.under(g, mc);
    }

    /** Draws widgets + crosshair feedback (before chat). */
    public static void over(GuiGraphics g, Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        Overlays.over(g, mc);
        if (suppress || cinematicHidden()) return;
        for (HudElem e : HudElems.ALL) {
            if (!e.enabled()) continue;
            Canvas c = e.build(mc, false);
            if (c == null || c.empty()) continue;
            draw(g, mc, e, c);
        }
    }

    public static float scaleOf(HudElem e) { return Mods.HS_SCALE.f() * e.h.scale.f(); }
    public static int padOf(Canvas c) { return c.noPanel ? 0 : Mods.HS_PAD.i(); }

    /** Draws one widget at its saved position and records its bounds. */
    public static void draw(GuiGraphics g, Minecraft mc, HudElem e, Canvas c) {
        int GW = mc.getWindow().getGuiScaledWidth(), GH = mc.getWindow().getGuiScaledHeight();
        float s = scaleOf(e);
        int pad = padOf(c), W = c.w + pad * 2, H = c.h + pad * 2;
        float sw = W * s, sh = H * s;
        int align = Math.round(e.h.pa.f());
        float ax = e.h.px.f() * GW, ay = e.h.py.f() * GH;
        float x = align == 0 ? ax : align == 1 ? ax - sw / 2 : ax - sw;
        x = Mth.clamp(x, 0, Math.max(0, GW - sw));
        float y = Mth.clamp(ay, 0, Math.max(0, GH - sh));
        e.bx = (int) x; e.by = (int) y; e.bw = (int) Math.ceil(sw); e.bh = (int) Math.ceil(sh);

        UI.pushScale(g, x, y, s);
        if (!c.noPanel) Style.panel(g, 0, 0, W, H, 0.35f + 0.25f * UI.pulse(1.1f));
        c.draw(g, pad, pad, c.w);
        UI.pop(g);
    }

    /** Stores a new position for a widget given its desired top-left on screen. */
    public static void place(HudElem e, Minecraft mc, float x, float y, float sw, float sh) {
        int GW = mc.getWindow().getGuiScaledWidth(), GH = mc.getWindow().getGuiScaledHeight();
        x = Mth.clamp(x, 0, Math.max(0, GW - sw));
        y = Mth.clamp(y, 0, Math.max(0, GH - sh));
        float cx = x + sw / 2;
        int align = cx < GW / 3f ? 0 : cx > GW * 2 / 3f ? 2 : 1;
        float ax = align == 0 ? x : align == 1 ? cx : x + sw;
        e.h.px.set(ax / GW); e.h.py.set(y / GH); e.h.pa.set(align);
    }

    public static void resetPos(HudElem e) {
        e.h.px.reset(); e.h.py.reset(); e.h.pa.reset(); e.h.scale.reset();
    }
}
