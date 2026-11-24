package com.sirens.visuals.cfg;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** JSON persistence for every module/setting: { "moduleId": { "enabled": true, "settingId": value } } */
public final class Config {
    private static volatile boolean dirty;
    private static long lastSave;

    private Config() { }

    public static Path dir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("sirensvisuals");
        try { Files.createDirectories(p); } catch (Exception ignored) { }
        return p;
    }

    static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("sirensvisuals.json"); }

    public static void touch() { dirty = true; }

    public static void load() {
        Mods.init();
        try {
            Path p = file();
            if (!Files.exists(p)) { save(); return; }
            JsonObject root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            for (Module m : Module.ALL) {
                if (!root.has(m.id) || !root.get(m.id).isJsonObject()) continue;
                JsonObject o = root.getAsJsonObject(m.id);
                if (m.enabled != null && o.has("enabled")) m.enabled.load(o.get("enabled"));
                for (Setting s : m.settings) if (s.persistent() && o.has(s.id)) s.load(o.get(s.id));
            }
        } catch (Exception ex) {
            System.err.println("[Sirens Visuals] could not read config, using defaults: " + ex);
        }
        dirty = false;
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : Module.ALL) {
                JsonObject o = new JsonObject();
                if (m.enabled != null) o.add("enabled", m.enabled.save());
                for (Setting s : m.settings) if (s.persistent()) o.add(s.id, s.save());
                root.add(m.id, o);
            }
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ex) {
            System.err.println("[Sirens Visuals] could not save config: " + ex);
        }
        dirty = false;
        lastSave = System.currentTimeMillis();
    }

    /** Called every client tick: flushes changes at most every 2 seconds. */
    public static void tick() {
        if (dirty && System.currentTimeMillis() - lastSave > 2000) save();
    }

    public static void resetEverything() {
        for (Module m : Module.ALL) m.resetAll();
        save();
    }
}
