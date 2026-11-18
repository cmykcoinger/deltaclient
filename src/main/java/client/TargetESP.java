package client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Кольцо со шлейфом вокруг цели AimAssist / TriggerBot. */
public class TargetESP extends Module {
    private final Setting trail = add(Setting.num("Trail", 8, 1, 16));

    public TargetESP() { super("TargetESP", Category.VISUALS); enabled = true; }

    @Override
    public void onRender3D(Matrix4f m, float delta) {
        LivingEntity t = TriggerBot.target != null ? TriggerBot.target : AimAssist.target;
        if (t == null || !t.isAlive()) return;

        double x = MathHelper.lerp(delta, t.prevX, t.getX());
        double y = MathHelper.lerp(delta, t.prevY, t.getY());
        double z = MathHelper.lerp(delta, t.prevZ, t.getZ());
        double r = t.getWidth() * 0.85;
        double time = System.currentTimeMillis() / 600.0;
        int n = (int) trail.value;

        RenderUtil.begin3D();
        for (int k = 0; k < n; k++) {
            double yy = y + (Math.sin(time - k * 0.13) + 1) / 2 * t.getHeight();
            int base = t.hurtTime > 0 ? 0xFF3030 : RenderUtil.rainbow(k * 0.02f);
            int color = RenderUtil.alpha(base, 1f - k / (float) n);
            List<Vec3d> pts = new ArrayList<>();
            for (int i = 0; i <= 40; i++) {
                double a = i * Math.PI * 2 / 40;
                pts.add(new Vec3d(x + Math.cos(a) * r, yy, z + Math.sin(a) * r));
            }
            RenderUtil.strip(m, pts, color);
        }
        RenderUtil.end3D();
    }
}
