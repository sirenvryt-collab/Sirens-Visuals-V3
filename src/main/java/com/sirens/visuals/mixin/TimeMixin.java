package com.sirens.visuals.mixin;

import com.sirens.visuals.fx.SkyFx;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class TimeMixin {
    @Inject(method = {"getDayTime", "getOverworldClockTime"}, at = @At("RETURN"), cancellable = true, require = 0)
    private void sirens$time(CallbackInfoReturnable<Long> cir) {
        if (!((Object) this instanceof ClientLevel)) return;
        Long v = SkyFx.time(cir.getReturnValue());
        if (v != null) { com.sirens.visuals.Hooks.hit("time"); cir.setReturnValue(v); }
    }
}
