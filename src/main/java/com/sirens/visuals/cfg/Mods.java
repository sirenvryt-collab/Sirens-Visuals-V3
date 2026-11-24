package com.sirens.visuals.cfg;

import com.sirens.visuals.Actions;
import com.sirens.visuals.cfg.Module.Category;
import com.sirens.visuals.cfg.Setting.*;

/** Every module and setting in the mod. Fields are read directly by the feature code. */
public final class Mods {
    private Mods() { }
    public static void init() { /* forces static initialisation */ }

    /** Order must match Fx.particle(). */
    public static final String[] PARTICLES = {"End Rod", "Sparkle", "Flame", "Soul Flame", "Heart", "Note", "Crit", "Enchant",
            "Glow", "Totem", "Cloud", "Snowflake", "Firework", "Happy", "Angry", "Dust"};

    static String clock(float t) {
        int mins = Math.round(((t / 1000f + 6f) % 24f) * 60f) % 1440;
        return String.format("%02d:%02d", mins / 60, mins % 60);
    }

    // =====================================================================================================
    //  SYSTEM
    // =====================================================================================================
    public static final Module MENU = new Module("menu", "Menu & Theme", Category.SYSTEM, "Look and feel of this menu and tools.", false, true);
    public static final Color M_ACCENT = MENU.color("accent", "Accent Color", 0x8B5CFF).desc("Main highlight color of the menu.");
    public static final Color M_ACCENT2 = MENU.color("accent2", "Second Accent", 0x22D3EE).desc("Second color used for gradients.");
    public static final Num M_ALPHA = MENU.num("alpha", "Panel Opacity", 0.92f, 0.5f, 1f, 0.01f).desc("How see-through the menu is.");
    public static final Bool M_ANIM = MENU.bool("anim", "Animations", true).desc("Smooth toggles and hover effects.");
    public static final Bool M_DIAG = MENU.bool("diag", "Show Hook Status", true).desc("Shows which render hooks are active in the footer.");
    public static final Setting.Action M_HUDED = MENU.action("hudeditor", "Open HUD Editor (drag & drop)", () -> Actions.openHudEditor());
    public static final Setting.Action M_HANDED = MENU.action("handeditor", "Open Hand Position Editor", () -> Actions.openHandEditor());
    public static final Setting.Action M_STUDIO = MENU.action("studio", "Open Skin & Cape Studio", () -> Actions.openStudio());
    public static final Setting.Action M_DIAGS = MENU.action("diagscreen", "Hook Diagnostics", () -> Actions.openDiag());
    public static final Setting.Action M_RESET = MENU.action("reset", "Reset EVERYTHING to defaults", () -> Config.resetEverything());

    // =====================================================================================================
    //  HUD
    // =====================================================================================================
    public static final Module HS = new Module("hudstyle", "HUD Style", Category.HUD, "Global look of every HUD widget.", false, true).kw("font color background");
    public static final Color HS_TEXT = HS.color("text", "Text Color", 0xFFFFFF);
    public static final Color HS_ACCENT = HS.color("accent", "Accent Color", 0x8B5CFF).desc("Used for bars, highlights and key presses.");
    public static final Bool HS_SHADOW = HS.bool("shadow", "Text Shadow", true);
    public static final Bool HS_BG = HS.bool("bg", "Background Panels", true);
    public static final Color HS_BGC = HS.color("bgc", "Background Color", 0x0B0B14);
    public static final Num HS_BGA = HS.num("bga", "Background Opacity", 0.45f, 0f, 1f, 0.01f);
    public static final Num HS_ROUND = HS.num("round", "Corner Radius", 5, 0, 10, 1).ints();
    public static final Num HS_PAD = HS.num("pad", "Padding", 4, 1, 10, 1).ints();
    public static final Bool HS_OUTLINE = HS.bool("outline", "Accent Outline", true);
    public static final Num HS_SCALE = HS.num("scale", "Global Scale", 1f, 0.5f, 2f, 0.05f).suffix("x");

