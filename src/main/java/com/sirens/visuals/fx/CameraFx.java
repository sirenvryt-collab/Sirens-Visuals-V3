package com.sirens.visuals.fx;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/** Freelook, cinematic smoothing, smooth zoom. All state is plain floats so mixin handlers stay trivial. */
public final class CameraFx {
    private CameraFx() { }

    public static boolean freelook, zoomHeld, freelookKey, cineKey;
    public static float fyaw, fpitch;           // absolute camera angles while in freelook
    public static float zoomCur = 1f, zoomExtra = 1f;
    private static float pendYaw, pendPitch;
    private static CameraType prevType;
    private static boolean applying;

    public static boolean cinematic() { return Visuals.cinematicOn; }

    public static void setFreelook(Minecraft mc, boolean on) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (on && !freelook) {
            freelook = true; fyaw = p.getYRot(); fpitch = p.getXRot();
            if (Mods.FL_THIRD.on()) { prevType = mc.options.getCameraType(); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); }
        } else if (!on && freelook) {
            freelook = false;
            if (!Mods.FL_RESET.on()) { p.setYRot(fyaw); p.setXRot(Mth.clamp(fpitch, -90f, 90f)); }
            if (prevType != null) { mc.options.setCameraType(prevType); prevType = null; }
        }
    }

    /** Per-frame update (called from the HUD callback). */
    public static void frame(Minecraft mc, float dt) {
        if (mc.player == null) return;
        boolean fl = Mods.FREELOOK.on();
        if (!fl && freelook) setFreelook(mc, false);
        if (fl) {
            if (Mods.FL_MODE.idx() == 0) setFreelook(mc, freelookKey && mc.screen == null);
        }
        boolean z = Mods.ZOOM.on() && zoomHeld && mc.screen == null;
        Visuals.cinematicOn = (Mods.CINE.on() && cineKey) || (z && Mods.Z_CINE.on());
        float target = z ? Math.max(1f, zoomExtra) : 1f;
        float k = 1f - (float) Math.pow(Mods.Z_SMOOTH.f(), dt * 60f);
        zoomCur += (target - zoomCur) * Mth.clamp(k, 0.02f, 1f);
        if (Math.abs(zoomCur - target) < 0.002f) zoomCur = target;
        if (!z) zoomExtra = Mods.Z_AMT.f();
        else if (zoomExtra <= 1f) zoomExtra = Mods.Z_AMT.f();

        // apply pending smoothed rotation
        if (Math.abs(pendYaw) > 0.001f || Math.abs(pendPitch) > 0.001f) {
            float s = Visuals.cinematicOn ? Mods.CI_SMOOTH.f() : 0f;
            float f = 1f - (float) Math.pow(Math.max(0.0001f, s), dt * 60f);
            if (s <= 0f) f = 1f;
            float ay = pendYaw * f, ap = pendPitch * f;
            pendYaw -= ay; pendPitch -= ap;
            LocalPlayer p = mc.player;
            p.setYRot(p.getYRot() + ay);
            p.setXRot(Mth.clamp(p.getXRot() + ap, -90f, 90f));
            p.yRotO += ay; p.xRotO = Mth.clamp(p.xRotO + ap, -90f, 90f);
        }
        if (!Visuals.cinematicOn && (pendYaw != 0 || pendPitch != 0)) {
            LocalPlayer p = mc.player;
            p.setYRot(p.getYRot() + pendYaw); p.setXRot(Mth.clamp(p.getXRot() + pendPitch, -90f, 90f));
            pendYaw = pendPitch = 0;
        }
    }

    /** Return true when the turn was consumed (vanilla turn must be cancelled). dy = yaw input, dp = pitch input (raw units). */
    public static boolean turn(double dy, double dp) {
        if (applying) return false;
        float zs = zoomCur > 1.01f ? 1f / (float) Math.sqrt(zoomCur) : 1f;
        float yaw = (float) dy * 0.15f * zs, pitch = (float) dp * 0.15f * zs;
        if (freelook) {
            float s = Mods.FL_SENS.f();
            fyaw += yaw * s; fpitch = Mth.clamp(fpitch + pitch * s, -90f, 90f);
            return true;
        }
        if (Visuals.cinematicOn) { pendYaw += yaw; pendPitch += pitch; return true; }
        if (zs != 1f) { pendYaw += yaw; pendPitch += pitch; return true; }
        return false;
    }

    public static void scroll(double d) {
        zoomExtra = Mth.clamp(zoomExtra + (float) d * 0.5f, 1.2f, 30f);
    }

    public static boolean wantsScroll() { return Mods.ZOOM.on() && Mods.Z_SCROLL.on() && zoomHeld; }

    public static float fovDiv() { return Mods.ZOOM.on() ? Math.max(1f, zoomCur) : 1f; }
}
