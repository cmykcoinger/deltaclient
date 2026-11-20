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

public class MikgerKillAura extends Module {
    public static LivingEntity target;

    private final Setting mode = add(Setting.mode("Rotation", 0, "FunTime", "SpookyTime", "Snap"));
    private final Setting critMode = add(Setting.mode("Criticals", 0, "Smart", "Only Crits", "No Crits"));
    private final Setting range = add(Setting.num("Range", 3.2, 2.0, 4.5));
    private final Setting fov = add(Setting.num("FOV", 360, 30, 360));
    private final Setting speed = add(Setting.num("Aim speed", 16.0, 3.0, 35.0));
    private final Setting jitter = add(Setting.num("Micro jitter", 0.55, 0.0, 3.0));
    private final Setting sprintReset = add(Setting.bool("Sprint reset", true));
    private final Setting playersOnly = add(Setting.bool("Players only", true));
    private final Setting priority = add(Setting.mode("Priority", 0, "Distance", "Health", "FOV"));

    private float rotYaw, rotPitch;
    private boolean rotActive;
    private float noiseYaw, noisePitch;
    private long nextNoiseTime, nextAttackTime;

    public MikgerKillAura() {
        super("MikgerKillAura", Category.COMBAT);
        key = GLFW.GLFW_KEY_K;
    }

    @Override
    public void onEnable() {
        target = null;
        rotActive = false;
        var p = MinecraftClient.getInstance().player;
        if (p != null) { rotYaw = p.getYaw(); rotPitch = p.getPitch(); }
    }

    @Override
    public void onDisable() { target = null; rotActive = false; }

    private static float[] anglesTo(Vec3d from, Vec3d to) {
        Vec3d d = to.subtract(from);
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        return new float[]{yaw, pitch};
    }

    private void updateRotation(MinecraftClient mc, net.minecraft.client.network.ClientPlayerEntity p, LivingEntity t, float dt) {
        int rotType = (int) Math.round(mode.value);
        long now = System.currentTimeMillis();

        if (now >= nextNoiseTime) {
            double j = jitter.value * (rotType == 1 ? 1.15 : (rotType == 0 ? 0.85 : 0.45));
            noiseYaw = (float) ThreadLocalRandom.current().nextDouble(-j, j);
            noisePitch = (float) ThreadLocalRandom.current().nextDouble(-j * 0.65, j * 0.65);
            nextNoiseTime = now + ThreadLocalRandom.current().nextLong(45, 115);
        }

        if (!rotActive) { rotYaw = p.getYaw(); rotPitch = p.getPitch(); rotActive = true; }

        Vec3d eye = p.getCameraPosVec(1.0f);
        float[] want = anglesTo(eye, t.getBoundingBox().getCenter());
        float desiredYaw = want[0] + noiseYaw;
        float desiredPitch = MathHelper.clamp(want[1] + noisePitch, -89.5f, 89.5f);

        float dy = MathHelper.wrapDegrees(desiredYaw - rotYaw);
        float dp = desiredPitch - rotPitch;

        double spd = speed.value * (rotType == 2 ? 1.65 : (rotType == 1 ? 1.35 : 1.15));
        float factor = 1f - (float) Math.exp(-spd * Math.max(dt, 0.004f));
        float maxDeg = (float) (spd * 42.0 * Math.max(dt, 0.004f));

        float stepY = MathHelper.clamp(dy * factor, -maxDeg, maxDeg);
        float stepP = MathHelper.clamp(dp * factor, -maxDeg, maxDeg);

        rotYaw += stepY;
        rotPitch = MathHelper.clamp(rotPitch + stepP, -89.5f, 89.5f);
        p.setYaw(rotYaw);
        p.setPitch(rotPitch);
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
        if (mc.currentScreen != null || p.isUsingItem()) { target = null; rotActive = false; return; }

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        Vec3d eye = p.getEyePos();

        for (var e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == p || !le.isAlive() || le.isRemoved() || le instanceof ArmorStandEntity) continue;
            if (playersOnly.on() && !(le instanceof PlayerEntity)) continue;
            if (le instanceof PlayerEntity pe && pe.isCreative()) continue;
            double dist = p.distanceTo(le);
            if (dist > range.value) continue;
            if (!p.canSee(le)) continue;
            float[] ang = anglesTo(eye, le.getBoundingBox().getCenter());
            double fovDiff = Math.hypot(MathHelper.wrapDegrees(ang[0] - p.getYaw()), ang[1] - p.getPitch());
            if (fovDiff > fov.value / 2.0) continue;
            int prio = (int) Math.round(priority.value);
            double score = switch (prio) {
                case 1 -> le.getHealth() + le.getAbsorptionAmount();
                case 2 -> fovDiff;
                default -> dist;
            };
            if (score < bestScore) { bestScore = score; best = le; }
        }

        target = best;
        if (target == null) { rotActive = false; return; }

        long now = System.currentTimeMillis();
        if (now < nextAttackTime) return;
        if (p.getAttackCooldownProgress(0.5f) < 0.90f) return;

        int cMode = (int) Math.round(critMode.value);
        if (cMode == 1 && !canCritNow(p)) return;

        float savedYaw = p.getYaw(), savedPitch = p.getPitch();
        p.setYaw(rotYaw); p.setPitch(rotPitch);
        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        p.setYaw(savedYaw); p.setPitch(savedPitch);

        int rotType = (int) Math.round(mode.value);
        long minMs = rotType == 1 ? 18 : (rotType == 0 ? 22 : 12);
        long maxMs = rotType == 1 ? 45 : (rotType == 0 ? 52 : 28);
        nextAttackTime = now + ThreadLocalRandom.current().nextLong(minMs, maxMs);
    }

    @Override
    public void onFrame(float dt) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (target == null || !target.isAlive() || p == null || mc.currentScreen != null) { rotActive = false; return; }
        updateRotation(mc, p, target, dt);
    }
}
