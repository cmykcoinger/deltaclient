package client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Кольцо со шлейфом и летающие призраки вокруг цели. Не пропадает сразу при отведении прицела. */
public class TargetESP extends Module {
    private final Setting ring   = add(Setting.bool("Ring", true));
    private final Setting ghosts = add(Setting.bool("Ghosts", true));
    private final Setting hold   = add(Setting.num("Hold (sec)", 4, 0.5, 10));
    private final Setting trailS = add(Setting.num("Trail", 10, 4, 20));

    public TargetESP() { super("TargetESP", Category.VISUALS); enabled = true; }

    @Override
    public void onRender3D(Matrix4f m, float delta) {
        LivingEntity t = TargetInfo.target;
        if (t == null) return;
        long since = System.currentTimeMillis() - TargetInfo.lastSeen;
        float holdMs = (float) (hold.value * 1000);
        float a = since < holdMs ? 1f : Math.max(0f, 1f - (since - holdMs) / 700f);
        if (a <= 0.01f) return;

        double x = MathHelper.lerp(delta, t.prevX, t.getX());
        double y = MathHelper.lerp(delta, t.prevY, t.getY());
        double z = MathHelper.lerp(delta, t.prevZ, t.getZ());
        double h = t.getHeight(), r = t.getWidth() * 0.85;
        double time = System.currentTimeMillis() / 1000.0;
        int n = (int) trailS.value;
        boolean hurt = t.hurtTime > 0;

        RenderUtil.begin3D();
        if (ring.on()) {
            for (int k = 0; k < n; k++) {
                double yy = y + (Math.sin(time * 1.6 - k * 0.1) + 1) / 2 * h;
                final float al = a * (1f - k / (float) n);
                final int kk = k;
                List<Vec3d> pts = new ArrayList<>();
                for (int i = 0; i <= 40; i++) {
                    double ang = i * Math.PI * 2 / 40;
                    pts.add(new Vec3d(x + Math.cos(ang) * r, yy, z + Math.sin(ang) * r));
                }
                RenderUtil.stripColored(m, pts, i -> RenderUtil.alpha(hurt ? 0xFF3030 : RenderUtil.accent(kk * 0.02f + i * 0.01f), al));
            }
        }
        if (ghosts.on()) {
            for (int g = 0; g < 3; g++) {
                List<Vec3d> pts = new ArrayList<>();
                for (int k = 0; k < n + 6; k++) {
                    double ph = time * 1.9 + g * 2.094 - k * 0.09;
                    double rr = r + 0.3 + Math.sin(ph * 2 + g) * 0.1;
                    pts.add(new Vec3d(x + Math.cos(ph) * rr,
                            y + h * 0.55 + Math.sin(ph * 1.5 + g * 1.7) * h * 0.45,
                            z + Math.sin(ph) * rr));
                }
                final int gg = g, len = pts.size();
                final float aa = a;
                RenderUtil.stripColored(m, pts, i -> RenderUtil.alpha(hurt ? 0xFF5050 : RenderUtil.accent(gg * 0.25f), aa * (1f - i / (float) len)));
                // яркая искра на «голове» призрака
                Vec3d p0 = pts.get(0);
                double s = 0.07;
                List<Vec3d> c1 = List.of(p0.add(-s, 0, 0), p0.add(s, 0, 0));
                List<Vec3d> c2 = List.of(p0.add(0, -s, 0), p0.add(0, s, 0));
                List<Vec3d> c3 = List.of(p0.add(0, 0, -s), p0.add(0, 0, s));
                int white = RenderUtil.alpha(0xFFFFFF, aa);
                RenderUtil.strip(m, c1, white);
                RenderUtil.strip(m, c2, white);
                RenderUtil.strip(m, c3, white);
            }
        }
        RenderUtil.end3D();
    }
}
