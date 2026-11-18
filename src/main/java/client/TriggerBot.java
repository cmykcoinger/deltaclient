package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

/** Бьёт, когда в прицеле цель на расстоянии не больше Reach (в блоках). */
public class TriggerBot extends Module {
    public static LivingEntity target;

    private final Setting reach      = add(Setting.num("Reach (blocks)", 3.0, 1, 6));
    private final Setting cooldown   = add(Setting.num("Cooldown", 1.0, 0.3, 1.0));
    private final Setting weaponOnly = add(Setting.bool("Only sword/axe", false));
    private final Setting walls      = add(Setting.bool("Through walls", false));
    private final Setting players    = add(Setting.bool("Players", true));
    private final Setting mobs       = add(Setting.bool("Mobs", false));

    public TriggerBot() { super("TriggerBot", Category.COMBAT); key = GLFW.GLFW_KEY_T; }

    @Override public void onDisable() { target = null; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        target = null;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        ItemStack held = mc.player.getMainHandStack();
        if (weaponOnly.on() && !held.isIn(ItemTags.SWORDS) && !held.isIn(ItemTags.AXES)) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1f);
        Vec3d end = eye.add(look.multiply(reach.value));

        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        Box search = mc.player.getBoundingBox().stretch(look.multiply(reach.value)).expand(1.0);
        for (Entity e : mc.world.getOtherEntities(mc.player, search)) {
            if (!(e instanceof LivingEntity) || !e.isAlive() || e instanceof ArmorStandEntity) continue;
            boolean isPlayer = e instanceof PlayerEntity;
            if (isPlayer && !players.on()) continue;
            if (!isPlayer && !mobs.on()) continue;

            Box box = e.getBoundingBox().expand(e.getTargetingMargin());
            Optional<Vec3d> hit = box.raycast(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceTo(hit.get());

            if (!walls.on()) {
                var block = mc.world.raycast(new RaycastContext(eye, hit.get(),
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                if (block.getType() != HitResult.Type.MISS) continue;
            }
            if (d < bestDist) { bestDist = d; best = e; }
        }
        if (best == null) return;
        target = (LivingEntity) best;

        if (mc.player.getAttackCooldownProgress(0.5f) >= (float) cooldown.value) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
