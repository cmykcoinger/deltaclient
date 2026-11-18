package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/** Плавно подтягивает прицел к ближайшей к центру экрана цели в заданном FOV. */
public class AimAssist extends Module {
    public static LivingEntity target;

    private final Setting range     = add(Setting.num("Range (blocks)", 4.5, 1, 8));
    private final Setting fov       = add(Setting.num("FOV", 60, 5, 180));
    private final Setting smooth    = add(Setting.num("Smoothness", 6, 1, 20));
    private final Setting maxSpeed  = add(Setting.num("Max speed", 25, 1, 90));
    private final Setting aimHeight = add(Setting.num("Aim height", 0.9, 0.3, 1.0));
    private final Setting vertical  = add(Setting.bool("Vertical", true));
    private final Setting onlyClick = add(Setting.bool("Only while attacking", true));
    private final Setting walls     = add(Setting.bool("Through walls", false));
    private final Setting players   = add(Setting.bool("Players", true));
    private final Setting mobs      = add(Setting.bool("Mobs", false));

    public AimAssist() { super("AimAssist", Category.COMBAT); key = GLFW.GLFW_KEY_R; }

    @Override public void onDisable() { target = null; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        target = null;
        if (mc.currentScreen != null) return;
        if (onlyClick.on() && !mc.options.attackKey.isPressed()) return;

        double bestAngle = Double.MAX_VALUE;
        float bestDy = 0, bestDp = 0;
        float yaw = mc.player.getYaw(), pitch = mc.player.getPitch();
        Vec3d eye = mc.player.getEyePos();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity) || e == mc.player || !e.isAlive() || e instanceof ArmorStandEntity) continue;
            boolean isPlayer = e instanceof PlayerEntity;
            if (isPlayer && !players.on()) continue;
            if (!isPlayer && !mobs.on()) continue;
            if (mc.player.distanceTo(e) > range.value) continue;
            if (!walls.on() && !mc.player.canSee(e)) continue;

            Vec3d diff = e.getPos().add(0, e.getHeight() * aimHeight.value, 0).subtract(eye);
            float ty = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f);
            float tp = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
            float dy = MathHelper.wrapDegrees(ty - yaw);
            float dp = tp - pitch;
            double ang = Math.hypot(dy, dp);
            if (ang > fov.value / 2.0 || ang >= bestAngle) continue;
            bestAngle = ang; bestDy = dy; bestDp = dp;
            target = (LivingEntity) e;
        }
        if (target == null) return;

        float max = (float) maxSpeed.value, s = (float) smooth.value;
        mc.player.setYaw(yaw + MathHelper.clamp(bestDy / s, -max, max));
        if (vertical.on())
            mc.player.setPitch(MathHelper.clamp(pitch + MathHelper.clamp(bestDp / s, -max, max), -90f, 90f));
    }
}