    /** Creates a draggable HUD widget module with hidden position settings. */
    public static final class Hud {
        public final Module m; public final Num px, py, pa, scale;
        Hud(String id, String name, String desc, float x, float y, int align, boolean on) {
            m = new Module(id, name, Category.HUD, desc, true, on);
            px = m.num("px", "x", x, 0, 1, 0); px.hide();
            py = m.num("py", "y", y, 0, 1, 0); py.hide();
            pa = m.num("pa", "a", align, 0, 2, 1); pa.hide();
            scale = m.num("scale", "Scale", 1f, 0.5f, 2.5f, 0.05f).suffix("x");
        }
        public float defX() { return px.def; }
        public float defY() { return py.def; }
        public int defA() { return Math.round(pa.def); }
    }

    public static final Hud COORDS = new Hud("coords", "Coordinates", "XYZ, facing direction and biome.", 0.01f, 0.01f, 0, true);
    public static final Num COORDS_DEC = COORDS.m.num("dec", "Decimals", 1, 0, 3, 1).ints();
    public static final Bool COORDS_DIR = COORDS.m.bool("dir", "Show Direction", true);
    public static final Bool COORDS_BIOME = COORDS.m.bool("biome", "Show Biome", false);

    public static final Hud FPS = new Hud("fps", "FPS & Ping", "Frame rate and latency.", 0.01f, 0.075f, 0, true);
    public static final Bool FPS_FPS = FPS.m.bool("fps", "Show FPS", true);
    public static final Bool FPS_PING = FPS.m.bool("ping", "Show Ping", true);
    public static final Bool FPS_COLOR = FPS.m.bool("color", "Color by Performance", true);

    public static final Hud CPS = new Hud("cps", "CPS Counter", "Clicks per second.", 0.01f, 0.115f, 0, true);
    public static final Bool CPS_RMB = CPS.m.bool("rmb", "Show Right Click", true);

    public static final Hud KEYS = new Hud("keys", "Keystrokes", "WASD, space and mouse buttons.", 0.01f, 0.70f, 0, true);
    public static final Bool KEYS_MOUSE = KEYS.m.bool("mouse", "Mouse Buttons", true);
    public static final Bool KEYS_SPACE = KEYS.m.bool("space", "Space Bar", true);
    public static final Bool KEYS_CPS = KEYS.m.bool("cps", "CPS on Mouse Keys", true);
    public static final Num KEYS_FADE = KEYS.m.num("fade", "Fade Speed", 0.35f, 0.05f, 1f, 0.01f);
    public static final Color KEYS_COL = KEYS.m.color("pressed", "Pressed Color", 0x8B5CFF);

    public static final Hud ARMOR = new Hud("armor", "Armor Status", "Armor pieces with durability.", 0.01f, 0.40f, 0, true);
    public static final Choice ARMOR_MODE = ARMOR.m.choice("mode", "Durability", 0, "Percent", "Number", "Off");
    public static final Bool ARMOR_HORIZ = ARMOR.m.bool("horiz", "Horizontal Layout", false);
    public static final Bool ARMOR_HELD = ARMOR.m.bool("held", "Show Held Item", true);

    public static final Hud POTIONS = new Hud("potions", "Potion Effects", "Active effects with timers.", 0.99f, 0.01f, 2, true);
    public static final Bool POT_TIME = POTIONS.m.bool("time", "Show Timers", true);

    public static final Hud ITEMC = new Hud("itemcount", "Item Counter", "Counts an item in your inventory.", 0.01f, 0.155f, 0, true);
    public static final Choice ITEMC_ITEM = ITEMC.m.choice("item", "Item", 0, "Totem", "Golden Apple", "Ender Pearl", "End Crystal", "Arrow", "XP Bottle");
    public static final Bool ITEMC_HIDE0 = ITEMC.m.bool("hide0", "Hide When Zero", true);

    public static final Hud STATUS = new Hud("status", "Movement State", "Sprinting / Sneaking / Flying / Swimming.", 0.01f, 0.19f, 0, true);
    public static final Hud SPEED = new Hud("speed", "Speed", "Your movement speed.", 0.01f, 0.225f, 0, false);
    public static final Choice SPEED_UNIT = SPEED.m.choice("unit", "Unit", 0, "blocks/s", "km/h");

    public static final Hud CLOCK = new Hud("clock", "Clock", "Real time and in-game time.", 0.99f, 0.95f, 2, false);
    public static final Bool CLOCK_REAL = CLOCK.m.bool("real", "Real Time", true);
    public static final Bool CLOCK_GAME = CLOCK.m.bool("game", "Game Time", true);
    public static final Bool CLOCK_24 = CLOCK.m.bool("h24", "24-hour", true);

