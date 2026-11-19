package client.mixin;

import client.Removals;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class GlowMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void mik$glow(CallbackInfoReturnable<Boolean> cir) {
        if (Removals.glow()) cir.setReturnValue(false);
    }
}
