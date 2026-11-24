package com.sirens.visuals.hud;

import com.sirens.visuals.MusicBridge;
import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.fx.Visuals;
import com.sirens.visuals.gui.UI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** All draggable HUD widgets. */
public final class HudElems {
    private HudElems() { }

    public static final List<HudElem> ALL = new ArrayList<>();

    static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    static String pretty(String id) {
        int c = id.indexOf(':');
        if (c >= 0) id = id.substring(c + 1);
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) { if (sb.length() > 0) sb.append(' '); sb.append(cap(p)); }
        return sb.toString();
    }
    static int perf(float v, float good, float ok) { return v >= good ? 0xFF55FF55 : v >= ok ? 0xFFFFFF55 : 0xFFFF5555; }

    static {
        // ---------------------------------------------------------------- coordinates
        ALL.add(new HudElem(Mods.COORDS) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                LocalPlayer p = mc.player; if (p == null) return null;
                Canvas c = new Canvas();
                String f = "%." + Mods.COORDS_DEC.i() + "f";
                String xyz = "XYZ  " + String.format(f + "  " + f + "  " + f, p.getX(), p.getY(), p.getZ());
                c.text(xyz, 0, 0, 0);
                int y = 11;
                if (Mods.COORDS_DIR.on()) {
                    c.text("Facing " + cap(p.getDirection().getName()) + "  (" + Math.round(Mth.wrapDegrees(p.getYRot())) + ")", 0, y, Style.dim());
                    y += 11;
                }
                if (Mods.COORDS_BIOME.on() && mc.level != null) {
                    var b = mc.level.getBiome(p.blockPosition());
                    c.text("Biome " + pretty(b.getRegisteredName()), 0, y, Style.dim());
                }
                return c;
            }
        });
        // ---------------------------------------------------------------- fps / ping
        ALL.add(new HudElem(Mods.FPS) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                LocalPlayer p = mc.player; if (p == null) return null;
                Canvas c = new Canvas(); int x = 0;
                if (Mods.FPS_FPS.on()) {
                    String s = Hud.fps + " FPS";
                    c.text(s, 0, 0, Mods.FPS_COLOR.on() ? perf(Hud.fps, 60, 30) : 0);
                    x = UI.tw(s) + 8;
                }
                if (Mods.FPS_PING.on()) {
                    var cn = mc.getConnection();
                    if (cn != null) {
                        var pi = cn.getPlayerInfo(p.getUUID());
                        if (pi != null) {
                            int ms = pi.getLatency();
                            c.text(ms + " ms", x, 0, Mods.FPS_COLOR.on() ? perf(-ms, -80, -160) : 0);
                        }
                    }
                }
                return c.empty() ? (pv ? new Canvas().text("-", 0, 0, 0) : null) : c;
            }
        });
        // ---------------------------------------------------------------- cps
        ALL.add(new HudElem(Mods.CPS) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                Canvas c = new Canvas();
                String s = "CPS " + Visuals.cps(false) + (Mods.CPS_RMB.on() ? " | " + Visuals.cps(true) : "");
                return c.text(s, 0, 0, 0);
            }
        });
        // ---------------------------------------------------------------- keystrokes
        final float[] kl = new float[8];
        ALL.add(new HudElem(Mods.KEYS) {
            void key(Canvas c, int i, boolean down, int x, int y, int w, int hh, String label, Minecraft mc) {
                kl[i] = UI.ease(kl[i], down ? 1f : 0f, 8f + 40f * Mods.KEYS_FADE.f());
                int col = UI.mix(UI.argb(Math.max(70, (int) (Mods.HS_BGA.f() * 255)), Mods.HS_BGC.rgb()), 0xE0000000 | Mods.KEYS_COL.rgb(), kl[i]);
                if (kl[i] > 0.02f) c.glowRect(x, y, w, hh, col, Mods.KEYS_COL.rgb(), kl[i]);
                else c.glowRect(x, y, w, hh, col, 0, 0f);
                if (label != null) c.textAt(label, x + w / 2, y + (hh - 8) / 2, kl[i] > 0.5f ? 0xFF101010 : 0);
            }
            @Override public Canvas build(Minecraft mc, boolean pv) {
                var o = mc.options;
                Canvas c = new Canvas(); c.noPanel = true;
                key(c, 0, o.keyUp.isDown(), 22, 0, 20, 20, "W", mc);
                key(c, 1, o.keyLeft.isDown(), 0, 22, 20, 20, "A", mc);
                key(c, 2, o.keyDown.isDown(), 22, 22, 20, 20, "S", mc);
                key(c, 3, o.keyRight.isDown(), 44, 22, 20, 20, "D", mc);
                int y = 44;
                if (Mods.KEYS_SPACE.on()) { key(c, 4, o.keyJump.isDown(), 0, y, 64, 12, null, mc); c.rect(20, y + 5, 24, 2, kl[4] > 0.5f ? 0xFF101010 : 0xAAFFFFFF); y += 14; }
                if (Mods.KEYS_MOUSE.on()) {
                    boolean cps = Mods.KEYS_CPS.on();
                    key(c, 5, o.keyAttack.isDown(), 0, y, 31, cps ? 22 : 16, "LMB", mc);
                    key(c, 6, o.keyUse.isDown(), 33, y, 31, cps ? 22 : 16, "RMB", mc);
                    if (cps) {
                        c.textAt(String.valueOf(Visuals.cps(false)), 15, y + 12, kl[5] > 0.5f ? 0xFF101010 : Style.dim());
                        c.textAt(String.valueOf(Visuals.cps(true)), 48, y + 12, kl[6] > 0.5f ? 0xFF101010 : Style.dim());
                    }
                }
                return c;
            }
        });
        // ---------------------------------------------------------------- armor
        ALL.add(new HudElem(Mods.ARMOR) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                LocalPlayer p = mc.player; if (p == null) return null;
                List<ItemStack> st = new ArrayList<>();
                EquipmentSlot[] sl = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                if (Mods.ARMOR_HELD.on()) st.add(p.getMainHandItem());
                for (EquipmentSlot s : sl) st.add(p.getItemBySlot(s));
                Canvas c = new Canvas(); boolean horiz = Mods.ARMOR_HORIZ.on();
                int i = 0, mode = Mods.ARMOR_MODE.idx();
                for (ItemStack s : st) {
                    if (s.isEmpty()) continue;
                    int x = horiz ? i * 20 : 0, y = horiz ? 0 : i * 18;
                    c.item(s, x, y);
                    if (mode != 2 && s.isDamageableItem()) {
                        int left = s.getMaxDamage() - s.getDamageValue();
                        int pct = left * 100 / Math.max(1, s.getMaxDamage());
                        String t = mode == 0 ? pct + "%" : String.valueOf(left);
                        int col = pct > 50 ? 0xFF55FF55 : pct > 20 ? 0xFFFFFF55 : 0xFFFF5555;
                        if (horiz) c.textAt(t, x + 8, 18, col); else c.text(t, 20, y + 4, col);
                    }
                    i++;
                }
                if (c.empty()) return pv ? new Canvas().text("(no armor)", 0, 0, Style.dim()) : null;
                return c;
            }
        });
        // ---------------------------------------------------------------- potions
        ALL.add(new HudElem(Mods.POTIONS) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                LocalPlayer p = mc.player; if (p == null) return null;
                Canvas c = new Canvas(); int y = 0;
                for (MobEffectInstance e : p.getActiveEffects()) {
                    if (!e.isVisible()) continue;
                    String s = e.getEffect().value().getDisplayName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
                    if (Mods.POT_TIME.on()) {
                        int d = e.getDuration() / 20;
                        s += "  " + (e.isInfiniteDuration() ? "inf" : String.format("%d:%02d", d / 60, d % 60));
                    }
                    c.text(s, 0, y, 0); y += 11;
                }
                if (c.empty()) return pv ? new Canvas().text("Speed II  1:30", 0, 0, Style.dim()) : null;
                return c;
            }
        });
        // ---------------------------------------------------------------- item counter
        ALL.add(new HudElem(Mods.ITEMC) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                LocalPlayer p = mc.player; if (p == null) return null;
                ItemStack proto = switch (Mods.ITEMC_ITEM.idx()) {
                    case 1 -> new ItemStack(Items.GOLDEN_APPLE);
                    case 2 -> new ItemStack(Items.ENDER_PEARL);
                    case 3 -> new ItemStack(Items.END_CRYSTAL);
                    case 4 -> new ItemStack(Items.ARROW);
                    case 5 -> new ItemStack(Items.EXPERIENCE_BOTTLE);
                    default -> new ItemStack(Items.TOTEM_OF_UNDYING);
                };
                int n = 0; var inv = p.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack s = inv.getItem(i); if (s.is(proto.getItem())) n += s.getCount(); }
                if (n == 0 && Mods.ITEMC_HIDE0.on() && !pv) return null;
                Canvas c = new Canvas();
                c.item(proto, 0, 0);
                c.text("x " + n, 20, 4, n == 0 ? 0xFFFF5555 : 0);
                return c;
            }
        });
        // ---------------------------------------------------------------- state
        ALL.add(new HudElem(Mods.STATUS) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                LocalPlayer p = mc.player; if (p == null) return null;
                String s = p.getAbilities().flying ? "Flying" : p.isFallFlying() ? "Gliding" : p.isSwimming() ? "Swimming"
                        : p.isCrouching() ? "Sneaking" : p.isSprinting() ? "Sprinting" : null;
                if (s == null) return pv ? new Canvas().text("Sprinting", 0, 0, Style.dim()) : null;
                return new Canvas().text(s, 0, 0, Style.accent());
            }
        });
        // ---------------------------------------------------------------- speed
        ALL.add(new HudElem(Mods.SPEED) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                boolean kmh = Mods.SPEED_UNIT.idx() == 1;
                float v = Visuals.speed * (kmh ? 3.6f : 1f);
                return new Canvas().text(String.format("%.1f %s", v, kmh ? "km/h" : "b/s"), 0, 0, 0);
            }
        });
        // ---------------------------------------------------------------- clock
        ALL.add(new HudElem(Mods.CLOCK) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                Canvas c = new Canvas(); int y = 0;
                if (Mods.CLOCK_REAL.on()) {
                    LocalTime t = LocalTime.now();
                    String s = Mods.CLOCK_24.on() ? String.format("%02d:%02d", t.getHour(), t.getMinute())
                            : String.format("%d:%02d %s", t.getHour() % 12 == 0 ? 12 : t.getHour() % 12, t.getMinute(), t.getHour() < 12 ? "AM" : "PM");
                    c.text("Real  " + s, 0, y, 0); y += 11;
                }
                if (Mods.CLOCK_GAME.on() && mc.level != null) {
                    long dt = ((mc.level.getDayTime() % 24000L) + 24000L) % 24000L;
                    int mins = (int) (((dt / 1000f + 6f) % 24f) * 60f) % 1440;
                    c.text("Game  " + String.format("%02d:%02d", mins / 60, mins % 60), 0, y, Style.dim());
                }
                return c.empty() ? null : c;
            }
        });
        // ---------------------------------------------------------------- now playing
        ALL.add(new HudElem(Mods.PLAYING) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                boolean live = MusicBridge.isActive() && !MusicBridge.title.isEmpty();
                if (!live && !pv) return null;
                Canvas c = new Canvas();
                if (live && MusicBridge.ad) return c.ctext("Ad playing (muted)", 0, Style.dim());
                String t = live ? MusicBridge.title : "Song Title", a = live ? MusicBridge.artist : "Artist";
                c.ctext("♪ " + UI.fit(t, 220), 0, 0);
                if (!a.isEmpty()) c.ctext(UI.fit(a, 220), 11, Style.dim());
                double d = live ? MusicBridge.duration : 100, e = live ? MusicBridge.elapsed() : 40;
                if (Mods.PLAYING_BAR.on() && d > 0) {
                    int bw = 150, y = a.isEmpty() ? 13 : 24;
                    int fill = (int) (bw * Mth.clamp(e / d, 0, 1));
                    c.crect(bw, y, 3, 0x40FFFFFF);
                    if (fill > 0) c.gradBar((Math.max(c.w, bw) - bw) / 2, y, fill, 3, false);
                }
                return c;
            }
        });
        // ---------------------------------------------------------------- combo
        ALL.add(new HudElem(Mods.COMBO) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                if (Visuals.combo < 2 && !pv) return null;
                int n = Visuals.combo < 2 ? 5 : Visuals.combo;
                Canvas c = new Canvas();
                float k = Mth.clamp(n / 20f, 0f, 1f);
                int col = UI.mix(UI.mix(0xFF8BE9FF, 0xFFFFC857, k * 2f), 0xFFFF5C9A, Math.max(0f, k * 2f - 1f));
                return c.text("x" + n + " COMBO", 0, 0, col);
            }
        });
        // ---------------------------------------------------------------- lyrics
        ALL.add(new HudElem(Mods.LYRICS) {
            @Override public Canvas build(Minecraft mc, boolean pv) {
                LocalPlayer p = mc.player; if (p == null) return null;
                Canvas c = new Canvas(); c.noPanel = true;
                if (pv) return c.ctext("This is where the current lyric line appears", 0, 0xFFFFFFFF).ctext("Next line preview", 12, 0x99FFFFFF);
                if (!MusicBridge.isActive() || MusicBridge.ad) return null;
                List<MusicBridge.Line> ls = MusicBridge.lines;
                if (ls.isEmpty()) return null;
                float vis = Mods.LYR_ALWAYS.on() ? 1f : Mth.clamp((-p.getXRot() - Mods.LYR_PITCH.f()) / 25f, 0, 1);
                if (vis <= 0.02f) return null;
                double pos = MusicBridge.pos();
                int idx = -1;
                for (int i = 0; i < ls.size(); i++) { if (ls.get(i).t() <= pos) idx = i; else break; }
                int ctx = Mods.LYR_LINES.i(), a = (int) (255 * vis), y = 0;
                for (int k = -ctx; k <= ctx; k++) {
                    int i = idx + k;
                    if (i >= 0 && i < ls.size() && !ls.get(i).s().isEmpty()) {
                        boolean cur = k == 0;
                        c.ctext(ls.get(i).s(), y, UI.argb((int) (a * (cur ? 1f : 0.45f)), cur ? 0xFFFFFF : 0xBBBBBB));
                    }
                    y += 12;
                }
                return c.empty() ? null : c;
            }
        });
    }
}
