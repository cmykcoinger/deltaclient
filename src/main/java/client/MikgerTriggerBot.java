package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public class MikgerTriggerBot extends Module {
    private final Setting range = add(Setting.num("Range", 3.2, 2.0, 4.5));
    private final Setting cpsMin = add(Setting.num("CPS min", 8, 4, 20));
    private final Setting cpsMax = add(Setting.num("CPS max", 12, 5, 20));
    private final Setting crits = add(Setting.bool("Smart crits", true));
    private final Setting playersOnly = add(Setting.bool("Players only", true));
    private final Setting reaction = add(Setting.num("Reaction ms", 120, 0, 400));

    private long nextAttackTime, aimStartTime;
    private LivingEntity lastTarget;
    private int jumpCooldown = 0;

    public MikgerTriggerBot() {
        super("MikgerTriggerBot", Category.COMBAT);
        key = GLFW.GLFW_KEY_J;
    }

    private boolean canCritNow(net.minecraft.client.network.ClientPlayerEntity p) {
        if (p.isClimbing() || p.isTouchingWater() || p.isInLava() || p.hasVehicle()
                || p.hasStatusEffect(StatusEffects.BLINDNESS) || p.getAbilities().flying) return true;
        if (p.isOnGround()) return false;
        if (p.getVelocity().y >= 0.02) return false;
        return p.fallDistance > 0.03f || p.getVelocity().y < -0.06;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null || p.isUsingItem()) return;

        if (jumpCooldown > 0) jumpCooldown--;

        LivingEntity target = null;
        double bestDist = Double.MAX_VALUE;
        Vec3d eye = p.getEyePos();

        for (var e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == p || !le.isAlive() || le.isRemoved() || le instanceof ArmorStandEntity) continue;
            if (playersOnly.on() && !(le instanceof PlayerEntity)) continue;
            if (le instanceof PlayerEntity pe && pe.isCreative()) continue;
            if (p.distanceTo(le) > range.value) continue;
            if (!p.canSee(le)) continue;
            
            Vec3d look = eye.add(p.getRotationVec(1.0f).multiply(p.distanceTo(le)));
            if (le.getBoundingBox().expand(0.2).contains(look)) {
                double d = p.distanceTo(le);
                if (d < bestDist) { bestDist = d; target = le; }
            }
        }

        if (target != lastTarget) { lastTarget = target; aimStartTime = System.currentTimeMillis(); }
        if (target == null) return;

        if (crits.on()) {
            if (p.isOnGround() && jumpCooldown == 0) {
                p.jump();
                jumpCooldown = 12;
                return; 
            }
            if (!canCritNow(p)) return;
        }

        long now = System.currentTimeMillis();
        if (now - aimStartTime < reaction.value) return;
        if (p.getAttackCooldownProgress(0.5f) < 0.90f) return;
        if (now < nextAttackTime) return;

        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);

        int lo = (int) Math.round(cpsMin.value), hi = (int) Math.round(cpsMax.value);
        int cps = ThreadLocalRandom.current().nextInt(lo, hi + 1);
        nextAttackTime = now + 1000L / cps + ThreadLocalRandom.current().nextLong(-20, 20);
    }
}
