package com.sirens.visuals;

import com.mojang.blaze3d.platform.InputConstants;
import com.sirens.visuals.cfg.Config;
import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.fx.CameraFx;
import com.sirens.visuals.fx.Glint;
import com.sirens.visuals.fx.Skins;
import com.sirens.visuals.fx.Visuals;
import com.sirens.visuals.gui.EmoteScreen;
import com.sirens.visuals.gui.MenuScreen;
import com.sirens.visuals.hud.Hud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

public class SirensVisuals implements ClientModInitializer {
    static KeyMapping menu, zoom, emote, bright, freelook, cine, mPlay, mNext, mPrev;

    private static KeyMapping key(String id, int code, KeyMapping.Category cat) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key.sirensvisuals." + id, InputConstants.Type.KEYSYM, code, cat));
    }

    @Override public void onInitializeClient() {
        try { Config.load(); } catch (Throwable t) { System.err.println("[Sirens Visuals] config: " + t); }
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("sirensvisuals", "main"));
        int none = InputConstants.UNKNOWN.getValue();
        menu = key("menu", 344, cat);
        zoom = key("zoom", 67, cat);
        emote = key("emote", 86, cat);
        bright = key("fullbright", 66, cat);
        freelook = key("freelook", 342, cat);
        cine = key("cinematic", 79, cat);
        mPlay = key("music_play", none, cat);
        mNext = key("music_next", none, cat);
        mPrev = key("music_prev", none, cat);

        HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath("sirensvisuals", "under"), (g, dt) -> {
            Minecraft mc = Minecraft.getInstance();
            try {
                Hud.under(g, mc);
                Visuals.pollClicks(mc);
                CameraFx.zoomHeld = zoom.isDown();
                CameraFx.freelookKey = freelook.isDown();
                CameraFx.cineKey = cine.isDown();
                CameraFx.frame(mc, Hud.dt);
            } catch (Throwable t) { err("under", t); }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("sirensvisuals", "over"), (g, dt) -> {
            try { Hud.over(g, Minecraft.getInstance()); } catch (Throwable t) { err("over", t); }
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            try { tick(mc); } catch (Throwable t) { err("tick", t); }
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide()) { try { Visuals.onAttack(entity); } catch (Throwable t) { err("attack", t); } }
            return InteractionResult.PASS;
        });

        try { MusicBridge.start(); } catch (Throwable t) { err("music", t); }
        Skins.scan();
    }

    static long lastErr;
    static void err(String w, Throwable t) {
        if (System.currentTimeMillis() - lastErr > 5000) { lastErr = System.currentTimeMillis(); System.err.println("[Sirens Visuals] error in " + w + ": " + t); t.printStackTrace(); }
    }

    static void tick(Minecraft mc) {
        Config.tick();
        Visuals.tick(mc);
        Glint.tick(mc);
        while (menu.consumeClick()) if (mc.screen == null) mc.setScreen(new MenuScreen());
        while (emote.consumeClick()) if (mc.screen == null && mc.player != null) mc.setScreen(new EmoteScreen());
        while (bright.consumeClick()) { Mods.FULLBRIGHT.enabled.toggle(); }
        while (freelook.consumeClick()) if (Mods.FREELOOK.on() && Mods.FL_MODE.idx() == 1) CameraFx.setFreelook(mc, !CameraFx.freelook);
        while (cine.consumeClick()) { }
        while (zoom.consumeClick()) { }
        while (mPlay.consumeClick()) MusicBridge.cmds.add(MusicBridge.playing ? "pause" : "play");
        while (mNext.consumeClick()) MusicBridge.cmds.add("next");
        while (mPrev.consumeClick()) MusicBridge.cmds.add("prev");
    }
}
