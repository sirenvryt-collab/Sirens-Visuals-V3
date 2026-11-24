package com.sirens.visuals.fx;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;

/** Particle-based cosmetics around the local player. Only visible to you (third person or with the first-person option). */
public final class Cosmetics {
    private Cosmetics() { }

    static int col(float t, boolean second) {
        if (Mods.CO_RAINBOW.on()) return Fx.rainbow(t);
        return second ? Mods.CO_C2.rgb() : Mods.CO_C1.rgb();
    }

    /** Position relative to the player body: right/back/up in blocks, rotated by body yaw. */
    static double[] at(LocalPlayer p, double right, double back, double up) {
        double yaw = Math.toRadians(p.yBodyRot);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw), bx = Math.sin(yaw), bz = -Math.cos(yaw);
        return new double[]{p.getX() + rx * right + bx * back, p.getY() + up, p.getZ() + rz * right + bz * back};
    }

    /** True with probability perTick (clamped to 0..1). */
    static boolean chance(float perTick) { return Fx.R.nextFloat() < perTick; }

    private static double lastX, lastZ;
    private static boolean haveLast;

    public static void tick(Minecraft mc, ClientLevel lv, LocalPlayer p, int tick) {
        double mdx = haveLast ? p.getX() - lastX : 0, mdz = haveLast ? p.getZ() - lastZ : 0;
        lastX = p.getX(); lastZ = p.getZ(); haveLast = true;
        if (!Mods.COSM.on() || Mods.CO_TYPE.idx() == 0) return;
        if (!Mods.CO_FIRST.on() && mc.options.getCameraType().isFirstPerson()) return;
        if (p.isInvisible() || p.isSpectator()) return;
        float size = Mods.CO_SIZE.f(), dens = Mods.CO_DENS.f();
        float t = tick * 0.02f;
        double h = p.getBbHeight();
        boolean moving = mdx * mdx + mdz * mdz > 0.0004;
        switch (Mods.CO_TYPE.idx()) {
            case 1 -> { // halo: bright ring + soft outer ring + a few rising sparks
                int n = Math.max(6, (int) (9 * dens));
                double cy = p.getY() + h + 0.32 * size + Math.sin(tick * 0.1) * 0.02;
                for (int i = 0; i < n; i++) {
                    double a = tick * 0.12 + i * Math.PI * 2 / n;
                    Fx.dust(lv, p.getX() + Math.cos(a) * 0.34 * size, cy, p.getZ() + Math.sin(a) * 0.34 * size, col(t + i / (float) n, false), 0.8f);
                }
                if (chance(0.5f * dens)) {
                    double a = Fx.R.nextDouble() * Math.PI * 2;
                    Fx.glow(lv, p.getX() + Math.cos(a) * 0.42 * size, cy + 0.02, p.getZ() + Math.sin(a) * 0.42 * size, col(t + 0.5f, true), 0.7f);
                    Fx.add(lv, ParticleTypes.END_ROD, p.getX() + Math.cos(a) * 0.34 * size, cy, p.getZ() + Math.sin(a) * 0.34 * size, 0, 0.012, 0);
                }
            }
            case 2 -> { // wings: feathers fanning out from the shoulders, colour gradient root -> tip
                int feathers = 6;
                float flap = (float) Math.sin(tick * 0.18) * 0.22f + (moving ? 0.1f : 0f);
                double shoulder = p.getY() + (p.isCrouching() ? 1.05 : 1.3);
                int per = Math.max(3, (int) (6 * dens));
                for (int side = -1; side <= 1; side += 2) {
                    for (int k = 0; k < per; k++) {
                        int f = Fx.R.nextInt(feathers);
                        float fk = f / (float) (feathers - 1);
                        double ang = Math.toRadians(14 + fk * 92) + flap * (0.6 + fk);
                        double len = (1.25 - 0.62 * fk) * size;
                        float u = 0.25f + Fx.R.nextFloat() * 0.75f;
                        double right = side * (0.14 + u * len * Math.sin(ang));
                        double up = u * len * Math.cos(ang) * 0.95 - fk * 0.25 * size;
                        double back = 0.2 + u * 0.12 + Math.abs(flap) * u * 0.5;
                        double[] q = at(p, right, back, up + shoulder - p.getY());
                        int c = Fx.grad(col(t, false), col(t + 0.2f, true), u);
                        if (Mods.CO_RAINBOW.on()) c = Fx.rainbow(t + u * 0.25f + fk * 0.1f);
                        Fx.dust(lv, q[0], q[1], q[2], c, 0.7f + (u > 0.85f ? 0.3f : 0f));
                        if (u > 0.9f && chance(0.25f)) Fx.add(lv, ParticleTypes.END_ROD, q[0], q[1], q[2], 0, -0.01, 0);
                    }
                }
            }
            case 3 -> { // aura: rising double spiral + sparkles
                int n = Math.max(2, (int) (4 * dens));
                for (int i = 0; i < n; i++) {
                    double yy = ((tick * 0.04 + i * 0.7) % 1.0) * h;
                    for (int s2 = 0; s2 < 2; s2++) {
                        double a = tick * 0.12 + yy * 5 + s2 * Math.PI, r = (0.5 + 0.1 * Math.sin(yy * 6)) * size;
                        Fx.dust(lv, p.getX() + Math.cos(a) * r, p.getY() + yy, p.getZ() + Math.sin(a) * r, col(t + (float) yy * 0.3f, s2 == 1), 0.7f);
                    }
                }
                if (chance(0.4f * dens)) {
                    double a = Fx.R.nextDouble() * Math.PI * 2, r = 0.7 * size;
                    Fx.add(lv, ParticleTypes.END_ROD, p.getX() + Math.cos(a) * r, p.getY() + Fx.R.nextDouble() * h, p.getZ() + Math.sin(a) * r, 0, 0.02, 0);
                }
            }
            case 4 -> { // orbit: three glowing orbs with short comet trails
                for (int k = 0; k < 3; k++) {
                    for (int trail = 0; trail < 5; trail++) {
                        double a = tick * 0.16 - trail * 0.14 + k * Math.PI * 2 / 3;
                        double r = 0.95 * size, y = p.getY() + h * 0.55 + Math.sin(tick * 0.1 + k * 2.1 - trail * 0.1) * 0.3;
                        float fade = 1f - trail / 5f;
                        if (trail == 0) Fx.glow(lv, p.getX() + Math.cos(a) * r, y, p.getZ() + Math.sin(a) * r, col(t + k * 0.2f, k == 1), 1.0f);
                        else if (trail % 2 == 1 || dens > 1.5f) Fx.dust(lv, p.getX() + Math.cos(a) * r, y, p.getZ() + Math.sin(a) * r, col(t + k * 0.2f + trail * 0.02f, k == 1), 0.8f * fade);
                    }
                }
            }
            case 5 -> { // crown: solid spiked ring
                int spikes = 5;
                double base = p.getY() + h + 0.02;
                for (int i = 0; i < spikes; i++) {
                    double a = i * Math.PI * 2 / spikes + tick * 0.01, r = 0.3 * size;
                    double bx = p.getX() + Math.cos(a) * r, bz = p.getZ() + Math.sin(a) * r;
                    for (int s2 = 0; s2 < 3; s2++) Fx.dust(lv, bx, base + 0.05 + s2 * 0.09 * size, bz, col(t + s2 * 0.1f, s2 == 2), 0.65f);
                    if (chance(0.15f * dens)) Fx.add(lv, ParticleTypes.END_ROD, bx, base + 0.35 * size, bz, 0, 0.01, 0);
                }
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4 + tick * 0.01 + Math.PI / 5;
                    Fx.dust(lv, p.getX() + Math.cos(a) * 0.3 * size, base + 0.04, p.getZ() + Math.sin(a) * 0.3 * size, col(t + 0.3f, true), 0.55f);
                }
            }
            case 6 -> { // trail: ribbon of colour behind your feet
                if (moving) {
                    int n = Math.max(2, (int) (4 * dens));
                    for (int i = 0; i < n; i++) {
                        float f = i / (float) n;
                        double px = p.getX() - mdx * f, pz = p.getZ() - mdz * f;
                        Fx.glow(lv, px + (Fx.R.nextDouble() - .5) * 0.25, p.getY() + 0.1 + Fx.R.nextDouble() * 0.5 * size, pz + (Fx.R.nextDouble() - .5) * 0.25, col(t + i * 0.1f, i % 2 == 1), 0.8f * size);
                    }
                }
            }
            case 7 -> { // cape sparkle
                int n = Math.max(1, (int) (2 * dens));
                for (int i = 0; i < n; i++) {
                    double[] q = at(p, (Fx.R.nextDouble() - .5) * 0.5, 0.28, 0.5 + Fx.R.nextDouble() * 0.9);
                    if (chance(0.7f)) Fx.add(lv, ParticleTypes.END_ROD, q[0], q[1], q[2], 0, -0.012, 0);
                    else Fx.dust(lv, q[0], q[1], q[2], col(t + (float) Fx.R.nextDouble() * 0.3f, Fx.R.nextBoolean()), 0.6f);
                }
            }
            default -> { }
        }
    }
}
