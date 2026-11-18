package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

/** Настройки для слабых ПК: включил - всё урезается, выключил - возвращается как было. */
public class Optimize extends Module {
    private final Setting dist      = add(Setting.num("View distance", 6, 2, 16));
    private final Setting entity    = add(Setting.num("Entity distance", 0.5, 0.5, 1.5));
    private final Setting particles = add(Setting.bool("Minimal particles", true));
    private final Setting fast      = add(Setting.bool("Fast graphics", true));
    private final Setting clouds    = add(Setting.bool("No clouds", true));
    private final Setting shadows   = add(Setting.bool("No entity shadows", true));
    private final Setting ao        = add(Setting.bool("No smooth lighting", true));

    private int oDist, oBlend;
    private double oEntity;
    private ParticlesMode oPart;
    private GraphicsMode oGfx;
    private CloudRenderMode oCloud;
    private boolean oShadow, oAo;

    public Optimize() { super("Optimize", Category.MISC); }

    @Override
    public void onEnable() {
        GameOptions o = MinecraftClient.getInstance().options;
        oDist = o.getViewDistance().getValue();
        oEntity = o.getEntityDistanceScaling().getValue();
        oPart = o.getParticles().getValue();
        oGfx = o.getGraphicsMode().getValue();
        oCloud = o.getCloudRenderMode().getValue();
        oShadow = o.getEntityShadows().getValue();
        oAo = o.getAo().getValue();
        oBlend = o.getBiomeBlendRadius().getValue();
        apply();
    }

    @Override
    public void onTick() {
        // применяем настройки, если слайдеры поменяли, пока модуль включён
        if (MinecraftClient.getInstance().options.getViewDistance().getValue() != (int) dist.value) apply();
    }

    private void apply() {
        GameOptions o = MinecraftClient.getInstance().options;
        o.getViewDistance().setValue((int) dist.value);
        o.getEntityDistanceScaling().setValue(entity.value);
        o.getBiomeBlendRadius().setValue(0);
        if (particles.on()) o.getParticles().setValue(ParticlesMode.MINIMAL);
        if (fast.on()) o.getGraphicsMode().setValue(GraphicsMode.FAST);
        if (clouds.on()) o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        if (shadows.on()) o.getEntityShadows().setValue(false);
        if (ao.on()) o.getAo().setValue(false);
    }

    @Override
    public void onDisable() {
        GameOptions o = MinecraftClient.getInstance().options;
        o.getViewDistance().setValue(oDist);
        o.getEntityDistanceScaling().setValue(oEntity);
        o.getBiomeBlendRadius().setValue(oBlend);
        o.getParticles().setValue(oPart);
        o.getGraphicsMode().setValue(oGfx);
        o.getCloudRenderMode().setValue(oCloud);
        o.getEntityShadows().setValue(oShadow);
        o.getAo().setValue(oAo);
    }
}
