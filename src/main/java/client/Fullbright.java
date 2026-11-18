package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {
    public Fullbright() { super("Fullbright", Category.VISUALS); }

    @Override
    public void onTick() {
        var p = MinecraftClient.getInstance().player;
        var e = p.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (e == null || e.getDuration() < 400)
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false));
    }

    @Override
    public void onDisable() {
        var p = MinecraftClient.getInstance().player;
        if (p != null) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
