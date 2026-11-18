package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Траектория предмета в руке (лук, арбалет, перл, снежок, яйцо, зелья) + уже летящие снаряды. */
public class Trajectories extends Module {
    private final Setting others = add(Setting.bool("Flying projectiles", true));

    public Trajectories() { super("Trajectories", Category.VISUALS); enabled = true; }

    @Override
    public void onRender3D(Matrix4f m, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        RenderUtil.begin3D();

        for (Hand h : Hand.values()) {
            ItemStack st = mc.player.getStackInHand(h);
            Item it = st.getItem();
            double speed, grav;
            if (it instanceof BowItem && mc.player.isUsingItem() && mc.player.getActiveHand() == h) {
                int charge = st.getMaxUseTime(mc.player) - mc.player.getItemUseTimeLeft();
                float f = BowItem.getPullProgress(charge);
                if (f < 0.1f) continue;
                speed = f * 3.0; grav = 0.05;
            } else if (it instanceof CrossbowItem && CrossbowItem.isCharged(st)) { speed = 3.15; grav = 0.05; }
            else if (it instanceof EnderPearlItem || it instanceof SnowballItem || it instanceof EggItem) { speed = 1.5; grav = 0.03; }
            else if (it instanceof SplashPotionItem || it instanceof LingeringPotionItem) { speed = 0.5; grav = 0.05; }
            else continue;

            double yaw = Math.toRadians(mc.player.getYaw());
            Vec3d start = mc.player.getEyePos().add(-Math.cos(yaw) * 0.16, -0.1, -Math.sin(yaw) * 0.16);
            Vec3d vel = mc.player.getRotationVec(delta).multiply(speed);
            draw(m, simulate(start, vel, grav), RenderUtil.rainbow(0f));
            break;
        }

        if (others.on()) {
            for (Entity e : mc.world.getEntities()) {
                double grav;
                if (e instanceof PotionEntity) grav = 0.05;
                else if (e instanceof ThrownItemEntity) grav = 0.03;
                else if (e instanceof PersistentProjectileEntity) grav = 0.05;
                else continue;
                if (e.getVelocity().lengthSquared() < 0.01) continue;
                int col = e instanceof ThrownItemEntity ? 0xFFB040FF : 0xFFFFC040;
                draw(m, simulate(e.getPos(), e.getVelocity(), grav), col);
            }
        }
        RenderUtil.end3D();
    }

    private List<Vec3d> simulate(Vec3d pos, Vec3d vel, double grav) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Vec3d> pts = new ArrayList<>();
        pts.add(pos);
        for (int i = 0; i < 300; i++) {
            Vec3d next = pos.add(vel);
            var r = mc.world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, mc.player));
            if (r.getType() != HitResult.Type.MISS) { pts.add(r.getPos()); break; }
            pts.add(next);
            pos = next;
            vel = vel.multiply(0.99).subtract(0, grav, 0);
            if (pos.y < mc.world.getBottomY()) break;
        }
        return pts;
    }

    private void draw(Matrix4f m, List<Vec3d> pts, int color) {
        RenderUtil.strip(m, pts, color);
        Vec3d end = pts.get(pts.size() - 1);
        List<Vec3d> ring = new ArrayList<>();
        for (int i = 0; i <= 24; i++) {
            double a = i * Math.PI * 2 / 24;
            ring.add(end.add(Math.cos(a) * 0.3, 0.02, Math.sin(a) * 0.3));
        }
        RenderUtil.strip(m, ring, color);
    }
}
