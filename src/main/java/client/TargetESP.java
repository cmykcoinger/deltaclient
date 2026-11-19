package client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class TargetESP extends Module {
    private final Setting mode   = add(Setting.mode("Style", 0, "Ghosts + Helix", "Dual Helix", "Ghosts Only"));
    private final Setting hold   = add(Setting.num("Hold (sec)", 3.5, 0.5, 10.0));
    private final Setting trailS = add(Setting.num("Trail length", 16, 6, 28));
    private final Setting radius = add(Setting.num("Radius scale", 1.0, 0.6, 1.6));

    public TargetESP() {
        super("TargetESP", Category.VISUALS);
        enabled = true;
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
        double baseR = t.getWidth() * 0.85 * radius.value;
        double time = System.currentTimeMillis() / 1000.0;
        int n = (int) trailS.value;
        boolean hurt = t.hurtTime > 0;
        int style = (int) Math.round(mode.value);

        RenderUtil.begin3D();

        double pulse = 1.0 + Math.sin(time * 4.0) * 0.07;
        for (int ringIdx = 0; ringIdx < 2; ringIdx++) {
            double rr = baseR * (ringIdx == 0 ? pulse : pulse * 0.72);
            double ry = y + 0.03 + ringIdx * 0.02;
            float ringAlpha = a * (ringIdx == 0 ? 0.9f : 0.45f);
            List<Vec3d> ringPts = new ArrayList<>();
            for (int i = 0; i <= 48; i++) {
                double ang = i * Math.PI * 2.0 / 48.0 + (ringIdx == 0 ? time : -time);
                ringPts.add(new Vec3d(x + Math.cos(ang) * rr, ry, z + Math.sin(ang) * rr));
            }
            RenderUtil.stripColored(m, ringPts, i -> {
                int col = hurt ? 0xFF3545 : RenderUtil.accent(i * 0.025f);
                return RenderUtil.alpha(col, ringAlpha);
            });
        }

        if (style == 0 || style == 1) {
            for (int strand = 0; strand < 2; strand++) {
                List<Vec3d> helix = new ArrayList<>();
                int segs = 32;
                for (int i = 0; i <= segs; i++) {
                    double frac = i / (double) segs;
                    double ang = frac * Math.PI * 2.5 + time * 3.0 + strand * Math.PI;
                    double waveY = y + ((frac + time * 0.45) % 1.0) * h;
                    double rr = baseR * (0.95 + Math.sin(frac * Math.PI) * 0.18);
                    helix.add(new Vec3d(x + Math.cos(ang) * rr, waveY, z + Math.sin(ang) * rr));
                }
                final int st = strand;
                RenderUtil.stripColored(m, helix, i -> {
                    float fade = (float) Math.sin((i / (double) segs) * Math.PI);
                    int col = hurt ? 0xFF4555 : RenderUtil.accent(st * 0.35f + i * 0.02f);
                    return RenderUtil.alpha(col, a * fade * 0.85f);
                });
            }
        }

        if (style == 0 || style == 2) {
            for (int g = 0; g < 3; g++) {
                List<Vec3d> pts = new ArrayList<>();
                for (int k = 0; k < n + 8; k++) {
                    double ph = time * 2.3 + g * 2.094 - k * 0.075;
                    double rr = baseR + 0.28 + Math.sin(ph * 2.2 + g) * 0.12;
                    double py = y + h * 0.52 + Math.sin(ph * 1.65 + g * 1.7) * (h * 0.44);
                    pts.add(new Vec3d(x + Math.cos(ph) * rr, py, z + Math.sin(ph) * rr));
                }
                final int gg = g, len = pts.size();
                final float aa = a;
                RenderUtil.stripColored(m, pts, i -> {
                    float tNorm = 1f - i / (float) len;
                    int col = hurt ? 0xFF4B5C : RenderUtil.accent(gg * 0.28f + i * 0.015f);
                    return RenderUtil.alpha(col, aa * (tNorm * tNorm));
                });

                Vec3d p0 = pts.get(0);
                double s = 0.075;
                int headCol = RenderUtil.alpha(hurt ? 0xFF8899 : 0xFFFFFF, aa);
                Vec3d top = p0.add(0, s, 0), bot = p0.add(0, -s, 0);
                Vec3d e1 = p0.add(s, 0, 0), e2 = p0.add(0, 0, s), e3 = p0.add(-s, 0, 0), e4 = p0.add(0, 0, -s);
                RenderUtil.strip(m, List.of(e1, e2, e3, e4, e1), headCol);
                RenderUtil.strip(m, List.of(top, e1, bot, e3, top), headCol);
                RenderUtil.strip(m, List.of(top, e2, bot, e4, top), headCol);
            }
        }

        RenderUtil.end3D();
    }
}