    public static final Hud PLAYING = new Hud("nowplaying", "Now Playing", "Current song from YouTube Music.", 0.5f, 0.80f, 1, true);
    public static final Bool PLAYING_BAR = PLAYING.m.bool("bar", "Progress Bar", true);

    public static final Hud COMBO = new Hud("combo", "Combo Counter", "Consecutive hits.", 0.5f, 0.56f, 1, true);

    public static final Hud LYRICS = new Hud("lyrics", "Lyrics (HUD)", "Synced lyrics as a HUD widget. The sky lyrics (Music tab) are the default.", 0.5f, 0.14f, 1, false);
    public static final Bool LYR_ALWAYS = LYRICS.m.bool("always", "Always Visible", true).desc("Off = only fade in while looking up at the sky.");
    public static final Num LYR_PITCH = LYRICS.m.num("pitch", "Look-up Angle", 10, 0, 60, 1).suffix("deg").showIf(() -> !Mods.LYR_ALWAYS.on());
    public static final Num LYR_OFFSET = LYRICS.m.num("offset", "Sync Offset", 0f, -3f, 3f, 0.05f).suffix("s");
    public static final Num LYR_LINES = LYRICS.m.num("lines", "Context Lines", 1, 0, 3, 1).ints();

    // =====================================================================================================
    //  COMBAT
    // =====================================================================================================
    public static final Module HITFX = new Module("hitfx", "Hit Ripples", Category.COMBAT, "Expanding ring of particles where you hit.", true, true).kw("particles");
    public static final Choice HIT_PART = HITFX.choice("particle", "Particle", 0, PARTICLES);
    public static final Color HIT_COL = HITFX.color("color", "Dust Color", 0x8B5CFF).showIf(() -> HITFX_dust());
    public static final Num HIT_SIZE = HITFX.num("size", "Ring Size", 1f, 0.4f, 3f, 0.05f).suffix("x");
    public static final Num HIT_RINGS = HITFX.num("rings", "Rings", 1, 1, 3, 1).ints();
    static boolean HITFX_dust() { return HIT_PART.idx == PARTICLES.length - 1; }

    public static final Module KILLFX = new Module("killfx", "Kill Effects", Category.COMBAT, "Burst when you finish off a mob or player.", true, true);
    public static final Choice KILL_STYLE = KILLFX.choice("style", "Style", 0, "Totem", "Souls", "Fireworks", "Hearts", "Sparkles", "Flames", "Snow", "Enchant");
    public static final Num KILL_INT = KILLFX.num("intensity", "Intensity", 1f, 0.3f, 4f, 0.1f).suffix("x");

    public static final Module DMG = new Module("dmgnum", "Damage Numbers", Category.COMBAT, "Floating damage numbers near the crosshair.", true, true);
    public static final Color DMG_COL = DMG.color("color", "Color", 0xFF5555);
    public static final Num DMG_SCALE = DMG.num("scale", "Size", 1f, 0.6f, 2.5f, 0.05f).suffix("x");
    public static final Num DMG_RISE = DMG.num("rise", "Rise Speed", 1f, 0.3f, 3f, 0.05f).suffix("x");

    public static final Module HITMARK = new Module("hitmark", "Hit Marker", Category.COMBAT, "Flash on the crosshair when you deal damage.", true, true);
    public static final Choice HM_STYLE = HITMARK.choice("style", "Style", 0, "X", "Plus", "Ring");
    public static final Color HM_COL = HITMARK.color("color", "Color", 0xFFFFFF);
    public static final Num HM_SIZE = HITMARK.num("size", "Size", 1f, 0.5f, 3f, 0.05f).suffix("x");

    public static final Module LOWHP = new Module("lowhp", "Low Health Vignette", Category.COMBAT, "Pulsing red edges when you are hurt.", true, true);
    public static final Num LOWHP_AT = LOWHP.num("at", "Starts Below", 6f, 2f, 12f, 1f).suffix(" HP");
    public static final Color LOWHP_COL = LOWHP.color("color", "Color", 0xFF0000);
    public static final Num LOWHP_STR = LOWHP.num("strength", "Strength", 1f, 0.2f, 2f, 0.05f).suffix("x");

