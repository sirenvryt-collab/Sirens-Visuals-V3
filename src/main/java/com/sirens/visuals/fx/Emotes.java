package com.sirens.visuals.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;

public final class Emotes {
    private Emotes() { }
    public static final String[] NAMES = {"Wave", "Love", "Party", "Angry", "Happy", "Music", "Sparkle", "Magic", "Flames", "Snow", "Halo Burst", "Dizzy"};
    public static int cur = -1, left = 0, total = 0;

    private static boolean forcedCam;

    public static void play(int i, float seconds) {
        cur = i; total = left = Math.max(10, (int) (seconds * 20));
        // most emotes happen around/above the head: show them by switching to third person for the duration
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson()) { mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK); forcedCam = true; }
    }

    static void head(ClientLevel lv, LocalPlayer p, net.minecraft.core.particles.ParticleOptions o) {
        Fx.add(lv, o, p.getX() + (Fx.R.nextDouble() - .5) * .6, p.getY() + p.getBbHeight() + 0.25, p.getZ() + (Fx.R.nextDouble() - .5) * .6, 0, 0.05, 0);
    }

    public static void tick(ClientLevel lv, LocalPlayer p) {
        if (left <= 0) {
            if (forcedCam) { forcedCam = false; net.minecraft.client.Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); }
            return;
        }
        left--;
        int n = total - left;
        double top = p.getY() + p.getBbHeight();
        switch (cur) {
            case 0 -> {
                if (left % 8 == 0) p.swing(InteractionHand.MAIN_HAND);
                if (left % 2 == 0) Fx.glow(lv, p.getX() + Math.cos(n * 0.3 + 1.2) * 0.7, top - 0.1 + Math.sin(n * 0.3) * 0.5, p.getZ() + Math.sin(n * 0.3 + 1.2) * 0.7, Fx.rainbow(n * 0.02f), 0.8f);
            }
            case 1 -> { if (left % 3 == 0) head(lv, p, ParticleTypes.HEART); }
            case 2 -> {
                if (left % 2 == 0) head(lv, p, ParticleTypes.FIREWORK);
                Fx.dust(lv, p.getX() + Math.cos(n * 0.4) * 0.8, p.getY() + 1 + (n % 20) * 0.05, p.getZ() + Math.sin(n * 0.4) * 0.8, Fx.rainbow(n * 0.03f), 0.8f);
            }
            case 3 -> { if (left % 4 == 0) head(lv, p, ParticleTypes.ANGRY_VILLAGER); }
            case 4 -> { if (left % 4 == 0) head(lv, p, ParticleTypes.HAPPY_VILLAGER); }
            case 5 -> { if (left % 3 == 0) head(lv, p, ParticleTypes.NOTE); }
            case 6 -> { for (int i = 0; i < 2; i++) Fx.add(lv, ParticleTypes.END_ROD, p.getX() + (Fx.R.nextDouble() - .5) * 1.2, p.getY() + Fx.R.nextDouble() * 2, p.getZ() + (Fx.R.nextDouble() - .5) * 1.2, 0, 0.02, 0); }
            case 7 -> {
                double a = n * 0.35;
                for (int k = 0; k < 2; k++)
                    Fx.add(lv, ParticleTypes.ENCHANT, p.getX() + Math.cos(a + k * Math.PI) * 0.7, p.getY() + 0.2 + (n % 30) * 0.06, p.getZ() + Math.sin(a + k * Math.PI) * 0.7, 0, 0.1, 0);
            }
            case 8 -> { if (left % 2 == 0) Fx.ring(lv, p.getX(), p.getY() + 0.1, p.getZ(), 0.8, 8, ParticleTypes.FLAME); }
            case 9 -> { for (int i = 0; i < 3; i++) Fx.add(lv, ParticleTypes.SNOWFLAKE, p.getX() + (Fx.R.nextDouble() - .5) * 3, top + 1.5, p.getZ() + (Fx.R.nextDouble() - .5) * 3, 0, -0.05, 0); }
            case 10 -> { if (n % 10 == 0) Fx.ring(lv, p.getX(), top + 0.4, p.getZ(), 0.3 + (n % 40) * 0.04, 20, new net.minecraft.core.particles.DustParticleOptions(0xFFE066, 0.8f)); }
            case 11 -> {
                double a = n * 0.5;
                Fx.add(lv, ParticleTypes.CRIT, p.getX() + Math.cos(a) * 0.4, top + 0.2, p.getZ() + Math.sin(a) * 0.4, 0, 0, 0);
                Fx.add(lv, ParticleTypes.CRIT, p.getX() - Math.cos(a) * 0.4, top + 0.2, p.getZ() - Math.sin(a) * 0.4, 0, 0, 0);
            }
            default -> { }
        }
    }
}
