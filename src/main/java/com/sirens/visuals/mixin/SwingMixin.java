package com.sirens.visuals.mixin;

import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SwingMixin {
    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
    private void sirens$swing(CallbackInfoReturnable<Integer> cir) {
        if (!Mods.SWING.on() || (Object) this != Minecraft.getInstance().player) return;
        cir.setReturnValue(Math.round(cir.getReturnValue() * Mods.SW_MUL.f()));
    }
}
