package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/** Плавно подтягивает прицел к цели. Наводка идёт каждый кадр (не раз в тик), поэтому без рывков. */
public class AimAssist extends Module {
    public static LivingEntity target;

    private final Setting range     = add(Setting.num("Range (blocks)", 4.5, 1, 8));
    private final Setting fov       = add(Setting.num("FOV", 60, 5, 180));
    private final Setting strength  = add(Setting.num("Strength %", 60, 5, 100));
    private final Setting smooth    = add(Setting.num("Smoothness", 8, 1, 20));
    private final Setting maxSpeed  = add(Setting.num("Max speed", 12, 1, 60));
    private final Setting aimHeight = add(Setting.num("Aim height", 0.85, 0.3, 1.0));
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
        if (mc.currentScreen != null) { target = null; return; }
        if (onlyClick.on() && !mc.options.attackKey.isPressed()) { target = null; return; }

        double bestAngle = Double.MAX_VALUE;
        LivingEntity best = null;
        float yaw = mc.player.getYaw(), pitch = mc.player.getPitch();
        Vec3d eye = mc.player.getEyePos();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity) || e == mc.player || !e.isAlive() || e instanceof ArmorStandEntity) continue;
            boolean isPlayer = e instanceof PlayerEntity;
            if (isPlayer && !players.on()) continue;
            if (!isPlayer && !mobs.on()) continue;
            if (mc.player.distanceTo(e) > range.value) continue;
            if (!walls.on() && !mc.player.canSee(e)) continue;
            Vec3d d = e.getPos().add(0, e.getHeight() * aimHeight.value, 0).subtract(eye);
            float ty = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
            float tp = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
            double ang = Math.hypot(MathHelper.wrapDegrees(ty - yaw), tp - pitch);
            if (ang > fov.value / 2.0 || ang >= bestAngle) continue;
            bestAngle = ang; best = (LivingEntity) e;
        }
        target = best;
    }

    @Override
    public void onFrame(float dt) {
        MinecraftClient mc = MinecraftClient.getInstance();
        LivingEntity t = target;
        if (t == null || !t.isAlive() || mc.currentScreen != null) return;
        if (onlyClick.on() && !mc.options.attackKey.isPressed()) return;
        var p = mc.player;
        float td = mc.getRenderTickCounter().getTickDelta(false);

        Vec3d aim = t.getLerpedPos(td).add(0, t.getHeight() * aimHeight.value, 0);
        Vec3d d = aim.subtract(p.getCameraPosVec(td));
        float ty = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
        float tp = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        float dy = MathHelper.wrapDegrees(ty - p.getYaw());
        float dp = tp - p.getPitch();
        float ang = (float) Math.hypot(dy, dp);
        if (ang > fov.value / 2.0 || ang < 0.2f) return;

        // экспоненциальное сглаживание по времени кадра: чем больше Smoothness, тем мягче
        float rate = (float) (strength.value / 100.0 * 30.0 / smooth.value);
        float k = 1f - (float) Math.exp(-rate * dt);
        float maxStep = (float) (maxSpeed.value * 20.0 * dt);
        p.setYaw(p.getYaw() + MathHelper.clamp(dy * k, -maxStep, maxStep));
        if (vertical.on())
            p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(dp * k, -maxStep, maxStep), -90f, 90f));
    }
}
