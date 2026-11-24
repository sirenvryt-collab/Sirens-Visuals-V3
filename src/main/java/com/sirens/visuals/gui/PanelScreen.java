package com.sirens.visuals.gui;

import com.sirens.visuals.cfg.Config;
import com.sirens.visuals.cfg.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Screen showing one or more module panels side by side (hand editor, studio). */
public class PanelScreen extends Screen {
    private final Module[] mods;
    private final SettingsPanel[] panels;
    private final boolean dim;
    private final String hint;

    public PanelScreen(String title, boolean dim, String hint, Module... mods) {
        super(Component.literal(title));
        this.mods = mods; this.dim = dim; this.hint = hint;
        panels = new SettingsPanel[mods.length];
        for (int i = 0; i < mods.length; i++) { panels[i] = new SettingsPanel(); panels[i].setModule(mods[i]); }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        UI.beginFrame();
        if (dim) g.fill(0, 0, width, height, 0x99050810);
        int pw = Math.min(250, (width - 30) / mods.length), total = pw * mods.length + (mods.length - 1) * 8;
        int x0 = dim ? (width - total) / 2 : width - total - 10, y0 = 24, ph = height - 60;
        for (int i = 0; i < mods.length; i++) {
            int x = x0 + i * (pw + 8);
            UI.card(g, x, y0, pw, ph, 10, 0.86f, 0.55f + 0.25f * UI.pulse(1.2f));
            UI.glowText(g, mods[i].name, x + 10, y0 + 10, 0xFFFFFFFF, UI.accent() & 0xFFFFFF, 1f);
            panels[i].x = x + 8; panels[i].y = y0 + 26; panels[i].w = pw - 16; panels[i].h = ph - 34;
            panels[i].render(g, mx, my);
        }
        UI.ctext(g, hint, width / 2, height - 22, 0xFFC8D0E4);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean d) { for (SettingsPanel p : panels) if (p.click(e.x(), e.y(), e.button())) return true; return true; }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { for (SettingsPanel p : panels) if (p.drag(e.x(), e.y())) return true; return false; }
    @Override public boolean mouseReleased(MouseButtonEvent e) { for (SettingsPanel p : panels) p.release(); return true; }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { for (SettingsPanel p : panels) if (p.scroll(mx, my, sy)) return true; return true; }
    @Override public boolean keyPressed(KeyEvent e) {
        for (SettingsPanel p : panels) if (p.typing()) return p.key(e.key());
        return super.keyPressed(e);
    }
    @Override public boolean charTyped(CharacterEvent e) { for (SettingsPanel p : panels) if (p.chr(e.codepoint())) return true; return false; }
    @Override public void removed() { Config.save(); }

    public void onFilesDrop(java.util.List<java.nio.file.Path> paths) {
        com.sirens.visuals.Actions.dropped(paths, lastX > width / 2 && mods.length > 1 ? 2 : 1);
    }
    private double lastX;
    public void mouseMoved(double x, double y) { lastX = x; }
}