    // =====================================================================================================
    //  WORLD
    // =====================================================================================================
    public static final Module FULLBRIGHT = new Module("fullbright", "Fullbright", Category.WORLD, "See clearly in the dark (client-side night vision).", true, false).kw("night vision gamma");
    public static final Module NOWEATHER = new Module("noweather", "No Weather", Category.WORLD, "Hides rain and thunder on your screen.", true, false);

    public static final Module WATER = new Module("water", "Realistic Water", Category.WORLD, "Better water color, clear vision, caustics, light shafts, glints and ripples.", true, true).kw("ocean underwater caustics");
    public static final Choice W_TINT = WATER.choice("tint", "Water Color", 1, "Vanilla", "Crystal", "Tropical", "Deep Blue", "Murky", "Custom");
    public static final Color W_CUSTOM = WATER.color("custom", "Custom Color", 0x2F8BFF).showIf(() -> W_TINT.is("Custom"));
    public static final Num W_STR = WATER.num("strength", "Color Strength", 0.55f, 0f, 1f, 0.05f).showIf(() -> !W_TINT.is("Vanilla"));
    public static final Bool W_CLEAR = WATER.bool("clear", "Clear Underwater Vision", true);
    public static final Setting.Header W_H1 = WATER.header("Underwater screen effects");
    public static final Bool W_OVER = WATER.bool("overlay", "Underwater Tint", true);
    public static final Color W_OVERC = WATER.color("overlayc", "Tint Color", 0x2A86C8).showIf(() -> W_OVER.on());
    public static final Num W_OVERS = WATER.num("overlays", "Tint Strength", 0.22f, 0.05f, 0.6f, 0.01f).showIf(() -> W_OVER.on());
    public static final Bool W_CAUS = WATER.bool("caustics", "Caustics", true).desc("Moving light patterns while submerged.");
    public static final Bool W_RAYS = WATER.bool("rays", "Light Shafts", true);
    public static final Bool W_DEPTH = WATER.bool("depth", "Depth Darkening", true);
    public static final Setting.Header W_H2 = WATER.header("Surface & swimming");
    public static final Bool W_GLINT = WATER.bool("glints", "Sun Glints", true).desc("Sparkles on the water surface in daylight.");
    public static final Num W_GLINTD = WATER.num("glintd", "Glint Density", 1f, 0.2f, 3f, 0.1f).suffix("x").showIf(() -> W_GLINT.on());
    public static final Bool W_RIPPLE = WATER.bool("ripples", "Swim Ripples", true);
    public static final Bool W_BUBBLE = WATER.bool("bubbles", "Bubble Trail", true);

    public static final Module WET = new Module("wet", "Wet Terrain", Category.WORLD, "The world gets damp in the rain: puddle ripples, drips and a cooler, darker grade.", true, true).kw("rain puddle");
    public static final Bool WET_GRADE = WET.bool("grade", "Damp Color Grade", true);
    public static final Color WET_COL = WET.color("color", "Grade Color", 0x1A2A44).showIf(() -> WET_GRADE.on());
    public static final Num WET_STR = WET.num("strength", "Grade Strength", 0.8f, 0.1f, 1f, 0.05f).showIf(() -> WET_GRADE.on());
    public static final Bool WET_PUDDLE = WET.bool("puddles", "Puddle Ripples", true);
    public static final Num WET_DENS = WET.num("density", "Ripple Density", 1f, 0.2f, 3f, 0.1f).suffix("x").showIf(() -> WET_PUDDLE.on());
    public static final Bool WET_STEPS = WET.bool("steps", "Wet Footsteps", true);
    public static final Bool WET_DRIP = WET.bool("drips", "Dripping Leaves", true);
    public static final Num WET_DRY = WET.num("dry", "Dry-off Time", 40f, 5f, 180f, 5f).suffix("s");

    public static final Module LOWFIRE = new Module("lowfire", "Low Fire", Category.WORLD, "Shrinks the burning overlay so you can still see. Needs a render hook.", true, true).kw("burning overlay");
    public static final Num LF_HEIGHT = LOWFIRE.num("height", "Height", 0.22f, 0.05f, 0.6f, 0.01f).fmt(f -> Math.round(f * 100) + "%");
    public static final Num LF_TILE = LOWFIRE.num("tile", "Flame Size", 4f, 2f, 10f, 0.5f).suffix("x");
    public static final Bool LF_GLOW = LOWFIRE.bool("glow", "Warm Glow", true);

