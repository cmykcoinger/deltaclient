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

    private final Setting mode        = add(Setting.mode("Rotation", 0, "FunTime", "SpookyTime", "Rockstar Snap"));
    private final Setting critMode    = add(Setting.mode("Criticals", 0, "Smart", "Only Crits", "No Crits"));
    private final Setting range       = add(Setting.num("Range", 3.2, 2.0, 4.5));
    private final Setting fov         = add(Setting.num("FOV", 360, 30, 360));
    private final Setting speed       = add(Setting.num("Aim speed", 16.0, 3.0, 35.0));
    private final Setting jitter      = add(Setting.num("Micro jitter", 0.55, 0.0, 3.0));
    private final Setting sprintReset = add(Setting.bool("Sprint reset", true));
    private final Setting shieldBreak = add(Setting.bool("Shield break", true));
    private final Setting walls       = add(Setting.bool("Through walls", false));
    private final Setting playersOnly = add(Setting.bool("Players only", true));
    private final Setting priority    = add(Setting.mode("Priority", 0, "Distance", "Health", "FOV"));

    private float rotYaw;
    private float rotPitch;
    private boolean rotActive;
    private float noiseYaw;
    private float noisePitch;
    private long nextNoiseTime;
    private long nextAttackTime;
    private boolean sprintDroppedForAttack;

    public KillAura() {
        super("KillAura", Category.COMBAT);
        key = GLFW.GLFW_KEY_K;
    }

    @Override
    public void onEnable() {
        target = null;
        noSprint = false;
        rotActive = false;
        sprintDroppedForAttack = false;
        var p = MinecraftClient.getInstance().player;
        if (p != null) {
            rotYaw = p.getYaw();
            rotPitch = p.getPitch();
        }
    }

    @Override
    public void onDisable() {
        target = null;
        noSprint = false;
        rotActive = false;
        sprintDroppedForAttack = false;
    }

    private static float[] anglesTo(Vec3d from, Vec3d to) {
        Vec3d d = to.subtract(from);
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        return new float[]{yaw, pitch};
    }

    private Vec3d hitSpot(Vec3d eye, LivingEntity e, float td, int rotType, double tSec) {
        Vec3d base = e.getLerpedPos(td);
        double w = e.getWidth() * 0.32;
        double h = e.getHeight();
        double yRatio = switch (rotType) {
            case 0 -> 0.56 + Math.sin(tSec * 5.2) * 0.14;
            case 1 -> 0.48 + Math.cos(tSec * 7.8) * 0.18;
            default -> 0.62;
        };
        Box box = new Box(base.x - w, base.y + h * 0.14, base.z - w, base.x + w, base.y + h * 0.88, base.z + w);
        double targetY = base.y + h * MathHelper.clamp(yRatio, 0.18, 0.84);
        return new Vec3d(
                MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(targetY, box.minY, box.maxY),
                MathHelper.clamp(eye.z, box.minZ, box.maxZ)
        );
    }

    private void updateRotation(MinecraftClient mc, net.minecraft.client.network.ClientPlayerEntity p, LivingEntity t, float dt, float td) {
        int rotType = (int) Math.round(mode.value);
        double tSec = System.currentTimeMillis() / 1000.0;
        long now = System.currentTimeMillis();

        if (now >= nextNoiseTime) {
            double j = jitter.value * (rotType == 1 ? 1.15 : (rotType == 0 ? 0.85 : 0.45));
            noiseYaw = (float) ThreadLocalRandom.current().nextDouble(-j, j);
            noisePitch = (float) ThreadLocalRandom.current().nextDouble(-j * 0.65, j * 0.65);
            nextNoiseTime = now + ThreadLocalRandom.current().nextLong(45, 115);
        }

        if (!rotActive) {
            rotYaw = p.getYaw();
            rotPitch = p.getPitch();
            rotActive = true;
        }

        Vec3d eye = p.getCameraPosVec(td);
        float[] want = anglesTo(eye, hitSpot(eye, t, td, rotType, tSec));
        float desiredYaw = want[0] + noiseYaw;
        float desiredPitch = MathHelper.clamp(want[1] + noisePitch, -89.5f, 89.5f);

        float dy = MathHelper.wrapDegrees(desiredYaw - rotYaw);
        float dp = desiredPitch - rotPitch;

        double spd = speed.value * (rotType == 2 ? 1.65 : (rotType == 1 ? 1.35 : 1.15));
        float factor = 1f - (float) Math.exp(-spd * Math.max(dt, 0.004f));
        float maxDeg = (float) (spd * 42.0 * Math.max(dt, 0.004f));

        float stepY = MathHelper.clamp(dy * factor, -maxDeg, maxDeg);
        float stepP = MathHelper.clamp(dp * factor, -maxDeg, maxDeg);

        double sens = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double gcd = sens * sens * sens * 1.2;
        if (gcd > 0.0001) {
            stepY -= (float) (stepY % gcd);
            stepP -= (float) (stepP % gcd);
        }

        rotYaw = rotYaw + stepY;
        rotPitch = MathHelper.clamp(rotPitch + stepP, -89.5f, 89.5f);
    }

    private boolean canCritNow(net.minecraft.client.network.ClientPlayerEntity p) {
        if (p.isClimbing() || p.isTouchingWater() || p.isInLava() || p.hasVehicle()
                || p.hasStatusEffect(StatusEffects.BLINDNESS) || p.getAbilities().flying) {
            return true;
        }
        if (p.isOnGround()) return false;
        if (p.getVelocity().y >= 0.02) return false;
        return p.fallDistance > 0.03f || p.getVelocity().y < -0.06;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) return;

        noSprint = false;
        if (mc.currentScreen != null || p.isUsingItem()) {
            target = null;
            rotActive = false;
            sprintDroppedForAttack = false;
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

            int prio = (int) Math.round(priority.value);
            double score = switch (prio) {
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
        if (target == null) {
            rotActive = false;
            sprintDroppedForAttack = false;
            return;
        }

        updateRotation(mc, p, target, 0.05f, 1.0f);

        long now = System.currentTimeMillis();
        if (now < nextAttackTime) return;

        int rotType = (int) Math.round(mode.value);
        float cdNeed = rotType == 1 ? 0.88f : 0.90f;
        if (p.getAttackCooldownProgress(0.5f) < cdNeed) return;

        int cMode = (int) Math.round(critMode.value);
        if (cMode == 1) {
            if (!canCritNow(p)) {
                if (!p.isOnGround() && p.getVelocity().y < 0.05) {
                    noSprint = true;
                    p.setSprinting(false);
                }
                return;
            }
        } else if (cMode == 0) {
            if (!p.isOnGround() && p.getVelocity().y < 0.05 && !canCritNow(p)) {
                return;
            }
        }

        if (shieldBreak.on() && target.isBlocking() && target.getActiveItem().getItem() instanceof ShieldItem) {
            tryBreakShield(mc, p);
            return;
        }

        if (sprintReset.on() && p.isSprinting()) {
            noSprint = true;
            p.setSprinting(false);
            if (!sprintDroppedForAttack && p.isOnGround()) {
                sprintDroppedForAttack = true;
                return;
            }
        }
        sprintDroppedForAttack = false;

        float savedYaw = p.getYaw();
        float savedPitch = p.getPitch();
        p.setYaw(rotYaw);
        p.setPitch(rotPitch);
        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        p.setYaw(savedYaw);
        p.setPitch(savedPitch);

        long minMs = rotType == 1 ? 18 : (rotType == 0 ? 22 : 12);
        long maxMs = rotType == 1 ? 45 : (rotType == 0 ? 52 : 28);
        nextAttackTime = now + ThreadLocalRandom.current().nextLong(minMs, maxMs);
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
        float savedYaw = p.getYaw();
        float savedPitch = p.getPitch();
        p.setYaw(rotYaw);
        p.setPitch(rotPitch);
        if (axeSlot != -1 && axeSlot != prev) {
            p.getInventory().selectedSlot = axeSlot;
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
            p.getInventory().selectedSlot = prev;
        } else {
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }
        p.setYaw(savedYaw);
        p.setPitch(savedPitch);
        nextAttackTime = System.currentTimeMillis() + 55;
    }

    @Override
    public void onFrame(float dt) {
        MinecraftClient mc = MinecraftClient.getInstance();
        LivingEntity t = target;
        if (t == null || !t.isAlive() || mc.currentScreen != null) {
            rotActive = false;
            return;
        }
        var p = mc.player;
        if (p == null) return;

        float td = mc.getRenderTickCounter().getTickDelta(false);
        updateRotation(mc, p, t, dt, td);
    }
}
