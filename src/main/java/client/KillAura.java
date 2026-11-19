package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public class KillAura extends Module {
    public static LivingEntity target;
    public static boolean noSprint;

    private final Setting range       = add(Setting.num("Range", 3.1, 2.0, 4.5));
    private final Setting fov         = add(Setting.num("FOV", 120, 30, 180));
    private final Setting smooth      = add(Setting.num("Smoothness", 6.5, 1.5, 20.0));
    private final Setting maxSpeed    = add(Setting.num("Max speed", 28, 5, 90));
    private final Setting jitter      = add(Setting.num("Human jitter", 1.1, 0.0, 4.0));
    private final Setting crits       = add(Setting.bool("Smart crits", true));
    private final Setting onlyCrits   = add(Setting.bool("Only crits", false));
    private final Setting sprintReset = add(Setting.bool("Sprint reset", true));
    private final Setting shieldBreak = add(Setting.bool("Shield break", true));
    private final Setting walls       = add(Setting.bool("Through walls", false));
    private final Setting playersOnly = add(Setting.bool("Players only", true));
    private final Setting priority    = add(Setting.mode("Priority", 0, "Distance", "Health", "FOV"));

    private int airTicks;
    private float noiseYaw;
    private float noisePitch;
    private long nextNoiseTime;
    private long nextAttackAllowed;

    public KillAura() {
        super("KillAura", Category.COMBAT);
        key = GLFW.GLFW_KEY_K;
    }

    @Override
    public void onEnable() {
        target = null;
        noSprint = false;
        noiseYaw = 0f;
        noisePitch = 0f;
    }

    @Override
    public void onDisable() {
        target = null;
        noSprint = false;
    }

    private static float[] anglesTo(Vec3d from, Vec3d to) {
        Vec3d d = to.subtract(from);
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        return new float[]{yaw, pitch};
    }

    private Vec3d bestHitPoint(Vec3d eye, LivingEntity e, float td) {
        Vec3d base = e.getLerpedPos(td);
        double w = e.getWidth() * 0.35;
        double h = e.getHeight();
        Box box = new Box(base.x - w, base.y + h * 0.15, base.z - w, base.x + w, base.y + h * 0.85, base.z + w);
        return new Vec3d(
                MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(eye.y, box.minY, box.maxY),
                MathHelper.clamp(eye.z, box.minZ, box.maxZ)
        );
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) return;

        noSprint = false;
        if (p.isOnGround()) airTicks = 0; else airTicks++;

        if (mc.currentScreen != null || p.isUsingItem()) {
            target = null;
            return;
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
            if (!walls.on() && !p.canSee(le)) continue;

            float[] ang = anglesTo(eye, le.getBoundingBox().getCenter());
            double fovDiff = Math.hypot(MathHelper.wrapDegrees(ang[0] - p.getYaw()), ang[1] - p.getPitch());
            if (fovDiff > fov.value / 2.0) continue;

            int mode = (int) Math.round(priority.value);
            double score = switch (mode) {
                case 1 -> le.getHealth() + le.getAbsorptionAmount();
                case 2 -> fovDiff;
                default -> dist;
            };
            if (score < bestScore) {
                bestScore = score;
                best = le;
            }
        }

        target = best;
        if (target == null) return;

        long now = System.currentTimeMillis();
        if (now >= nextNoiseTime) {
            double j = jitter.value;
            noiseYaw = (float) ThreadLocalRandom.current().nextDouble(-j, j);
            noisePitch = (float) ThreadLocalRandom.current().nextDouble(-j * 0.65, j * 0.65);
            nextNoiseTime = now + ThreadLocalRandom.current().nextLong(70, 160);
        }

        float[] want = anglesTo(eye, bestHitPoint(eye, target, 1.0f));
        float err = (float) Math.hypot(MathHelper.wrapDegrees(want[0] - p.getYaw()), want[1] - p.getPitch());
        if (err > 14f) return;

        if (now < nextAttackAllowed) return;
        if (p.getAttackCooldownProgress(0.5f) < 0.93f) return;

        if (crits.on() && !critBlocked(p)) {
            boolean air = !p.isOnGround();
            if (!air && mc.options.jumpKey.isPressed()) return;
            if (air) {
                if (sprintReset.on()) {
                    noSprint = true;
                    if (p.isSprinting()) {
                        p.setSprinting(false);
                        return;
                    }
                }
                boolean falling = p.fallDistance > 0.11f;
                if (onlyCrits.on() && !falling) return;
                if (!falling && airTicks < 20) return;
            } else if (onlyCrits.on()) {
                return;
            }
        }

        if (shieldBreak.on() && target.isBlocking() && target.getActiveItem().getItem() instanceof ShieldItem) {
            tryBreakShield(mc, p);
            return;
        }

        if (sprintReset.on() && p.isSprinting() && p.isOnGround()) {
            p.setSprinting(false);
        }

        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        nextAttackAllowed = now + ThreadLocalRandom.current().nextLong(12, 38);
    }

    private void tryBreakShield(MinecraftClient mc, net.minecraft.client.network.ClientPlayerEntity p) {
        int prev = p.getInventory().selectedSlot;
        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            var st = p.getInventory().getStack(i);
            if (!st.isEmpty() && (st.getItem() instanceof AxeItem || st.getName().getString().toLowerCase().contains("топор"))) {
                axeSlot = i;
                break;
            }
        }
        if (axeSlot != -1 && axeSlot != prev) {
            p.getInventory().selectedSlot = axeSlot;
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
            p.getInventory().selectedSlot = prev;
        } else {
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }
        nextAttackAllowed = System.currentTimeMillis() + 60;
    }

    @Override
    public void onFrame(float dt) {
        MinecraftClient mc = MinecraftClient.getInstance();
        LivingEntity t = target;
        if (t == null || !t.isAlive() || mc.currentScreen != null) return;
        var p = mc.player;
        if (p == null) return;

        float td = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d aim = bestHitPoint(p.getCameraPosVec(td), t, td);
        float[] want = anglesTo(p.getCameraPosVec(td), aim);

        float dy = MathHelper.wrapDegrees((want[0] + noiseYaw) - p.getYaw());
        float dp = MathHelper.clamp(want[1] + noisePitch, -89.5f, 89.5f) - p.getPitch();
        if (Math.hypot(dy, dp) < 0.15) return;

        float k = 1f - (float) Math.exp(-(32.0 / smooth.value) * dt);
        float maxStep = (float) (maxSpeed.value * 22.0 * dt);

        double sens = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double gcd = sens * sens * sens * 1.2;

        float stepY = MathHelper.clamp(dy * k, -maxStep, maxStep);
        float stepP = MathHelper.clamp(dp * k, -maxStep, maxStep);
        if (gcd > 0.0001) {
            stepY -= (float) (stepY % gcd);
            stepP -= (float) (stepP % gcd);
        }

        p.setYaw(p.getYaw() + stepY);
        p.setPitch(MathHelper.clamp(p.getPitch() + stepP, -90f, 90f));
    }

    private static boolean critBlocked(net.minecraft.client.network.ClientPlayerEntity p) {
        return p.isClimbing() || p.isTouchingWater() || p.isInLava() || p.hasVehicle()
                || p.hasStatusEffect(StatusEffects.BLINDNESS) || p.getAbilities().flying;
    }
}
