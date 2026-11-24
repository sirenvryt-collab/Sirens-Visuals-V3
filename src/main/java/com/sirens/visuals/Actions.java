package com.sirens.visuals;

import com.sirens.visuals.cfg.Config;
import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.fx.Skins;
import com.sirens.visuals.gui.DiagScreen;
import com.sirens.visuals.gui.HudEditorScreen;
import com.sirens.visuals.gui.PanelScreen;
import net.minecraft.client.Minecraft;

public final class Actions {
    private Actions() { }

    private static void show(net.minecraft.client.gui.screens.Screen s) { Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(s)); }

    public static void openHudEditor() { show(new HudEditorScreen()); }
    public static void openHandEditor() {
        Mods.HANDS.enabled.set(true);
        show(new PanelScreen("Hand Position", false, "Adjust the sliders and look at your hand. Esc to finish.", Mods.HANDS));
    }
    public static void openStudio() { Skins.scan(); show(new PanelScreen("Skin & Cape Studio", true, "Skins and capes are client-side: only you see them.", Mods.SKIN, Mods.CAPE)); }
    public static void openDiag() { show(new DiagScreen()); }

    public static void applySkin() { Skins.applySkin(); }
    public static void applyCape() { Skins.applyCape(); }
    public static void copySkinFolder() { copy(Skins.skinsDir().toAbsolutePath().toString()); }
    public static void copyCapeFolder() { copy(Skins.capesDir().toAbsolutePath().toString()); }
    public static void copyScriptUrl() { copy(MusicBridge.scriptUrl()); }

    public static void browseSkin() { FilePicker.pick("Choose a skin image (.png)", Skins::useSkinFile); }
    public static void browseSky() { FilePicker.pick("Choose a sky image (.png/.jpg)", com.sirens.visuals.fx.SkyFx::useFile); }
    public static void copySkyFolder() { copy(com.sirens.visuals.fx.SkyFx.dir().toAbsolutePath().toString()); }
    public static void browseCape() { FilePicker.pick("Choose a cape image (.png)", Skins::useCapeFile); }

    /** Files dropped on a screen. kind: 0 = by module, 1 = skin, 2 = cape. */
    public static void dropped(java.util.List<java.nio.file.Path> paths, int kind) {
        for (java.nio.file.Path p : paths) {
            if (!p.toString().toLowerCase().endsWith(".png")) continue;
            if (kind == 2) Skins.useCapeFile(p); else Skins.useSkinFile(p);
            return;
        }
    }

    private static void copy(String s) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.keyboardHandler.setClipboard(s));
    }
}
