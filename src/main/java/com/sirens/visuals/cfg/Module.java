package com.sirens.visuals.cfg;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public enum Category {
        HUD("HUD"), COMBAT("Combat"), WORLD("World"), SKY("Sky"), PLAYER("Player"), CAMERA("Camera"), MUSIC("Music"), SYSTEM("System");
        public final String label;
        Category(String l) { this.label = l; }
    }

    public static final List<Module> ALL = new ArrayList<>();

    public final String id, name, desc;
    public final Category cat;
    /** null when the module has no on/off switch (pure settings group). */
    public final Setting.Bool enabled;
    public final List<Setting> settings = new ArrayList<>();
    /** Extra words matched by the search box. */
    public String keywords = "";

    public Module(String id, String name, Category cat, String desc, boolean toggleable, boolean defOn) {
        this.id = id; this.name = name; this.cat = cat; this.desc = desc;
        this.enabled = toggleable ? new Setting.Bool("enabled", "Enabled", defOn) : null;
        if (enabled != null) enabled.module = this;
        ALL.add(this);
    }

    public Module kw(String k) { this.keywords = k; return this; }
    public boolean on() { return enabled == null || enabled.v; }

    private <T extends Setting> T add(T s) { s.module = this; settings.add(s); return s; }

    public Setting.Bool bool(String id, String label, boolean def) { return add(new Setting.Bool(id, label, def)); }
    public Setting.Num num(String id, String label, float def, float min, float max, float step) { return add(new Setting.Num(id, label, def, min, max, step)); }
    public Setting.Color color(String id, String label, int def) { return add(new Setting.Color(id, label, def)); }
    public Setting.Choice choice(String id, String label, int def, String... opts) { return add(new Setting.Choice(id, label, def, opts)); }
    public Setting.Text text(String id, String label, String def) { return add(new Setting.Text(id, label, def)); }
    public Setting.Action action(String id, String label, Runnable r) { return add(new Setting.Action(id, label, r)); }
    public Setting.Header header(String label) { return add(new Setting.Header("h_" + settings.size(), label)); }
    public Setting.Info info(java.util.function.Supplier<String> s) { return add(new Setting.Info("i_" + settings.size(), s)); }

    public void resetAll() {
        if (enabled != null) enabled.reset();
        for (Setting s : settings) s.reset();
    }

    public boolean matches(String q) {
        q = q.toLowerCase();
        if (name.toLowerCase().contains(q) || desc.toLowerCase().contains(q) || keywords.toLowerCase().contains(q)) return true;
        for (Setting s : settings) if (!s.hidden && s.label.toLowerCase().contains(q)) return true;
        return false;
    }
}
