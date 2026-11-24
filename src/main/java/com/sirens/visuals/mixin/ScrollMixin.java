package com.sirens.visuals.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.sirens.visuals.Hooks;
import com.sirens.visuals.fx.CameraFx;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class ScrollMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void sirens$scroll(CallbackInfo ci, @Local(argsOnly = true, ordinal = 1) double dy) {
        if (CameraFx.wantsScroll()) { Hooks.hit("scroll"); CameraFx.scroll(dy); ci.cancel(); }
    }
}
