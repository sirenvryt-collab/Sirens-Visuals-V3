package com.sirens.visuals.gui;

import com.sirens.visuals.Hooks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DiagScreen extends Screen {
    public DiagScreen() { super(Component.literal("Hook Diagnostics")); }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        UI.beginFrame();
        g.fill(0, 0, width, height, 0x88000000);
        int w = Math.min(420, width - 20), x = (width - w) / 2, y = 24;
        UI.rrect(g, x, y, w, 30 + (Hooks.INFO.size() + Hooks.ERRORS.size()) * 16 + 40, 8, 0xE00D1020);
        UI.hgrad(g, x, y, w, 3, UI.accent(), UI.accent2());
        UI.text(g, "Hook Diagnostics", x + 10, y + 10, 0xFFFFFFFF);
        int cy = y + 28;
        for (Hooks.Info i : Hooks.INFO.values()) {
            boolean ok = Hooks.seen(i.id());
            UI.rrect(g, x + 10, cy + 2, 8, 8, 4, ok ? 0xFF4ADE80 : 0xFF6B7288);
            UI.text(g, i.label(), x + 24, cy + 2, 0xFFE8ECF5);
            UI.rtext(g, ok ? "running (" + Hooks.count(i.id()) + ")" : "idle: " + i.trigger(), x + w - 10, cy + 2, ok ? 0xFF4ADE80 : 0xFF8791A8);
            cy += 16;
        }
        for (var e : Hooks.ERRORS.entrySet()) {
            UI.text(g, UI.fit("ERROR " + e.getKey() + ": " + e.getValue(), w - 20), x + 10, cy + 4, 0xFFFF7A7A);
            cy += 16;
        }
        UI.text(g, "Idle = not triggered yet OR the hook did not apply. Trigger it, then check again.", x + 10, cy + 6, 0xFF8791A8);
    }
}
