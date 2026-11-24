package com.sirens.visuals.mixin;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.fx.SkyFx;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class SkyColorMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void sirens$sky(CallbackInfoReturnable<Vec3> cir) {
        Vec3 v = SkyFx.skyColor(cir.getReturnValue());
        if (v != null) { Hooks.hit("dome"); cir.setReturnValue(v); }
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true, require = 0)
    private void sirens$stars(CallbackInfoReturnable<Float> cir) {
        Float f = SkyFx.stars(cir.getReturnValue());
        if (f != null) { Hooks.hit("celestial"); cir.setReturnValue(f); }
    }
}
