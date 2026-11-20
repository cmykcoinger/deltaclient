package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
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
    private final Setting playersOnly = add(Setting.bool("Players only", true));
    private final Setting silentAim = add(Setting.bool("Silent Aim (F5)", true)); 

    private float rotYaw, rotPitch;
    private boolean rotActive;
    private float noiseYaw, noisePitch;
    private long nextNoiseTime, nextAttackTime;
    private int jumpCooldown = 0;

    public MikgerKillAura() {
        super("MikgerKillAura", Category.COMBAT);
        key = GLFW.GLFW_KEY_K;
    }

    @Override
    public void onEnable() { target = null; rotActive = false; }
    @Override
    public void onDisable() { target = null; rotActive = false; }

    private static float[] anglesTo(Vec3d from, Vec3d to) {
        Vec3d d = to.subtract(from);
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        return new float[]{yaw, pitch};
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

        if (jumpCooldown > 0) jumpCooldown--;
        if (critMode.value == 1 || critMode.value == 0) { 
            if (p.isOnGround() && target != null && p.distanceTo(target) <= range.value && jumpCooldown == 0) {
                p.jump(); 
                jumpCooldown = 10;
                return; 
            }
        }

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
            double score = dist; 
            if (score < bestScore) { bestScore = score; best = le; }
        }

        target = best;
        if (target == null) { rotActive = false; return; }

        long now = System.currentTimeMillis();
        if (now < nextAttackTime) return;
        if (p.getAttackCooldownProgress(0.5f) < 0.90f) return;

        int cMode = (int) Math.round(critMode.value);
        if (cMode == 1 && !canCritNow(p)) return;

        float[] want = anglesTo(eye, target.getBoundingBox().getCenter());
        
        if (silentAim.on()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(want[0], want[1], p.isOnGround()));
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(p.getYaw(), p.getPitch(), p.isOnGround()));
        } else {
            float savedYaw = p.getYaw(), savedPitch = p.getPitch();
            p.setYaw(want[0]); p.setPitch(want[1]);
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
            p.setYaw(savedYaw); p.setPitch(savedPitch);
        }

        int rotType = (int) Math.round(mode.value);
        long minMs = rotType == 1 ? 18 : (rotType == 0 ? 22 : 12);
        long maxMs = rotType == 1 ? 45 : (rotType == 0 ? 52 : 28);
        nextAttackTime = now + ThreadLocalRandom.current().nextLong(minMs, maxMs);
    }

    @Override
    public void onFrame(float dt) { }
}
