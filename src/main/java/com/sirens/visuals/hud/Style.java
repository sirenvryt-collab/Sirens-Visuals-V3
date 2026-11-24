package com.sirens.visuals.hud;

import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.gui.UI;

public final class Style {
    private Style() { }
    public static int text() { return 0xFF000000 | Mods.HS_TEXT.rgb(); }
    public static int accent() { return 0xFF000000 | Mods.HS_ACCENT.rgb(); }
    public static boolean shadow() { return Mods.HS_SHADOW.on(); }
    public static int bg() { return UI.argb((int) (Mods.HS_BGA.f() * 255), Mods.HS_BGC.rgb()); }
    public static int dim() { return UI.alpha(text(), 0.65f); }

    public static int accent2() { return UI.accent2(); }
    public static int glassOpacityByte() { return (int) (Mods.HS_BGA.f() * 255); }

    /** HUD widget panel in the shared glass/neon style. {@code lit} 0..1 = glow/outline strength. */
    public static void panel(net.minecraft.client.gui.GuiGraphics g, int x, int y, int w, int h, float lit) {
        int r = Mods.HS_ROUND.i();
        if (Mods.HS_BG.on()) {
            UI.glass(g, x, y, w, h, r, Math.max(0.25f, Mods.HS_BGA.f() + 0.15f));
            UI.rrect(g, x, y, w, h, r, UI.argb((int) (Mods.HS_BGA.f() * 40), Mods.HS_BGC.rgb()));
        }
        if (Mods.HS_OUTLINE.on()) {
            UI.glow(g, x, y, w, h, r, accent() & 0xFFFFFF, lit * 0.6f);
            UI.goutline(g, x, y, w, h, r, UI.alpha(accent(), 0.55f + 0.45f * lit), UI.alpha(accent2(), 0.55f + 0.45f * lit));
        }
    }

    /** Thin gradient progress bar with a soft head glow. */
    public static void bar(net.minecraft.client.gui.GuiGraphics g, int x, int y, int w, int h, float f) {
        f = Math.max(0f, Math.min(1f, f));
        UI.rrect(g, x, y, w, h, h / 2, UI.argb(90, 0x000000));
        int fw = (int) (w * f);
        if (fw > 1) {
            UI.hgrad(g, x, y, fw, h, accent(), accent2());
            UI.rect(g, x + fw - 1, y - 1, 2, h + 2, UI.argb(120, 0xFFFFFF));
        }
    }
}
