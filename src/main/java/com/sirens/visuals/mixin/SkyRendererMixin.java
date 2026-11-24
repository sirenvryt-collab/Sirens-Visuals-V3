package com.sirens.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sirens.visuals.Hooks;
import com.sirens.visuals.cfg.Mods;
import com.sirens.visuals.fx.SkyDome;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    /** Runs just before vanilla draws sun, moon and stars: the skybox goes behind them (or replaces them). */
    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true, require = 0)
    private void sirens$skybox(PoseStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase phase,
                               float rain, float starBrightness, CallbackInfo ci) {
        if (!SkyDome.active()) return;
        Hooks.hit("skydome");
        try { SkyDome.render(pose); } catch (Throwable t) { Hooks.error("skydome", t); return; }
        if (Mods.S_IMG_HIDE.on()) ci.cancel();
    }
}
