package com.sirens.visuals.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sirens.visuals.Hooks;
import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class HandMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), require = 0)
    private void sirens$handIn(CallbackInfo ci, @Local(argsOnly = true) PoseStack pose) {
        if (!Mods.HANDS.on()) return;
        Hooks.hit("hand");
        pose.pushPose();
        pose.translate(Mods.H_X.f(), Mods.H_Y.f(), Mods.H_Z.f());
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(Mods.H_RX.f()));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(Mods.H_RY.f()));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(Mods.H_RZ.f()));
        float s = Mods.H_SCALE.f();
        pose.scale(s, s, s);
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"), require = 0)
    private void sirens$handOut(CallbackInfo ci, @Local(argsOnly = true) PoseStack pose) {
        if (Mods.HANDS.on()) pose.popPose();
    }
}
