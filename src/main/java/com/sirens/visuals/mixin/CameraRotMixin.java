package com.sirens.visuals.mixin;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.fx.CameraFx;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public abstract class CameraRotMixin {
    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float sirens$yaw(float yaw) {
        if (!CameraFx.freelook) return yaw;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return yaw;
        Hooks.hit("camrot");
        return yaw + (CameraFx.fyaw - p.getYRot());
    }

    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private float sirens$pitch(float pitch) {
        if (!CameraFx.freelook) return pitch;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return pitch;
        float d = CameraFx.fpitch - p.getXRot();
        return pitch + (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT ? -d : d);
    }
}