    public static final Module GLINT = new Module("glint", "Custom Glint", Category.WORLD, "Recolor the enchantment glint on items and armor.", true, false).kw("enchant shimmer");
    public static final Choice G_STYLE = GLINT.choice("style", "Style", 0, "Classic", "Stripes", "Sparkle", "Rainbow", "Gradient");
    public static final Color G_C1 = GLINT.color("c1", "Color", 0xC050FF).showIf(() -> !G_STYLE.is("Rainbow"));
    public static final Color G_C2 = GLINT.color("c2", "Second Color", 0x40E0FF).showIf(() -> G_STYLE.is("Gradient"));
    public static final Num G_OPA = GLINT.num("opacity", "Brightness", 1f, 0.2f, 1.5f, 0.05f);
    public static final Num G_SCALE = GLINT.num("scale", "Pattern Density", 2f, 1f, 6f, 1f).ints();

    // =====================================================================================================
    //  SKY
    // =====================================================================================================
    public static final Module SKY = new Module("sky", "Custom Sky", Category.SKY, "Real sun, moon and stars: lock the time, change sizes, star brightness and sky color.", true, false).kw("sun moon stars time lock night day");
    public static final Choice S_MODE = SKY.choice("mode", "Time", 0, "Normal", "Locked", "Custom Speed");
    public static final Num S_TIME = SKY.num("time", "Time of Day", 6000f, 0f, 24000f, 100f).fmt(f -> clock(f)).showIf(() -> S_MODE.idx == 1);
    public static final Choice S_PHASE = SKY.choice("phase", "Moon Phase", 0, "Keep Current", "Full Moon", "Waning Gibbous", "Third Quarter", "Waning Crescent", "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous").showIf(() -> S_MODE.idx == 1);
    public static final Num S_SPEED = SKY.num("speed", "Day Speed", 1f, 0f, 30f, 0.25f).suffix("x").showIf(() -> S_MODE.idx == 2);
    public static final Setting.Header S_H1 = SKY.header("Sun & moon");
    public static final Setting.Info S_NOTE = SKY.info(() -> "Sun/moon size is not available in this build.");
    public static final Num S_SUN = SKY.num("sun", "Sun Size", 1f, 0.4f, 4f, 0.05f).suffix("x");
    public static final Num S_MOON = SKY.num("moon", "Moon Size", 1f, 0.4f, 4f, 0.05f).suffix("x");
    public static final Setting.Header S_HI = SKY.header("Sky image");
    public static final Bool S_IMG_ON = SKY.bool("imgon", "Show Sky Image", true).desc("Wraps your image around the whole sky like a skybox. Best with a 2:1 panorama (equirectangular). Needs Custom Sky enabled.");
    public static final Setting.Action S_IMG_BROWSE = SKY.action("imgbrowse", "Choose Sky Image...", () -> Actions.browseSky());
    public static final Choice S_IMG = SKY.choice("img", "Sky Image File", 0, "(none)").desc("Images in the config folder sirensvisuals/sky (png, jpg).");
    public static final Text S_IMGFILE = SKY.text("imgfile", "Sky image", "").hide();
    public static final Num S_IMG_OP = SKY.num("imgop", "Image Opacity", 0.9f, 0.1f, 1f, 0.05f);
    public static final Num S_IMG_YAW = SKY.num("imgyaw", "Rotation", 0f, -180f, 180f, 1f).suffix("deg").desc("Turn the panorama to choose which part faces north.");
    public static final Num S_IMG_BRIGHT = SKY.num("imgbright", "Brightness", 1f, 0.2f, 1.5f, 0.05f);
    public static final Bool S_IMG_NIGHT = SKY.bool("imgnight", "Dim at Night", true).desc("Darkens the skybox when the in-game sun is down.");
    public static final Bool S_IMG_HIDE = SKY.bool("imghide", "Hide Sun, Moon & Stars", true).desc("Off = vanilla sun, moon and stars draw on top of your image.");
    public static final Bool S_IMG_TINT = SKY.bool("imgtint", "Tint Sky From Image", true).desc("Blends the sky dome toward the average color of the image.");
    public static final Setting.Info S_IMG_STATUS = SKY.info(() -> com.sirens.visuals.fx.SkyFx.status);
    public static final Setting.Action S_IMG_FOLDER = SKY.action("imgfolder", "Copy Sky Folder Path", () -> Actions.copySkyFolder());
    public static final Setting.Header S_H2 = SKY.header("Stars");
    public static final Num S_STARS = SKY.num("stars", "Minimum Star Brightness", 0f, 0f, 1f, 0.05f).desc("Above 0 keeps stars visible during the day.");
    public static final Bool S_TWINKLE = SKY.bool("twinkle", "Twinkle", false);
    public static final Setting.Header S_H3 = SKY.header("Colors");
    public static final Bool S_TINT = SKY.bool("tint", "Tint Sky", false);
    public static final Color S_TINTC = SKY.color("tintc", "Sky Color", 0x6A5CFF).showIf(() -> S_TINT.on());
    public static final Num S_TINTS = SKY.num("tints", "Tint Strength", 0.6f, 0.05f, 1f, 0.05f).showIf(() -> S_TINT.on());
    public static final Bool S_SET = SKY.bool("sunset", "Tint Sunrise/Sunset Glow", false);
    public static final Color S_SETC = SKY.color("setc", "Glow Color", 0xFF4FA0).showIf(() -> S_SET.on());

