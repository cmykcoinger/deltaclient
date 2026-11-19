package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public class KillAura extends Module {
    private final Setting range       = add(Setting.num("Range", 3.2, 2.0, 4.5));
    private final Setting rotSpeed    = add(Setting.num("Rot speed", 0.35, 0.05, 1.0));
    private final Setting jitter      = add(Setting.num("Jitter", 1.2, 0.0, 5.0));
    private final Setting crits       = add(Setting.bool("Criticals", true));
    private final Setting rayCheck    = add(Setting.bool("Through walls", false));
    private final Setting playersOnly = add(Setting.bool("Players only", true));
    private final Setting priority    = add(Setting.num("Priority (0=Dist, 1=HP)", 0, 0, 1));

    public static LivingEntity target;

    private float serverYaw;
    private float serverPitch;
    private boolean hasRot;

    public KillAura() {
        super("KillAura", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            serverYaw = mc.player.getYaw();
            serverPitch = mc.player.getPitch();
            hasRot = true;
        }
        target = null;
    }

    @Override
    public void onDisable() {
        target = null;
        hasRot = false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        target = findTarget(mc);
        if (target == null) {
            hasRot = false;
            return;
        }

        float[] desired = calcAnglesTo(
                mc.player.getEyePos(),
                target.getBoundingBox().getCenter().add(0, target.getHeight() * 0.15, 0)
        );
        float jYaw = (float) ThreadLocalRandom.current().nextDouble(-jitter.value, jitter.value);
        float jPitch = (float) ThreadLocalRandom.current().nextDouble(-jitter.value * 0.6, jitter.value * 0.6);

        if (!hasRot) {
            serverYaw = mc.player.getYaw();
            serverPitch = mc.player.getPitch();
            hasRot = true;
        }

        float step = (float) rotSpeed.value;
        float diffYaw = MathHelper.wrapDegrees((desired[0] + jYaw) - serverYaw);
        float diffPitch = MathHelper.wrapDegrees((desired[1] + jPitch) - serverPitch);

        serverYaw += diffYaw * step;
        serverPitch = MathHelper.clamp(serverPitch + diffPitch * step, -90f, 90f);

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                serverYaw, serverPitch, mc.player.isOnGround()
        ));

        float progress = mc.player.getAttackCooldownProgress(0.5f);
        if (progress < 0.92f) return;

        if (crits.on() && !canCritNow(mc)) {
            return;
        }

        float aimError = Math.abs(MathHelper.wrapDegrees(desired[0] - serverYaw));
        if (aimError > 28f) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private LivingEntity findTarget(MinecraftClient mc) {
        double maxDist = range.value;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity le)) continue;
            if (le == mc.player || !le.isAlive() || le.isRemoved()) continue;
            if (le instanceof ArmorStandEntity) continue;
            if (playersOnly.on() && !(le instanceof PlayerEntity)) continue;
            if (le instanceof PlayerEntity pe && pe.isCreative()) continue;

            double dist = mc.player.distanceTo(le);
            if (dist > maxDist) continue;

            if (!rayCheck.on() && !mc.player.canSee(le)) continue;

            double score = (priority.value >= 0.5)
                    ? (le.getHealth() + le.getAbsorptionAmount())
                    : dist;

            if (score < bestScore) {
                bestScore = score;
                best = le;
            }
        }
        return best;
    }

    private boolean canCritNow(MinecraftClient mc) {
        var p = mc.player;
        if (p.isTouchingWater() || p.isInLava() || p.isClimbing() || p.hasVehicle()) {
            return true;
        }
        return !p.isOnGround() && p.fallDistance > 0.0f;
    }

    private float[] calcAnglesTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
        return new float[]{yaw, pitch};
    }
}
