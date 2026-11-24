package com.sirens.visuals.hud;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;

/** A draggable HUD widget. */
public abstract class HudElem {
    public final Mods.Hud h;
    /** Last drawn screen-space bounds (used by the HUD editor for hit testing). */
    public int bx, by, bw, bh;

    protected HudElem(Mods.Hud h) { this.h = h; }

    public String name() { return h.m.name; }
    public boolean enabled() { return h.m.on(); }

    /** Returns null when there is nothing to show. {@code preview} = true inside the HUD editor (show sample data). */
    public abstract Canvas build(Minecraft mc, boolean preview);
}
