package com.sirens.visuals.hud;

import com.sirens.visuals.gui.UI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** A tiny recorded draw list so a widget can be measured first and then drawn at any position / scale. */
public final class Canvas {
    private static final class Op {
        int type, x, y, w, h, c; float gk; String s; ItemStack st; boolean ctr, shadow = true;
    }

    private final List<Op> ops = new ArrayList<>();
    public int w, h;
    public boolean noPanel;

    public boolean empty() { return ops.isEmpty(); }
    private void grow(int x2, int y2) { if (x2 > w) w = x2; if (y2 > h) h = y2; }

    /** color 0 = default HUD text color. */
    public Canvas text(String s, int x, int y, int c) {
        Op o = new Op(); o.type = 0; o.x = x; o.y = y; o.s = s; o.c = c; ops.add(o);
        grow(x + UI.tw(s), y + 9);
        return this;
    }
    /** Text centered on the canvas width. */
    public Canvas ctext(String s, int y, int c) {
        Op o = new Op(); o.type = 0; o.y = y; o.s = s; o.c = c; o.ctr = true; ops.add(o);
        grow(UI.tw(s), y + 9);
        return this;
    }
    /** Text centered on a given x. */
    public Canvas textAt(String s, int cx, int y, int c) { return text(s, cx - UI.tw(s) / 2, y, c); }
    public Canvas rect(int x, int y, int w, int h, int c) {
        Op o = new Op(); o.type = 1; o.x = x; o.y = y; o.w = w; o.h = h; o.c = c; ops.add(o);
        grow(x + w, y + h);
        return this;
    }
    public Canvas rrect(int x, int y, int w, int h, int c) {
        Op o = new Op(); o.type = 2; o.x = x; o.y = y; o.w = w; o.h = h; o.c = c; ops.add(o);
        grow(x + w, y + h);
        return this;
    }
    /** Horizontally centered rect. */
    public Canvas crect(int w, int y, int h, int c) {
        Op o = new Op(); o.type = 1; o.y = y; o.w = w; o.h = h; o.c = c; o.ctr = true; ops.add(o);
        grow(w, y + h);
        return this;
    }
    /** Rounded rect with a soft neon glow behind it (e.g. pressed keys). */
    public Canvas glowRect(int x, int y, int w, int h, int c, int glowRgb, float k) {
        Op o = new Op(); o.type = 4; o.x = x; o.y = y; o.w = w; o.h = h; o.c = c; o.s = String.valueOf(glowRgb); o.shadow = k > 0; o.gk = k; ops.add(o);
        grow(x + w, y + h);
        return this;
    }
    /** Horizontal accent -> accent2 gradient bar; {@code ctr} centres it on the canvas width. */
    public Canvas gradBar(int x, int y, int w, int h, boolean ctr) {
        Op o = new Op(); o.type = 5; o.x = x; o.y = y; o.w = w; o.h = h; o.ctr = ctr; ops.add(o);
        grow(x + w, y + h);
        return this;
    }
    public Canvas item(ItemStack st, int x, int y) {
        Op o = new Op(); o.type = 3; o.x = x; o.y = y; o.st = st; ops.add(o);
        grow(x + 16, y + 16);
        return this;
    }

    public void draw(GuiGraphics g, int ox, int oy, int cw) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        for (Op o : ops) {
            switch (o.type) {
                case 0 -> {
                    int col = o.c == 0 ? Style.text() : o.c;
                    int x = o.ctr ? ox + (cw - UI.tw(o.s)) / 2 : ox + o.x;
                    g.drawString(mc.font, o.s, x, oy + o.y, col, Style.shadow());
                }
                case 1 -> g.fill(o.ctr ? ox + (cw - o.w) / 2 : ox + o.x, oy + o.y, (o.ctr ? ox + (cw - o.w) / 2 : ox + o.x) + o.w, oy + o.y + o.h, o.c);
                case 2 -> UI.rrect(g, ox + o.x, oy + o.y, o.w, o.h, Math.min(4, o.h / 3), o.c);
                case 4 -> {
                    int r = Math.min(4, o.h / 3);
                    if (o.gk > 0.02f) UI.glow(g, ox + o.x, oy + o.y, o.w, o.h, r, Integer.parseInt(o.s) & 0xFFFFFF, o.gk);
                    UI.rrect(g, ox + o.x, oy + o.y, o.w, o.h, r, o.c);
                    UI.goutline(g, ox + o.x, oy + o.y, o.w, o.h, r, UI.alpha(UI.accent(), 0.25f + 0.6f * o.gk), UI.alpha(UI.accent2(), 0.25f + 0.6f * o.gk));
                }
                case 5 -> {
                    int bx = o.ctr ? ox + (cw - o.w) / 2 : ox + o.x;
                    UI.rrect(g, bx, oy + o.y, o.w, o.h, o.h / 2, 0x55000000);
                    UI.hgrad(g, bx, oy + o.y, Math.max(1, o.w), o.h, UI.alpha(UI.accent(), 0.95f), UI.alpha(UI.accent2(), 0.95f));
                }
                case 3 -> {
                    g.renderItem(o.st, ox + o.x, oy + o.y);
                    g.renderItemDecorations(mc.font, o.st, ox + o.x, oy + o.y);
                }
                default -> { }
            }
        }
    }
}
