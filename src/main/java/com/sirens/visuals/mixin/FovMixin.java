package com.sirens.visuals.mixin;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.fx.CameraFx;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class FovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void sirens$fov(CallbackInfoReturnable<Float> cir) {
        float d = CameraFx.fovDiv();
        if (d > 1.001f) { Hooks.hit("fov"); cir.setReturnValue(cir.getReturnValue() / d); }
    }
}