    public static final Module AURORA = new Module("aurora", "Aurora", Category.SKY, "Northern lights that fade in as you look up.", true, true);
    public static final Choice A_COLOR = AURORA.choice("color", "Colors", 0, "Green", "Purple", "Cyan", "Rainbow");
    public static final Num A_STR = AURORA.num("strength", "Strength", 0.6f, 0.1f, 1f, 0.05f);
    public static final Num A_SPEED = AURORA.num("speed", "Speed", 1f, 0.1f, 4f, 0.05f).suffix("x");
    public static final Bool A_NIGHT = AURORA.bool("night", "Only at Night", false);

    // =====================================================================================================
    //  PLAYER
    // =====================================================================================================
    public static final Module SKIN = new Module("skin", "Skin Changer", Category.PLAYER, "Wear any skin client-side: from a file, a username or a URL.", true, false).kw("texture model slim");
    public static final Setting.Action SK_BROWSE = SKIN.action("browse", "Choose Skin Image...", () -> Actions.browseSkin());
    public static final Setting.Info SK_DROP = SKIN.info(() -> "Tip: you can also drag a .png onto this screen. " + com.sirens.visuals.fx.Skins.status);
    public static final Choice SK_SRC = SKIN.choice("source", "Source", 1, "File", "Username", "URL");
    public static final Choice SK_FILE = SKIN.choice("file", "Skin File", 0, "(none)").showIf(() -> SK_SRC.idx == 0);
    public static final Text SK_NAME = SKIN.text("name", "Username", "").hint("Minecraft username").showIf(() -> SK_SRC.idx == 1);
    public static final Text SK_URL = SKIN.text("url", "Image URL", "").hint("https://...png").showIf(() -> SK_SRC.idx == 2);
    public static final Choice SK_MODEL = SKIN.choice("model", "Arm Model", 0, "Auto", "Wide (Steve)", "Slim (Alex)");
    public static final Setting.Action SK_APPLY = SKIN.action("apply", "Apply Skin", () -> Actions.applySkin());
    public static final Setting.Action SK_FOLDER = SKIN.action("folder", "Copy Skins Folder Path", () -> Actions.copySkinFolder());

    public static final Module CAPE = new Module("cape", "Custom Cape", Category.PLAYER, "Wear a cape (and matching elytra) of your own design or file.", true, false);
    public static final Setting.Action C_BROWSE = CAPE.action("browse", "Choose Cape Image...", () -> Actions.browseCape());
    public static final Setting.Info C_DROP = CAPE.info(() -> "Tip: drag a .png onto this screen, or use a pattern. " + com.sirens.visuals.fx.Skins.status);
    public static final Choice C_SRC = CAPE.choice("source", "Source", 0, "Pattern", "File", "Username's Cape", "URL");
    public static final Choice C_PAT = CAPE.choice("pattern", "Pattern", 6, "Solid", "Vertical Stripes", "Horizontal Stripes", "Gradient", "Diagonal", "Checker", "Galaxy", "Flames", "Waves").showIf(() -> C_SRC.idx == 0);
    public static final Color C_C1 = CAPE.color("c1", "Color 1", 0x6A2CFF).showIf(() -> C_SRC.idx == 0);
    public static final Color C_C2 = CAPE.color("c2", "Color 2", 0x22D3EE).showIf(() -> C_SRC.idx == 0);
    public static final Choice C_FILE = CAPE.choice("file", "Cape File", 0, "(none)").showIf(() -> C_SRC.idx == 1);
    public static final Text C_NAME = CAPE.text("name", "Username", "").hint("Minecraft username").showIf(() -> C_SRC.idx == 2);
    public static final Text C_URL = CAPE.text("url", "Image URL", "").hint("https://...png").showIf(() -> C_SRC.idx == 3);
    public static final Bool C_ELYTRA = CAPE.bool("elytra", "Use on Elytra", true);
    public static final Setting.Action C_APPLY = CAPE.action("apply", "Apply Cape", () -> Actions.applyCape());
    public static final Setting.Action C_FOLDER = CAPE.action("folder", "Copy Capes Folder Path", () -> Actions.copyCapeFolder());

