package client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class TargetESP extends Module {
    private final Setting styleMode = add(Setting.mode("Crystal style", 0, "Crystals + Halo", "Crystal Halo", "Floating Gems"));
    private final Setting hold      = add(Setting.num("Hold (sec)", 3.5, 0.5, 10.0));
    private final Setting gemsCount = add(Setting.num("Gems", 4, 2, 8));
    private final Setting radius    = add(Setting.num("Radius scale", 1.0, 0.6, 1.6));

    public TargetESP() {
        super("TargetESP", Category.VISUALS);
        enabled = true;
    }

    private static void drawCrystal(Matrix4f m, Vec3d c, double rx, double ry, double rz, int col) {
        Vec3d top = c.add(0, ry, 0);
        Vec3d bot = c.add(0, -ry, 0);
        Vec3d n = c.add(0, 0, -rz);
        Vec3d s = c.add(0, 0, rz);
        Vec3d e = c.add(rx, 0, 0);
        Vec3d w = c.add(-rx, 0, 0);

        RenderUtil.strip(m, List.of(n, e, s, w, n), col);
        RenderUtil.strip(m, List.of(top, n, bot, s, top), col);
        RenderUtil.strip(m, List.of(top, e, bot, w, top), col);
    }

    @Override
    public void onRender3D(Matrix4f m, float delta) {
        LivingEntity t = TargetInfo.target;
        if (t == null) return;

        long since = System.currentTimeMillis() - TargetInfo.lastSeen;
        float holdMs = (float) (hold.value * 1000.0);
        float a = since < holdMs ? 1f : Math.max(0f, 1f - (since - holdMs) / 600f);
        if (a <= 0.01f) return;

        double x = MathHelper.lerp(delta, t.prevX, t.getX());
        double y = MathHelper.lerp(delta, t.prevY, t.getY());
        double z = MathHelper.lerp(delta, t.prevZ, t.getZ());
        double h = t.getHeight();
        double baseR = t.getWidth() * 0.9 * radius.value;
        double time = System.currentTimeMillis() / 1000.0;
        boolean hurt = t.hurtTime > 0;
        int st = (int) Math.round(styleMode.value);
        int count = Math.max(2, (int) Math.round(gemsCount.value));

        RenderUtil.begin3D();

        if (st == 0 || st == 1) {
            double haloR = baseR * (1.0 + Math.sin(time * 3.5) * 0.05);
            double haloY = y + h * 0.55 + Math.cos(time * 2.4) * (h * 0.22);
            List<Vec3d> haloPts = new ArrayList<>();
            for (int i = 0; i <= 48; i++) {
                double ang = i * Math.PI * 2.0 / 48.0 + time * 1.4;
                haloPts.add(new Vec3d(x + Math.cos(ang) * haloR, haloY, z + Math.sin(ang) * haloR));
            }
            RenderUtil.stripColored(m, haloPts, i -> {
                int col = hurt ? 0xFF3B52 : RenderUtil.accent(i * 0.025f);
                return RenderUtil.alpha(col, a * 0.85f);
            });
        }

        if (st == 0 || st == 1 || st == 2) {
            for (int g = 0; g < count; g++) {
                double phase = time * 2.1 + g * (Math.PI * 2.0 / count);
                double orbitR = baseR + 0.34 + Math.sin(phase * 1.5) * 0.08;
                double gemY = y + h * 0.52 + Math.sin(phase * 1.3 + g) * (h * 0.38);
                Vec3d center = new Vec3d(x + Math.cos(phase) * orbitR, gemY, z + Math.sin(phase) * orbitR);

                int gemCol = RenderUtil.alpha(hurt ? 0xFF5A70 : RenderUtil.accent(g * 0.22f), a * 0.95f);
                drawCrystal(m, center, 0.085, 0.145, 0.085, gemCol);

                List<Vec3d> tail = new ArrayList<>();
                for (int k = 0; k <= 8; k++) {
                    double tp = phase - k * 0.065;
                    double tr = baseR + 0.34 + Math.sin(tp * 1.5) * 0.08;
                    double ty = y + h * 0.52 + Math.sin(tp * 1.3 + g) * (h * 0.38);
                    tail.add(new Vec3d(x + Math.cos(tp) * tr, ty, z + Math.sin(tp) * tr));
                }
                final int gg = g;
                RenderUtil.stripColored(m, tail, idx -> {
                    float fade = 1f - idx / 8f;
                    int col = hurt ? 0xFF3B52 : RenderUtil.accent(gg * 0.22f + idx * 0.02f);
                    return RenderUtil.alpha(col, a * fade * fade * 0.65f);
                });
            }
        }

        RenderUtil.end3D();
    }
}
