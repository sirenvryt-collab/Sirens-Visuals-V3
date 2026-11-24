package com.sirens.visuals.mixin;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraDistMixin {
    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true, require = 0)
    private void sirens$noclip(CallbackInfoReturnable<Float> cir) {
        if (Mods.THIRD.on() && Mods.TP_NOCLIP.on()) { Hooks.hit("camdist"); cir.setReturnValue(Mods.TP_DIST.f()); }
    }

    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float sirens$dist(float d) {
        if (Mods.THIRD.on() && !Mods.TP_NOCLIP.on()) { Hooks.hit("camdist"); return Mods.TP_DIST.f(); }
        return d;
    }
}