    public static final Module COSM = new Module("cosmetics", "Cosmetics", Category.PLAYER, "Particle cosmetics: halo, wings, aura, orbit, crown and trails.", true, true).kw("halo wings aura");
    public static final Choice CO_TYPE = COSM.choice("type", "Cosmetic", 1, "None", "Halo", "Wings", "Aura", "Orbit", "Crown", "Trail", "Cape Sparkle");
    public static final Color CO_C1 = COSM.color("c1", "Color", 0xFFD84A);
    public static final Color CO_C2 = COSM.color("c2", "Second Color", 0xFF5CE1);
    public static final Bool CO_RAINBOW = COSM.bool("rainbow", "Rainbow", false);
    public static final Num CO_SIZE = COSM.num("size", "Size", 1f, 0.5f, 2.5f, 0.05f).suffix("x");
    public static final Num CO_DENS = COSM.num("density", "Density", 1f, 0.3f, 3f, 0.1f).suffix("x");
    public static final Bool CO_FIRST = COSM.bool("first", "Show in First Person", true);

    public static final Module HANDS = new Module("hands", "Hand Position", Category.PLAYER, "Move, rotate and scale your first-person hands and held items.", true, false).kw("viewmodel item offset");
    public static final Choice H_WHICH = HANDS.choice("which", "Apply To", 0, "Both Hands", "Main Hand", "Off Hand");
    public static final Num H_X = HANDS.num("x", "Left / Right", 0f, -1f, 1f, 0.01f);
    public static final Num H_Y = HANDS.num("y", "Down / Up", 0f, -1f, 1f, 0.01f);
    public static final Num H_Z = HANDS.num("z", "Back / Forward", 0f, -1f, 1f, 0.01f);
    public static final Num H_RX = HANDS.num("rx", "Pitch", 0f, -90f, 90f, 1f).suffix("deg");
    public static final Num H_RY = HANDS.num("ry", "Yaw", 0f, -90f, 90f, 1f).suffix("deg");
    public static final Num H_RZ = HANDS.num("rz", "Roll", 0f, -90f, 90f, 1f).suffix("deg");
    public static final Num H_SCALE = HANDS.num("scale", "Scale", 1f, 0.3f, 1.8f, 0.01f).suffix("x");
    public static final Bool H_MIRROR = HANDS.bool("mirror", "Mirror Off Hand", true).desc("Flips X and roll for the left hand so one setup fits both.");
    public static final Setting.Action H_OPEN = HANDS.action("open", "Open Hand Editor", () -> Actions.openHandEditor());

    public static final Module SWING = new Module("swing", "Slow Swing", Category.PLAYER, "Slows the visual arm swing animation (no gameplay effect).", true, false);
    public static final Num SW_MUL = SWING.num("mul", "Slowness", 1.8f, 1f, 4f, 0.1f).suffix("x");

    public static final Module STEPS = new Module("footsteps", "Footsteps", Category.PLAYER, "Glowing footprints as you walk.", true, true);
    public static final Choice ST_PART = STEPS.choice("particle", "Particle", 15, PARTICLES);
    public static final Color ST_COL = STEPS.color("color", "Dust Color", 0x8FD3FF).showIf(() -> ST_PART.idx == PARTICLES.length - 1);
    public static final Num ST_GAP = STEPS.num("gap", "Step Spacing", 0.9f, 0.4f, 2.5f, 0.05f);
    public static final Num ST_SIZE = STEPS.num("size", "Dust Size", 0.6f, 0.3f, 2f, 0.05f);

