package com.sirens.visuals.mixin;

import com.sirens.visuals.Hooks;
import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels the vanilla full-screen fire; the HUD layer then draws the low version. */
@Mixin(ScreenEffectRenderer.class)
public abstract class FireMixin {
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sirens$fire(CallbackInfo ci) {
        if (Mods.LOWFIRE.on()) { Hooks.hit("fire"); ci.cancel(); }
    }
}
