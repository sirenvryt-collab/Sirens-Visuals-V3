package com.sirens.visuals.gui;

import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.fx.Emotes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Radial emote wheel. Click a slice, or press 1-9 / 0 / - for the first twelve emotes. */
public class EmoteScreen extends Screen {
    private final float[] lit = new float[Emotes.NAMES.length];
    private float open;

    public EmoteScreen() { super(Component.literal("Emotes")); }
    @Override public boolean isPauseScreen() { return false; }

    private static final int R_IN = 38, R_OUT = 92;

    /** Slice under the mouse, or -1. Slice 0 starts at the top and runs clockwise. */
    private int hover(double mx, double my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0, d = Math.sqrt(dx * dx + dy * dy);
        if (d < R_IN * open || d > R_OUT * open + 14) return -1;
        double a = Math.atan2(dx, -dy);
        if (a < 0) a += Math.PI * 2;
        int n = Emotes.NAMES.length;
        return (int) (a / (Math.PI * 2 / n) + 0.5) % n;
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        UI.beginFrame();
        open = UI.ease(open, 1f, 14f);
        g.fill(0, 0, width, height, UI.argb((int) (90 * open), 0x02040C));
        int cx = width / 2, cy = height / 2, n = Emotes.NAMES.length, hv = hover(mx, my);
        UI.glow(g, cx - (int) (R_OUT * open), cy - (int) (R_OUT * open), (int) (R_OUT * 2 * open), (int) (R_OUT * 2 * open), (int) (R_OUT * open), UI.accent() & 0xFFFFFF, 0.6f * open);
        for (int i = 0; i < n; i++) {
            lit[i] = UI.ease(lit[i], i == hv ? 1f : 0f, 16f);
            double a = i * Math.PI * 2 / n;
            float rr = (R_IN + R_OUT) / 2f * open + lit[i] * 5;
            int bx = cx + (int) Math.round(Math.sin(a) * rr), by = cy - (int) Math.round(Math.cos(a) * rr);
            int w = 62, h = 24, x = bx - w / 2, y = by - h / 2;
            float k = lit[i];
            UI.glow(g, x, y, w, h, 7, UI.mix(UI.accent(), UI.accent2(), i / (float) n) & 0xFFFFFF, k);
            UI.glass(g, x, y, w, h, 7, 0.55f + 0.35f * k);
            int c1 = UI.mix(UI.accent(), UI.accent2(), i / (float) n), c2 = UI.mix(UI.accent(), UI.accent2(), (i + 1f) / n);
            UI.goutline(g, x, y, w, h, 7, UI.alpha(c1, 0.35f + 0.65f * k), UI.alpha(c2, 0.35f + 0.65f * k));
            UI.ctext(g, Emotes.NAMES[i], bx, by - 4, UI.mix(0xFFB8C0D8, 0xFFFFFFFF, k));
        }
        UI.glowCtext(g, hv >= 0 ? Emotes.NAMES[hv].toUpperCase() : "EMOTES", cx, cy - 4, 0xFFFFFFFF, UI.accent() & 0xFFFFFF, 1f);
    }

    private void pick(int i) {
        if (i < 0 || i >= Emotes.NAMES.length) return;
        Emotes.play(i, Mods.EM_LEN.f());
        onClose();
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean d) {
        pick(hover(e.x(), e.y()));
        return true;
    }

    @Override public boolean keyPressed(KeyEvent e) {
        int k = e.key(); // GLFW: '1'..'9' = 49..57, '0' = 48, '-' = 45
        if (k >= 49 && k <= 57) { pick(k - 49); return true; }
        if (k == 48) { pick(9); return true; }
        if (k == 45) { pick(10); return true; }
        return super.keyPressed(e);
    }
}