    public static final Module EMOTES = new Module("emotes", "Emotes", Category.PLAYER, "Press the emote key for a wheel of particle emotes.", true, true);
    public static final Num EM_LEN = EMOTES.num("len", "Duration", 3f, 1f, 8f, 0.5f).suffix("s");

    // =====================================================================================================
    //  CAMERA
    // =====================================================================================================
    public static final Module FREELOOK = new Module("freelook", "Freelook", Category.CAMERA, "Hold the key and look around without turning your character.", true, true);
    public static final Choice FL_MODE = FREELOOK.choice("mode", "Mode", 0, "Hold", "Toggle");
    public static final Num FL_SENS = FREELOOK.num("sens", "Sensitivity", 1f, 0.2f, 2f, 0.05f).suffix("x");
    public static final Bool FL_THIRD = FREELOOK.bool("third", "Switch to Third Person", false);
    public static final Bool FL_RESET = FREELOOK.bool("reset", "Snap Back on Release", true);

    public static final Module CINE = new Module("cinematic", "Cinematic Camera", Category.CAMERA, "Silky smooth, eased camera movement. Toggle with its key.", true, false);
    public static final Num CI_SMOOTH = CINE.num("smooth", "Smoothing", 0.6f, 0.1f, 0.95f, 0.01f);
    public static final Bool CI_HUD = CINE.bool("hidehud", "Hide HUD While Active", false);

    public static final Module ZOOM = new Module("zoom", "Zoom", Category.CAMERA, "Hold the zoom key. Scroll to change the amount.", true, true);
    public static final Num Z_AMT = ZOOM.num("amount", "Zoom Level", 4f, 1.5f, 20f, 0.25f).suffix("x");
    public static final Num Z_SMOOTH = ZOOM.num("smooth", "Smoothness", 0.5f, 0.05f, 0.95f, 0.01f);
    public static final Bool Z_SCROLL = ZOOM.bool("scroll", "Scroll to Adjust", true);
    public static final Bool Z_CINE = ZOOM.bool("cine", "Cinematic While Zooming", true);

    public static final Module THIRD = new Module("thirdperson", "Third Person", Category.CAMERA, "Camera distance and clipping.", true, false);
    public static final Num TP_DIST = THIRD.num("dist", "Distance", 4f, 1f, 16f, 0.25f);
    public static final Bool TP_NOCLIP = THIRD.bool("noclip", "Camera Passes Through Blocks", true);

    // =====================================================================================================
    //  MUSIC
    // =====================================================================================================
    public static final Module MUSIC = new Module("music", "YouTube Music Bridge", Category.MUSIC, "Shows what you are listening to, synced lyrics and ad muting. Needs the browser userscript.", true, true).kw("lyrics ads spotify");
    public static final Bool MU_MUTE = MUSIC.bool("muteads", "Mute Ads", true);
    public static final Bool MU_LYR = MUSIC.bool("lyrics", "Fetch Lyrics", true);
    public static final Bool MU_SKY = MUSIC.bool("skylyrics", "Lyrics in the Sky", true).desc("Large glowing lyric line above you; look up to see it.");
    public static final Num MU_SKY_SIZE = MUSIC.num("skysize", "Sky Lyric Size", 2.2f, 1f, 4f, 0.1f).suffix("x").showIf(() -> MU_SKY.on());
    public static final Num MU_SKY_PITCH = MUSIC.num("skypitch", "Visible From Look-up Angle", 0f, 0f, 60f, 1f).suffix("deg").showIf(() -> MU_SKY.on()).desc("0 = always visible, higher = only when looking further up.");
    public static final Bool MU_SKY_NEXT = MUSIC.bool("skynext", "Preview Next Line", true).showIf(() -> MU_SKY.on());
    public static final Text MU_TOKEN = MUSIC.text("token", "Token", "").hide();
    public static final Setting.Info MU_STATUS = MUSIC.info(() -> com.sirens.visuals.MusicBridge.status());
    public static final Setting.Action MU_COPY = MUSIC.action("copyurl", "Copy Userscript Install URL", () -> Actions.copyScriptUrl());
    public static final Setting.Info MU_HELP = MUSIC.info(() -> "Paste the URL into your browser with Tampermonkey installed, then open music.youtube.com.");
    static { S_H1.hide(); S_SUN.hide(); S_MOON.hide(); S_SET.hide(); S_SETC.hide(); }
}
