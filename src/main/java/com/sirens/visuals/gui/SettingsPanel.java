package com.sirens.visuals.gui;

import com.sirens.visuals.cfg.Module;
import com.sirens.visuals.cfg.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Generic renderer/editor for a module's settings. Used by the menu and the studio screens. */
public class SettingsPanel {
    public Module mod;
    public int x, y, w, h;
    public float scroll, scrollT;
    static final int RH = 24;

    private Setting drag, editing, colorOpen;
    private int pickMode; // 1 = sv box, 2 = hue bar
    private float hue, sat = 1, val = 1;
    private final Map<Setting, Float> anim = new HashMap<>();
    private final List<Object[]> hits = new ArrayList<>(); // {Setting, x, y, w, h}
    private int contentH;

    public void setModule(Module m) { if (m != mod) { mod = m; scroll = scrollT = 0; editing = null; colorOpen = null; } }

    private List<Setting> visible() {
        List<Setting> l = new ArrayList<>();
        if (mod == null) return l;
        for (Setting s : mod.settings) if (!s.hidden && s.visible.getAsBoolean()) l.add(s);
        return l;
    }

    public void render(GuiGraphics g, int mx, int my) {
        hits.clear();
        if (mod == null) return;
        scroll += (scrollT - scroll) * Math.min(1f, UI.dt() * 18f);
        g.enableScissor(x, y, x + w, y + h);
        int cy = y - Math.round(scroll);
        int top = cy;
        List<Setting> list = visible();
        if (mod.enabled != null) { cy = row(g, mod.enabled, cy, mx, my); }
        for (Setting s : list) cy = row(g, s, cy, mx, my);
        contentH = cy - top;
        g.disableScissor();
        if (contentH > h) {
            int bh = Math.max(20, h * h / contentH), by = y + (int) ((h - bh) * (scroll / (contentH - h)));
            UI.rrect(g, x + w - 3, by, 3, bh, 1, UI.alpha(UI.accent(), 0.7f));
        }
    }

    private int row(GuiGraphics g, Setting s, int cy, int mx, int my) {
        int rh = RH;
        if (s instanceof Setting.Header) {
            UI.text(g, s.label.toUpperCase(), x + 4, cy + 10, UI.alpha(UI.accent(), 0.9f));
            UI.rect(g, x + 4, cy + 21, w - 12, 1, UI.argb(50, 0xFFFFFF));
            return cy + 28;
        }
        if (s instanceof Setting.Info i) {
            UI.text(g, UI.fit(i.text.get(), w - 14), x + 4, cy + 6, 0xFF9AA3B5);
            return cy + 18;
        }
        int rx = x, rw = w - 8;
        boolean hov = UI.in(mx, my, rx, cy, rw, rh - 2) && my >= y && my < y + h;
        UI.rrect(g, rx, cy, rw, rh - 2, 5, hov ? 0x3CFFFFFF : 0x1EFFFFFF);
        hits.add(new Object[]{s, rx, cy, rw, rh - 2});
        String lab = UI.fit(s.label, rw / 2 - 6);
        UI.text(g, lab, rx + 8, cy + 8, 0xFFE8ECF5);
        int cx = rx + rw - 8;
        if (s instanceof Setting.Bool b) {
            float a = UI.ease(anim.getOrDefault(s, b.v ? 1f : 0f), b.v ? 1f : 0f, 16f); anim.put(s, a);
            int tw = 30, th = 12, tx = cx - tw, ty = cy + 5;
            if (a > 0.05f) UI.glow(g, tx, ty, tw, th, 6, UI.accent() & 0xFFFFFF, a * 0.7f);
            UI.rrect(g, tx, ty, tw, th, 6, UI.mix(0xFF3A4054, UI.accentAt(a), a));
            UI.rrect(g, tx + 2 + (int) (a * (tw - th)), ty + 2, th - 4, th - 4, 4, 0xFFFFFFFF);
        } else if (s instanceof Setting.Num n) {
            int sw = Math.min(110, rw / 2 - 40), sx = cx - sw - 44, sy = cy + 10;
            UI.rrect(g, sx, sy, sw, 4, 2, 0xFF3A4054);
            UI.glow(g, sx, sy, Math.max(2, (int) (sw * n.frac())), 4, 2, UI.accent() & 0xFFFFFF, 0.5f);
            UI.hgrad(g, sx, sy, Math.max(2, (int) (sw * n.frac())), 4, UI.accent(), UI.accent2());
            UI.rrect(g, sx + (int) (sw * n.frac()) - 3, sy - 3, 6, 10, 3, 0xFFFFFFFF);
            UI.rtext(g, n.text(), cx, cy + 8, 0xFFB8C0D4);
            hits.set(hits.size() - 1, new Object[]{s, sx, cy, sw, rh - 2});
        } else if (s instanceof Setting.Color c) {
            UI.rrect(g, cx - 28, cy + 4, 28, 14, 4, 0xFF000000 | c.rgb);
            UI.outline(g, cx - 28, cy + 4, 28, 14, 0x80FFFFFF);
            if (colorOpen == s) {
                int px = rx + 8, py = cy + rh, pw = rw - 16, ph = 70;
                UI.rrect(g, rx, cy + rh - 2, rw, ph + 10, 4, 0x33000000);
                int hx = px + pw - 14;
                for (int i = 0; i < pw - 24; i += 2)
                    for (int j = 0; j < ph; j += 2)
                        g.fill(px + i, py + j, px + i + 2, py + j + 2, 0xFF000000 | Mth.hsvToRgb(hue, (i / (float) (pw - 24)), 1f - j / (float) ph));
                for (int j = 0; j < ph; j += 2) g.fill(hx, py + j, hx + 10, py + j + 2, 0xFF000000 | Mth.hsvToRgb(j / (float) ph, 1, 1));
                int mxp = px + (int) (sat * (pw - 24)), myp = py + (int) ((1 - val) * ph);
                UI.outline(g, mxp - 3, myp - 3, 6, 6, 0xFFFFFFFF);
                UI.rect(g, hx - 1, py + (int) (hue * ph), 12, 2, 0xFFFFFFFF);
                rh += ph + 10;
                hits.add(new Object[]{"sv", px, py, pw - 24, ph, s});
                hits.add(new Object[]{"hue", hx, py, 10, ph, s});
            }
        } else if (s instanceof Setting.Choice c) {
            String v = (editing == null ? "< " : "< ") + UI.fit(c.value(), rw / 2 - 30) + " >";
            UI.rtext(g, v, cx, cy + 8, UI.accent());
        } else if (s instanceof Setting.Text t) {
            String v = t.get().isEmpty() && editing != s ? t.hint : t.get() + (editing == s && (System.currentTimeMillis() / 400 % 2 == 0) ? "_" : "");
            UI.rtext(g, UI.fit(v, rw / 2 - 12), cx, cy + 8, editing == s ? 0xFFFFFFFF : 0xFFB8C0D4);
        } else if (s instanceof Setting.Action) {
            UI.rrect(g, rx, cy, rw, rh - 2, 5, UI.alpha(UI.accent(), hov ? 0.5f : 0.3f));
            UI.goutline(g, rx, cy, rw, rh - 2, 5, UI.alpha(UI.accent(), hov ? 0.95f : 0.55f), UI.alpha(UI.accent2(), hov ? 0.95f : 0.55f));
            UI.ctext(g, s.label, rx + rw / 2, cy + 8, 0xFFFFFFFF);
        }
        if (s.desc != null && !s.desc.isEmpty() && hov && !(s instanceof Setting.Action)) tip = s.desc;
        return cy + rh;
    }

