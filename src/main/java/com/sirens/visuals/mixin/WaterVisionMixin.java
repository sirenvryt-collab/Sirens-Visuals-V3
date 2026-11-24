package com.sirens.visuals.mixin;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class WaterVisionMixin {
    @Inject(method = "getWaterVision", at = @At("RETURN"), cancellable = true, require = 0)
    private void sirens$vision(CallbackInfoReturnable<Float> cir) {
        if (Mods.WATER.on() && Mods.W_CLEAR.on()) { Hooks.hit("watervision"); cir.setReturnValue(1.0f); }
    }
}
