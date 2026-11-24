package com.sirens.visuals.fx;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Per-tick gameplay effects and the shared state read by the HUD layer. */
public final class Visuals {
    private Visuals() { }

    public static final class Pop { public String s; public int age; public float dx; }

    public static int tickN, fireTick = -100, combo, marker;
    public static float wet, speed;
    public static boolean cinematicOn;
    public static final List<Pop> pops = new ArrayList<>();

    private static final Map<Integer, Float> tracked = new HashMap<>();
    private static final Map<Integer, Integer> trackedAge = new HashMap<>();
    private static int lastHit = -1000;
    private static final int COMBO_TIMEOUT = 40;
    private static float lastHp = -1f;
    private static final Map<Integer, Integer> hurtSeen = new HashMap<>();
    private static double stepDist;
    private static boolean nvForced;

    // click counters (polled per frame so fast clicking is counted)
    private static final long[] lc = new long[256], rc = new long[256];
    private static int lh, rh, ln, rn;
    private static boolean lDown, rDown;

    public static void pollClicks(Minecraft mc) {
        if (mc.options == null) return;
        long now = System.currentTimeMillis();
        boolean l = mc.options.keyAttack.isDown(), r = mc.options.keyUse.isDown();
        if (l && !lDown) { lc[lh++ & 255] = now; ln = Math.min(256, ln + 1); }
        if (r && !rDown) { rc[rh++ & 255] = now; rn = Math.min(256, rn + 1); }
        lDown = l; rDown = r;
    }

    public static int cps(boolean right) {
        long now = System.currentTimeMillis();
        long[] a = right ? rc : lc;
        int h = right ? rh : lh, n = right ? rn : ln, c = 0;
        for (int i = 1; i <= n; i++) if (now - a[(h - i) & 255] <= 1000) c++; else break;
        return c;
    }

