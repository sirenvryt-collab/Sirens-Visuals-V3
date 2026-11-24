package com.sirens.visuals.gui;

import com.sirens.visuals.cfg.Config;
import com.sirens.visuals.hud.Canvas;
import com.sirens.visuals.hud.Hud;
import com.sirens.visuals.hud.HudElem;
import com.sirens.visuals.hud.HudElems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
    private HudElem sel, drag;
    private double gx, gy;

    public HudEditorScreen() { super(Component.literal("HUD Editor")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() { Hud.suppress = true; }
    @Override public void removed() { Hud.suppress = false; Config.save(); }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        UI.beginFrame();
        g.fill(0, 0, width, height, 0x66000000);
        for (int x = 0; x < width; x += 20) g.fill(x, 0, x + 1, height, 0x10FFFFFF);
        for (int y = 0; y < height; y += 20) g.fill(0, y, width, y + 1, 0x10FFFFFF);
        for (HudElem e : HudElems.ALL) {
            Canvas c = e.build(minecraft, true);
            if (c == null || c.empty()) continue;
            Hud.draw(g, minecraft, e, c);
            int col = e == sel ? UI.accent() : e.enabled() ? UI.alpha(UI.accent(), 0.5f) : 0x55FF6060;
            UI.outline(g, e.bx - 1, e.by - 1, e.bw + 2, e.bh + 2, col);
            if (!e.enabled()) UI.text(g, "off", e.bx + 2, e.by + e.bh + 2, 0xFFFF8080);
            if (e == sel) UI.text(g, e.name(), e.bx, e.by - 11, 0xFFFFFFFF);
        }
        UI.ctext(g, "Drag widgets  |  Right-click: toggle  |  Scroll: scale  |  R: reset selected  |  Esc: done", width / 2, height - 14, 0xFFC8D0E4);
    }

    private HudElem at(double mx, double my) {
        for (int i = HudElems.ALL.size() - 1; i >= 0; i--) {
            HudElem e = HudElems.ALL.get(i);
            if (e.bw > 0 && UI.in(mx, my, e.bx, e.by, e.bw, e.bh)) return e;
        }
        return null;
    }

    @Override public boolean mouseClicked(MouseButtonEvent ev, boolean dbl) {
        HudElem e = at(ev.x(), ev.y());
        sel = e;
        if (e == null) return true;
        if (ev.button() == 1) { if (e.h.m.enabled != null) e.h.m.enabled.toggle(); return true; }
        drag = e; gx = ev.x() - e.bx; gy = ev.y() - e.by;
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent ev, double dx, double dy) {
        if (drag == null) return false;
        Hud.place(drag, minecraft, (float) (ev.x() - gx), (float) (ev.y() - gy), drag.bw, drag.bh);
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent ev) { drag = null; return true; }

    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        HudElem e = at(mx, my);
        if (e == null) e = sel;
        if (e != null) e.h.scale.set(e.h.scale.f() + (float) sy * 0.05f);
        return true;
    }

    @Override public boolean keyPressed(KeyEvent e) {
        if (e.key() == 82 && sel != null) { Hud.resetPos(sel); return true; }
        return super.keyPressed(e);
    }
}
