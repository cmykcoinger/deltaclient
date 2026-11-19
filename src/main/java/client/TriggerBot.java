package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
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

/** Бьёт, когда цель в прицеле. Умные криты: не бьёт с земли, если ты прыгаешь, и ждёт падения. */
public class TriggerBot extends Module {
    public static LivingEntity target;
    public static boolean noSprint;

    private final Setting reach      = add(Setting.num("Reach (blocks)", 3.0, 1, 6));
    private final Setting cooldown   = add(Setting.num("Cooldown", 0.95, 0.5, 1.0));
    private final Setting react      = add(Setting.num("Reaction (ticks)", 3, 0, 10));
    private final Setting crits      = add(Setting.bool("Smart crits", true));
    private final Setting strict     = add(Setting.bool("Only crits", false));
    private final Setting fallMin    = add(Setting.num("Min fall", 0.12, 0, 1.0));
    private final Setting autoJump   = add(Setting.bool("Auto jump", false));
    private final Setting weaponOnly = add(Setting.bool("Only sword/axe", false));
    private final Setting walls      = add(Setting.bool("Through walls", false));
    private final Setting players    = add(Setting.bool("Players", true));
    private final Setting mobs       = add(Setting.bool("Mobs", false));

    private int airTicks, seen;
    private Entity lastTarget;

    public TriggerBot() { super("TriggerBot", Category.COMBAT); key = GLFW.GLFW_KEY_T; }

    @Override public void onDisable() { target = null; noSprint = false; lastTarget = null; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        target = null;
        noSprint = false;
        if (p.isOnGround()) airTicks = 0; else airTicks++;
        if (mc.currentScreen != null || p.isUsingItem()) return;

        ItemStack held = p.getMainHandStack();
        if (weaponOnly.on() && !held.isIn(ItemTags.SWORDS) && !held.isIn(ItemTags.AXES)) return;

        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        Vec3d end = eye.add(look.multiply(reach.value));

        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        Box search = p.getBoundingBox().stretch(look.multiply(reach.value)).expand(1.0);
        for (Entity e : mc.world.getOtherEntities(p, search)) {
            if (!(e instanceof LivingEntity) || !e.isAlive() || e instanceof ArmorStandEntity) continue;
            boolean isPlayer = e instanceof PlayerEntity;
            if (isPlayer && !players.on()) continue;
            if (!isPlayer && !mobs.on()) continue;
            Optional<Vec3d> hit = e.getBoundingBox().expand(e.getTargetingMargin()).raycast(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceTo(hit.get());
            if (!walls.on()) {
                var block = mc.world.raycast(new RaycastContext(eye, hit.get(),
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
                if (block.getType() != HitResult.Type.MISS) continue;
            }
            if (d < bestDist) { bestDist = d; best = e; }
        }
        if (best == null) { lastTarget = null; return; }
        target = (LivingEntity) best;

        // «Реакция»: после наведения на цель ждём несколько тиков, а не бьём мгновенно
        if (best != lastTarget) { lastTarget = best; seen = 0; } else seen++;
        if (seen < (int) react.value) return;

        boolean cdReady = p.getAttackCooldownProgress(0.5f) >= (float) cooldown.value;

        if (crits.on() && !critBlocked(p)) {
            boolean air = !p.isOnGround();

            // Прыжок нажат, но ты ещё на земле: не бьём, ждём подъёма и падения
            if (!air && !autoJump.on() && mc.options.jumpKey.isPressed()) return;

            if (autoJump.on() && !air && !p.isSneaking()) { p.jump(); return; }

            if (air) {
                noSprint = true;
                if (p.isSprinting()) { p.setSprinting(false); return; }   // на спринте крита нет
            }
            boolean canCrit = air && p.fallDistance > fallMin.value;
            if (strict.on() && !canCrit) return;
            if (!strict.on() && air && !canCrit && airTicks < 20) return;   // летим вверх - ждём падения
        }

        if (cdReady) {
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    private static boolean critBlocked(net.minecraft.client.network.ClientPlayerEntity p) {
        return p.isClimbing() || p.isTouchingWater() || p.isInLava() || p.hasVehicle()
                || p.hasStatusEffect(StatusEffects.BLINDNESS) || p.getAbilities().flying;
    }
}
