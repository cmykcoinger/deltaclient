package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/** Делает невидимых существ видимыми. */
public class SeeInvisibles extends Module {
    private final Set<Entity> changed = new HashSet<>();

    public SeeInvisibles() { super("SeeInvisibles", Category.VISUALS); }

    @Override
    public void onTick() {
        var mc = MinecraftClient.getInstance();
        changed.removeIf(Entity::isRemoved);
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity && e != mc.player && e.isInvisible()) {
                e.setInvisible(false);
                changed.add(e);
            }
        }
    }

    @Override
    public void onDisable() {
        for (Entity e : changed) if (!e.isRemoved()) e.setInvisible(true);
        changed.clear();
    }
}