    public String tip = "";

    public boolean click(double mx, double my, int btn) {
        if (mod == null || !UI.in(mx, my, x, y, w, h)) return false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Object[] o = hits.get(i);
            int hx = (Integer) o[1], hy = (Integer) o[2], hw = (Integer) o[3], hh = (Integer) o[4];
            if (!UI.in(mx, my, hx, hy, hw, hh)) continue;
            if (o[0] instanceof String k) {
                Setting.Color c = (Setting.Color) o[5];
                pickMode = k.equals("sv") ? 1 : 2; drag = c; applyPick(mx, my, hx, hy, hw, hh); return true;
            }
            Setting s = (Setting) o[0];
            editing = null;
            if (s instanceof Setting.Bool b) b.toggle();
            else if (s instanceof Setting.Num n) { drag = n; n.setFrac((float) ((mx - hx) / hw)); }
            else if (s instanceof Setting.Choice c) c.cycle(btn == 1 ? -1 : 1);
            else if (s instanceof Setting.Text) editing = s;
            else if (s instanceof Setting.Action a) a.run.run();
            else if (s instanceof Setting.Color c) {
                if (colorOpen == s) colorOpen = null;
                else { colorOpen = s; float[] hsv = rgb2hsv(c.rgb); hue = hsv[0]; sat = hsv[1]; val = hsv[2]; }
            }
            return true;
        }
        return true;
    }

    private void applyPick(double mx, double my, int hx, int hy, int hw, int hh) {
        if (!(drag instanceof Setting.Color c)) return;
        if (pickMode == 1) { sat = Mth.clamp((float) ((mx - hx) / hw), 0, 1); val = 1f - Mth.clamp((float) ((my - hy) / hh), 0, 1); }
        else hue = Mth.clamp((float) ((my - hy) / hh), 0, 0.999f);
        c.set(Mth.hsvToRgb(hue, sat, val));
    }

    public boolean drag(double mx, double my) {
        if (drag instanceof Setting.Num n) {
            for (Object[] o : hits) if (o[0] == n) { n.setFrac((float) ((mx - (Integer) o[1]) / (Integer) o[3])); return true; }
        } else if (drag instanceof Setting.Color) {
            for (Object[] o : hits) if (o[0] instanceof String k && ((k.equals("sv") && pickMode == 1) || (k.equals("hue") && pickMode == 2)))
                { applyPick(mx, my, (Integer) o[1], (Integer) o[2], (Integer) o[3], (Integer) o[4]); return true; }
        }
        return false;
    }

    public void release() { drag = null; }

    public boolean scroll(double mx, double my, double dy) {
        if (!UI.in(mx, my, x, y, w, h)) return false;
        scrollT = Mth.clamp(scrollT - (float) dy * 30f, 0, Math.max(0, contentH - h));
        return true;
    }

    public boolean typing() { return editing != null; }

    public boolean key(int key) {
        if (editing instanceof Setting.Text t) {
            if (key == 257 || key == 335 || key == 256) { editing = null; return true; }
            if (key == 259 && !t.get().isEmpty()) { t.set(t.get().substring(0, t.get().length() - 1)); return true; }
            return true;
        }
        return false;
    }

    public boolean chr(int cp) {
        if (editing instanceof Setting.Text t && cp >= 32 && t.get().length() < t.maxLen) { t.set(t.get() + new String(Character.toChars(cp))); return true; }
        return false;
    }

    static float[] rgb2hsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b)), d = mx - mn, hh = 0;
        if (d > 0) {
            if (mx == r) hh = ((g - b) / d) % 6; else if (mx == g) hh = (b - r) / d + 2; else hh = (r - g) / d + 4;
            hh /= 6; if (hh < 0) hh += 1;
        }
        return new float[]{hh, mx == 0 ? 0 : d / mx, mx};
    }
}
