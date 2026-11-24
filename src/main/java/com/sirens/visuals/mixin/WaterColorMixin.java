package com.sirens.visuals.mixin;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.fx.WaterFx;
import net.minecraft.client.renderer.BiomeColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public abstract class WaterColorMixin {
    @Inject(method = "getAverageWaterColor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void sirens$water(CallbackInfoReturnable<Integer> cir) {
        Integer c = WaterFx.tint(cir.getReturnValue());
        if (c != null) { Hooks.hit("watercolor"); cir.setReturnValue(c); }
    }
}