    /**
     * Called by the attack callback on every swing at an entity, including spam clicks that do nothing (cooldown,
     * invulnerability frames, non-living targets). It only registers the target; the hit is counted by tickMain once
     * the target's health really drops (see {@link #confirmHit}).
     */
    public static void onAttack(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(target instanceof LivingEntity le) || !le.isAlive()) return;
        if (!tracked.containsKey(target.getId())) tracked.put(target.getId(), le.getHealth());
        trackedAge.put(target.getId(), tickN);
    }

    /** A swing of ours that actually damaged {@code le}: combo, hit marker and hit effects. */
    private static void confirmHit(ClientLevel lv, LivingEntity le) {
        if (tickN - lastHit > COMBO_TIMEOUT) combo = 0;
        combo++;
        lastHit = tickN;
        marker = 6;
        if (!Mods.HITFX.on()) return;
        int rings = Mods.HIT_RINGS.i();
        double y = le.getY() + le.getBbHeight() * 0.5;
        float sz = Mods.HIT_SIZE.f();
        int c1 = Mods.HIT_COL.rgb(), c2 = 0x000000 | Mods.M_ACCENT2.rgb();
        for (int r = 0; r < rings; r++) {
            double rad = (0.45 + r * 0.35) * sz;
            int n = 14 + r * 4;
            Fx.ring(lv, le.getX(), y, le.getZ(), rad, n, Fx.particle(Mods.HIT_PART.idx(), c1, 1f));
            if (Mods.HIT_PART.idx() != Mods.PARTICLES.length - 1)
                for (int i = 0; i < n; i += 2) {
                    double an = (i + 0.5) * Math.PI * 2 / n;
                    Fx.dust(lv, le.getX() + Math.cos(an) * rad, y, le.getZ() + Math.sin(an) * rad, Fx.grad(c1, c2, r / 2f), 0.6f);
                }
        }
        for (int i = 0; i < 8; i++) {
            double an = Fx.R.nextDouble() * Math.PI * 2, sp = 0.08 + Fx.R.nextDouble() * 0.1;
            Fx.dust(lv, le.getX(), y, le.getZ(), Math.cos(an) * sp, 0.04 + Fx.R.nextDouble() * 0.08, Math.sin(an) * sp, Fx.grad(c1, c2, Fx.R.nextFloat()), 0.7f);
        }
    }

    static void kill(ClientLevel lv, Entity e) {
        if (!Mods.KILLFX.on()) return;
        var o = switch (Mods.KILL_STYLE.idx()) {
            case 0 -> ParticleTypes.TOTEM_OF_UNDYING;
            case 1 -> ParticleTypes.SOUL;
            case 2 -> ParticleTypes.FIREWORK;
            case 3 -> ParticleTypes.HEART;
            case 4 -> ParticleTypes.END_ROD;
            case 5 -> ParticleTypes.FLAME;
            case 6 -> ParticleTypes.SNOWFLAKE;
            default -> ParticleTypes.ENCHANT;
        };
        int n = Math.min(40, (int) (14 * Mods.KILL_INT.f()));
        double cy = e.getY() + e.getBbHeight() * 0.5;
        for (int i = 0; i < n; i++)
            Fx.add(lv, o, e.getX() + (Fx.R.nextDouble() - .5) * e.getBbWidth(), e.getY() + Fx.R.nextDouble() * e.getBbHeight(),
                    e.getZ() + (Fx.R.nextDouble() - .5) * e.getBbWidth(), (Fx.R.nextDouble() - .5) * 0.2, 0.1 + Fx.R.nextDouble() * 0.15, (Fx.R.nextDouble() - .5) * 0.2);
        int c1 = Mods.M_ACCENT.rgb(), c2 = Mods.M_ACCENT2.rgb();
        int ring = Math.min(36, (int) (20 * Mods.KILL_INT.f()));
        for (int i = 0; i < ring; i++) {
            double an = i * Math.PI * 2 / ring, sp = 0.12;
            Fx.dust(lv, e.getX(), cy, e.getZ(), Math.cos(an) * sp, 0.02, Math.sin(an) * sp, Fx.grad(c1, c2, i / (float) ring), 0.9f);
        }
        for (int i = 0; i < 10; i++) Fx.glow(lv, e.getX() + (Fx.R.nextDouble() - .5) * 0.4, e.getY() + i * e.getBbHeight() / 10, e.getZ() + (Fx.R.nextDouble() - .5) * 0.4, Fx.grad(c1, c2, i / 10f), 0.8f);
    }

    public static void tick(Minecraft mc) {
        try { tickMain(mc); } catch (Throwable t) { com.sirens.visuals.Hooks.error("tick", t); runLate(mc); }
    }

    /** Still run the cosmetics/emotes if the main tick threw. */
    private static void runLate(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        try { Cosmetics.tick(mc, mc.level, mc.player, tickN); } catch (Throwable t) { com.sirens.visuals.Hooks.error("cosmetics", t); }
        try { Emotes.tick(mc.level, mc.player); } catch (Throwable t) { com.sirens.visuals.Hooks.error("emotes", t); }
    }

    private static void tickMain(Minecraft mc) {
        tickN++;
        ClientLevel lv = mc.level;
        LocalPlayer p = mc.player;
        if (lv == null || p == null) { pops.clear(); tracked.clear(); return; }

        if (marker > 0) marker--;
        if (tickN - lastHit > COMBO_TIMEOUT) combo = 0;
        // taking damage breaks the combo
        float myHp = p.getHealth();
        if (lastHp >= 0f && (myHp < lastHp - 0.01f || p.hurtTime >= p.hurtDuration - 1)) combo = 0;
        lastHp = myHp;
        for (Iterator<Pop> it = pops.iterator(); it.hasNext(); ) { Pop q = it.next(); if (++q.age > 25) it.remove(); }

        // damage numbers + kill effects for entities we hit
        for (Iterator<Map.Entry<Integer, Float>> it = tracked.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity e = lv.getEntity(en.getKey());
            if (!(e instanceof LivingEntity le) || tickN - trackedAge.getOrDefault(en.getKey(), 0) > 100) { hurtSeen.remove(en.getKey()); it.remove(); continue; }
            float hp = le.getHealth(), old = en.getValue();
            int id = en.getKey();
            boolean recent = tickN - trackedAge.getOrDefault(id, -1000) <= 12;
            boolean lost = hp < old - 0.01f;
            // second signal: the hurt flash just started (covers absorption / armour edge cases where health does not drop)
            boolean flash = le.hurtTime >= le.hurtDuration - 1 && tickN - hurtSeen.getOrDefault(id, -1000) > 8;
            if (lost || flash) hurtSeen.put(id, tickN);
            if ((lost || flash) && recent) {
                confirmHit(lv, le);
                if (lost && Mods.DMG.on()) {
                    Pop q = new Pop();
                    q.s = String.format("-%.1f", old - hp);
                    q.dx = Fx.R.nextInt(20) - 10;
                    pops.add(q);
                }
            }
            if (lost || hp > old) en.setValue(hp);
            if (le.isDeadOrDying() || hp <= 0) { if (tickN - trackedAge.getOrDefault(id, -1000) <= 40) kill(lv, e); it.remove(); }
        }

        // speed
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        speed = (float) (Math.sqrt(dx * dx + dz * dz) * 20.0);

        // wetness
        BlockPos bp = p.blockPosition();
        boolean inWater = p.isInWater() || p.isUnderWater();
        boolean rained = lv.isRaining() && lv.canSeeSky(bp.above());
        float dry = Math.max(5f, Mods.WET_DRY.f()) * 20f;
        if (Mods.WET.on()) {
            if (inWater) wet = Math.min(1f, wet + 0.08f);
            else if (rained) wet = Math.min(1f, wet + 0.01f);
            else wet = Math.max(0f, wet - 1f / dry);
        } else wet = Math.max(0f, wet - 0.05f);

        if (Mods.WET.on()) {
            if (Mods.WET_PUDDLE.on() && lv.isRaining()) {
                int n = (int) (3 * Mods.WET_DENS.f());
                for (int i = 0; i < n; i++) {
                    int x = bp.getX() + Fx.R.nextInt(17) - 8, z = bp.getZ() + Fx.R.nextInt(17) - 8;
                    BlockPos g = lv.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, bp.getY(), z));
                    if (Math.abs(g.getY() - bp.getY()) < 6)
                        Fx.add(lv, ParticleTypes.RAIN, x + Fx.R.nextDouble(), g.getY() + 0.05, z + Fx.R.nextDouble(), 0, 0, 0);
                }
            }
            if (wet > 0.3f && p.onGround() && speed > 1f && Mods.WET_STEPS.on()) {
                stepDist += speed / 20.0;
                if (stepDist > 0.8) {
                    stepDist = 0;
                    Fx.add(lv, ParticleTypes.SPLASH, p.getX(), p.getY() + 0.05, p.getZ(), 0, 0.02, 0);
                }
            }
            if (Mods.WET_DRIP.on() && wet > 0.35f && !inWater && Fx.R.nextFloat() < wet * 0.2f)
                Fx.add(lv, ParticleTypes.FALLING_WATER, p.getX() + (Fx.R.nextDouble() - .5) * 0.6, p.getY() + 0.3 + Fx.R.nextDouble() * 1.4, p.getZ() + (Fx.R.nextDouble() - .5) * 0.6, 0, 0, 0);
        }

        // footsteps: distance based (own position tracking), works while sneaking, sprinting and after landing
        if (Mods.STEPS.on()) footsteps(lv, p);
        else { footHave = false; }

        // fire overlay timestamp (fallback when the render hook is not available)
        if (p.isOnFire()) fireTick = tickN;

        // fullbright via a client-side night vision effect (no particles / icon)
        if (Mods.FULLBRIGHT.on()) {
            MobEffectInstance cur = p.getEffect(MobEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 220) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
                nvForced = true;
            }
        } else if (nvForced) {
            p.removeEffect(MobEffects.NIGHT_VISION);
            nvForced = false;
        }

        if (Mods.NOWEATHER.on()) { lv.setRainLevel(0f); lv.setThunderLevel(0f); }

        try { Cosmetics.tick(mc, lv, p, tickN); } catch (Throwable t) { com.sirens.visuals.Hooks.error("cosmetics", t); }
        try { Emotes.tick(lv, p); } catch (Throwable t) { com.sirens.visuals.Hooks.error("emotes", t); }
    }

    private static double footDist, footX, footZ;
    private static boolean footSide, footHave, wasGround = true;

    private static void footsteps(ClientLevel lv, LocalPlayer p) {
        double d = footHave ? Math.hypot(p.getX() - footX, p.getZ() - footZ) : 0;
        footX = p.getX(); footZ = p.getZ(); footHave = true;
        boolean ground = p.onGround();
        boolean landed = ground && !wasGround;
        wasGround = ground;
        if (p.isSpectator() || p.isPassenger() || p.isFallFlying() || p.getAbilities().flying || p.isInWater() || p.isInLava() || p.onClimbable()) { footDist = 0; return; }
        if (!ground && !landed) return;               // airborne: nothing, and no distance is banked
        if (d > 2.0) { footDist = 0; return; }        // teleport / respawn
        footDist += d;
        double gap = Math.max(0.3, Mods.ST_GAP.f()) * (p.isSprinting() ? 1.25 : p.isCrouching() ? 0.8 : 1.0);
        if (!landed && footDist < gap) return;
        // standing on air-like blocks (e.g. just left a ladder) or a fluid surface: skip
        if (lv.getBlockState(p.blockPosition().below()).isAir() && lv.getBlockState(p.blockPosition()).isAir() && !landed) return;
        footDist = 0; footSide = !footSide;
        double yaw = Math.toRadians(p.getYRot()), side = footSide ? 0.15 : -0.15;
        double ox = Math.cos(yaw) * side, oz = Math.sin(yaw) * side, y = p.getY() + 0.03;
        int idx = Mods.ST_PART.idx();
        int c1 = Mods.ST_COL.rgb(), c2 = Mods.M_ACCENT2.rgb();
        if (idx == Mods.PARTICLES.length - 1) {
            // a soft footprint: bright core + a small ring of dim dust
            Fx.glow(lv, p.getX() + ox, y, p.getZ() + oz, c1, Mods.ST_SIZE.f());
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + yaw;
                Fx.dust(lv, p.getX() + ox + Math.cos(a) * 0.09, y, p.getZ() + oz + Math.sin(a) * 0.09, Fx.grad(c1, c2, 0.5f), Mods.ST_SIZE.f() * 0.5f);
            }
        } else {
            Fx.add(lv, Fx.particle(idx, c1, Mods.ST_SIZE.f()), p.getX() + ox, y, p.getZ() + oz, 0, 0.01, 0);
            Fx.dust(lv, p.getX() + ox, y, p.getZ() + oz, Fx.grad(c1, c2, 0.4f), Mods.ST_SIZE.f() * 0.6f);
        }
    }
}
