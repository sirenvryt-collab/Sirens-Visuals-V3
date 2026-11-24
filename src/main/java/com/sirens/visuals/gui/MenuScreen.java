package com.sirens.visuals.gui;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.cfg.Config;
import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.cfg.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MenuScreen extends Screen {
    private static Module.Category cat = Module.Category.HUD;
    private static Module sel;
    private final SettingsPanel panel = new SettingsPanel();
    private String search = "";
    private boolean searching;
    private float open, listScroll, listScrollT;
    private int px, py, pw, ph, sbW = 96, lstW = 150;
    private final List<int[]> catHits = new ArrayList<>();

    public MenuScreen() { super(Component.literal("Sirens Visuals")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override protected void init() {
        pw = Math.min(width - 20, 560); ph = Math.min(height - 20, 330);
        px = (width - pw) / 2; py = (height - ph) / 2;
        if (sel == null) sel = firstOf(cat);
    }

    private Module firstOf(Module.Category c) { for (Module m : Module.ALL) if (m.cat == c) return m; return null; }

    private List<Module> modules() {
        List<Module> l = new ArrayList<>();
        for (Module m : Module.ALL) {
            if (!search.isEmpty()) { if (m.matches(search)) l.add(m); }
            else if (m.cat == cat && (m != Mods.MENU || true)) l.add(m);
        }
        return l;
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        UI.beginFrame();
        open = UI.ease(open, 1f, 14f);
        float a = Math.min(1f, open);
        g.fill(0, 0, width, height, UI.argb((int) (150 * a), 0x05060C));
        int off = (int) ((1 - a) * 18);
        int acc = UI.accent(), acc2 = UI.accent2();
        int pa = (int) (Mods.M_ALPHA.f() * 255);
        int y0 = py + off;
        UI.card(g, px, y0, pw, ph, 10, Math.min(0.88f, pa / 255f), 0.55f + 0.25f * UI.pulse(1.2f));
        UI.hgrad(g, px + 10, y0 + 1, pw - 20, 1, UI.alpha(acc, 0.9f), UI.alpha(acc2, 0.9f));
        // header
        UI.glowText(g, "SIRENS", px + 12, y0 + 10, acc, acc & 0xFFFFFF, 1f);
        UI.text(g, "VISUALS", px + 12 + UI.tw("SIRENS ") , y0 + 10, 0xFFFFFFFF);
        // search
        int sx = px + pw - 150, sy = y0 + 6;
        UI.rrect(g, sx, sy, 138, 16, 6, searching ? UI.alpha(acc, 0.25f) : 0x24FFFFFF);
        if (searching) UI.goutline(g, sx, sy, 138, 16, 6, UI.alpha(acc, 0.9f), UI.alpha(acc2, 0.9f));
        UI.text(g, searching ? search + (System.currentTimeMillis() / 400 % 2 == 0 ? "_" : "") : (search.isEmpty() ? "Search..." : search), sx + 6, sy + 4, search.isEmpty() && !searching ? 0xFF7A8299 : 0xFFFFFFFF);
        int top = y0 + 28, bodyH = ph - 28 - 16;
        // sidebar
        catHits.clear();
        int cy = top;
        for (Module.Category c : Module.Category.values()) {
            boolean s = c == cat && search.isEmpty();
            boolean hv = UI.in(mx, my, px + 6, cy, sbW - 8, 20);
            if (s) { UI.glow(g, px + 6, cy, sbW - 8, 20, 6, acc & 0xFFFFFF, 0.5f); UI.rrect(g, px + 6, cy, sbW - 8, 20, 6, UI.alpha(acc, 0.28f)); UI.goutline(g, px + 6, cy, sbW - 8, 20, 6, UI.alpha(acc, 0.8f), UI.alpha(acc2, 0.8f)); }
            else if (hv) UI.rrect(g, px + 6, cy, sbW - 8, 20, 6, 0x22FFFFFF);
            UI.text(g, c.label, px + 16, cy + 6, s ? 0xFFFFFFFF : 0xFFA9B2C7);
            catHits.add(new int[]{px + 6, cy, sbW - 8, 20, c.ordinal()});
            cy += 23;
        }
        // module list
        int lx = px + sbW + 4;
        UI.vgrad(g, lx - 2, top, 1, bodyH, UI.alpha(acc, 0.5f), UI.alpha(acc2, 0.1f));
        List<Module> mods = modules();
        g.enableScissor(lx, top, lx + lstW, top + bodyH);
        int my0 = top - (int) listScroll;
        listScroll += (listScrollT - listScroll) * Math.min(1f, UI.dt() * 18f);
        for (Module m : mods) {
            boolean s = m == sel, hv = UI.in(mx, my, lx, my0, lstW - 4, 24);
            if (s) { UI.rrect(g, lx, my0, lstW - 4, 24, 6, UI.alpha(acc, 0.24f)); UI.goutline(g, lx, my0, lstW - 4, 24, 6, UI.alpha(acc, 0.7f), UI.alpha(acc2, 0.7f)); }
            else if (hv) UI.rrect(g, lx, my0, lstW - 4, 24, 6, 0x1CFFFFFF);
            UI.text(g, UI.fit(m.name, lstW - 34), lx + 8, my0 + 8, 0xFFE8ECF5);
            if (m.enabled != null) {
                if (m.enabled.v) UI.glow(g, lx + lstW - 18, my0 + 8, 8, 8, 4, acc2 & 0xFFFFFF, 0.9f);
                UI.rrect(g, lx + lstW - 18, my0 + 8, 8, 8, 4, m.enabled.v ? acc2 : 0xFF3A4258);
            }
            my0 += 26;
        }
        g.disableScissor();
        // settings
        int sx2 = lx + lstW + 6;
        panel.x = sx2; panel.y = top + 22; panel.w = px + pw - sx2 - 8; panel.h = bodyH - 22;
        panel.setModule(sel);
        if (sel != null) {
            UI.text(g, sel.name, sx2, top + 2, 0xFFFFFFFF);
            UI.text(g, UI.fit(sel.desc, panel.w), sx2, top + 13, 0xFF8791A8);
            panel.tip = "";
            panel.render(g, mx, my);
            UI.rrect(g, px + pw - 62, top - 1, 54, 14, 6, 0x25FFFFFF);
            UI.goutline(g, px + pw - 62, top - 1, 54, 14, 6, 0x40FF8A8A, 0x40FF8A8A);
            UI.ctext(g, "Reset", px + pw - 35, top + 2, 0xFFFF8A8A);
        }
        // footer
        UI.text(g, "Hooks active: " + Hooks.active() + "/" + Hooks.INFO.size() + "   |   RShift closes   |   Esc closes", px + 10, y0 + ph - 12, 0xFF6F7890);
        if (!panel.tip.isEmpty()) UI.rtext(g, UI.fit(panel.tip, pw - 40), px + pw - 10, y0 + ph - 12, 0xFFB8C0D4);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        double mx = e.x(), my = e.y(); int b = e.button();
        searching = UI.in(mx, my, px + pw - 150, py + 6, 138, 16);
        if (searching) return true;
        for (int[] h : catHits) if (UI.in(mx, my, h[0], h[1], h[2], h[3])) {
            cat = Module.Category.values()[h[4]]; search = ""; sel = firstOf(cat); listScroll = listScrollT = 0; return true;
        }
        int top = py + 28, lx = px + sbW + 4;
        if (UI.in(mx, my, lx, top, lstW, ph - 44)) {
            int idx = (int) ((my - top + listScroll) / 26);
            List<Module> mods = modules();
            if (idx >= 0 && idx < mods.size()) {
                Module m = mods.get(idx);
                if (b == 1 && m.enabled != null) m.enabled.toggle();
                else if (mx > lx + lstW - 24 && m.enabled != null) m.enabled.toggle();
                else sel = m;
            }
            return true;
        }
        if (sel != null && UI.in(mx, my, px + pw - 62, top - 1, 54, 14)) { sel.resetAll(); return true; }
        panel.click(mx, my, b);
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { return panel.drag(e.x(), e.y()) || super.mouseDragged(e, dx, dy); }
    @Override public boolean mouseReleased(MouseButtonEvent e) { panel.release(); return true; }

    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (panel.scroll(mx, my, sy)) return true;
        int total = modules().size() * 26, vis = ph - 44;
        listScrollT = Math.max(0, Math.min(Math.max(0, total - vis), listScrollT - (float) sy * 26));
        return true;
    }

    @Override public boolean keyPressed(KeyEvent e) {
        int k = e.key();
        if (searching) {
            if (k == 256 || k == 257) { searching = false; return true; }
            if (k == 259 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); sel = modules().isEmpty() ? sel : modules().get(0); }
            return true;
        }
        if (panel.typing()) return panel.key(k);
        if (k == 344 || k == 345 || k == 256) { onClose(); return true; }
        return super.keyPressed(e);
    }

    @Override public boolean charTyped(CharacterEvent e) {
        int cp = e.codepoint();
        if (searching) { if (cp >= 32) { search += new String(Character.toChars(cp)); List<Module> m = modules(); if (!m.isEmpty()) sel = m.get(0); } return true; }
        return panel.chr(cp);
    }

    @Override public void onClose() { Config.save(); super.onClose(); }

    public void onFilesDrop(java.util.List<java.nio.file.Path> paths) {
        com.sirens.visuals.Actions.dropped(paths, sel == com.sirens.visuals.cfg.Mods.CAPE ? 2 : 1);
    }
}
