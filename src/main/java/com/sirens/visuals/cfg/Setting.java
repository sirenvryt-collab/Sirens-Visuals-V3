package com.sirens.visuals.cfg;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/** A single configurable value. Subclasses are plain data holders; the GUI renders them generically. */
public abstract class Setting {
    public final String id, label;
    public String desc = "";
    public boolean hidden = false;
    public Module module;
    public BooleanSupplier visible = () -> true;

    protected Setting(String id, String label) { this.id = id; this.label = label; }

    @SuppressWarnings("unchecked")
    public <T extends Setting> T desc(String d) { this.desc = d; return (T) this; }
    @SuppressWarnings("unchecked")
    public <T extends Setting> T hide() { this.hidden = true; return (T) this; }
    @SuppressWarnings("unchecked")
    public <T extends Setting> T showIf(BooleanSupplier s) { this.visible = s; return (T) this; }

    public abstract JsonElement save();
    public abstract void load(JsonElement e);
    public abstract void reset();
    /** Whether this setting stores a value that should be persisted. */
    public boolean persistent() { return true; }

    // ------------------------------------------------------------------ Bool
    public static class Bool extends Setting {
        public boolean v; final boolean def;
        public Bool(String id, String label, boolean def) { super(id, label); this.v = def; this.def = def; }
        public boolean on() { return v; }
        public void set(boolean b) { if (v != b) { v = b; Config.touch(); } }
        public void toggle() { set(!v); }
        @Override public JsonElement save() { return new JsonPrimitive(v); }
        @Override public void load(JsonElement e) { try { v = e.getAsBoolean(); } catch (Exception ex) { v = def; } }
        @Override public void reset() { set(def); }
    }

    // ------------------------------------------------------------------- Num
    public static class Num extends Setting {
        public float v; public final float def, min, max, step;
        public Function<Float, String> fmt = f -> String.format("%.2f", f);
        public Num(String id, String label, float def, float min, float max, float step) {
            super(id, label); this.v = def; this.def = def; this.min = min; this.max = max; this.step = step;
        }
        public Num fmt(Function<Float, String> f) { this.fmt = f; return this; }
        public Num ints() { this.fmt = f -> String.valueOf(Math.round(f)); return this; }
        public Num suffix(String s) { this.fmt = f -> (Math.abs(f - Math.round(f)) < 0.005f ? String.valueOf(Math.round(f)) : String.format("%.2f", f)) + s; return this; }
        public float f() { return v; }
        public int i() { return Math.round(v); }
        public void set(float x) {
            if (step > 0) x = Math.round((x - min) / step) * step + min;
            x = Math.max(min, Math.min(max, x));
            if (x != v) { v = x; Config.touch(); }
        }
        public float frac() { return max <= min ? 0 : (v - min) / (max - min); }
        public void setFrac(float t) { set(min + (max - min) * Math.max(0, Math.min(1, t))); }
        @Override public JsonElement save() { return new JsonPrimitive(v); }
        @Override public void load(JsonElement e) { try { v = Math.max(min, Math.min(max, e.getAsFloat())); } catch (Exception ex) { v = def; } }
        @Override public void reset() { set(def); }
        public String text() { return fmt.apply(v); }
    }

    // ----------------------------------------------------------------- Color
    public static class Color extends Setting {
        public int rgb; final int def;
        public Color(String id, String label, int def) { super(id, label); this.rgb = def & 0xFFFFFF; this.def = def & 0xFFFFFF; }
        public int rgb() { return rgb; }
        public int argb(int a) { return (Math.max(0, Math.min(255, a)) << 24) | rgb; }
        public void set(int c) { c &= 0xFFFFFF; if (c != rgb) { rgb = c; Config.touch(); } }
        @Override public JsonElement save() { return new JsonPrimitive(String.format("#%06X", rgb)); }
        @Override public void load(JsonElement e) {
            try { String s = e.getAsString(); if (s.startsWith("#")) s = s.substring(1); rgb = Integer.parseInt(s, 16) & 0xFFFFFF; }
            catch (Exception ex) { rgb = def; }
        }
        @Override public void reset() { set(def); }
    }

    // ---------------------------------------------------------------- Choice
    public static class Choice extends Setting {
        public String[] opts; public int idx; final int def;
        public Choice(String id, String label, int def, String... opts) { super(id, label); this.opts = opts; this.idx = def; this.def = def; }
        public int idx() { return idx; }
        public String value() { return opts.length == 0 ? "" : opts[Math.max(0, Math.min(opts.length - 1, idx))]; }
        public boolean is(String s) { return value().equalsIgnoreCase(s); }
        public void set(int i) {
            if (opts.length == 0) return;
            i = ((i % opts.length) + opts.length) % opts.length;
            if (i != idx) { idx = i; Config.touch(); }
        }
        public void cycle(int d) { set(idx + d); }
        public void setOpts(String[] o) { this.opts = o; if (idx >= o.length) idx = Math.max(0, o.length - 1); }
        @Override public JsonElement save() { return new JsonPrimitive(value()); }
        @Override public void load(JsonElement e) {
            try { String s = e.getAsString(); for (int i = 0; i < opts.length; i++) if (opts[i].equalsIgnoreCase(s)) { idx = i; return; } }
            catch (Exception ex) { }
            idx = Math.min(def, Math.max(0, opts.length - 1));
        }
        @Override public void reset() { set(def); }
    }

    // ------------------------------------------------------------------ Text
    public static class Text extends Setting {
        public String v; final String def; public int maxLen = 256; public String hint = "";
        public Text(String id, String label, String def) { super(id, label); this.v = def; this.def = def; }
        public Text hint(String h) { this.hint = h; return this; }
        public String get() { return v; }
        public void set(String s) { if (s == null) s = ""; if (!s.equals(v)) { v = s; Config.touch(); } }
        @Override public JsonElement save() { return new JsonPrimitive(v); }
        @Override public void load(JsonElement e) { try { v = e.getAsString(); } catch (Exception ex) { v = def; } }
        @Override public void reset() { set(def); }
    }

    // ---------------------------------------------------------------- Action
    public static class Action extends Setting {
        public final Runnable run;
        public Action(String id, String label, Runnable run) { super(id, label); this.run = run; }
        @Override public JsonElement save() { return JsonNull.INSTANCE; }
        @Override public void load(JsonElement e) { }
        @Override public void reset() { }
        @Override public boolean persistent() { return false; }
    }

    // ---------------------------------------------------------------- Header
    public static class Header extends Setting {
        public Header(String id, String label) { super(id, label); }
        @Override public JsonElement save() { return JsonNull.INSTANCE; }
        @Override public void load(JsonElement e) { }
        @Override public void reset() { }
        @Override public boolean persistent() { return false; }
    }

    // ------------------------------------------------------------------ Info
    /** Read-only line whose text is computed each frame (status displays). */
    public static class Info extends Setting {
        public final Supplier<String> text;
        public Info(String id, Supplier<String> text) { super(id, ""); this.text = text; }
        @Override public JsonElement save() { return JsonNull.INSTANCE; }
        @Override public void load(JsonElement e) { }
        @Override public void reset() { }
        @Override public boolean persistent() { return false; }
    }
}
