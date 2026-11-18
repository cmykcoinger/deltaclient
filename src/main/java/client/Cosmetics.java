package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Косметика: нимб над головой, искры вокруг тела и светящийся след. */
public class Cosmetics extends Module {
    private final Setting halo  = add(Setting.bool("Halo", true));
    private final Setting orbit = add(Setting.bool("Orbit sparks", true));
    private final Setting trail = add(Setting.bool("Trail", true));
    private final Setting tlen  = add(Setting.num("Trail length", 24, 8, 48));

    private final ArrayDeque<Vec3d> history = new ArrayDeque<>();

    public Cosmetics() { super("Cosmetics", Category.VISUALS); }

    @Override
    public void onTick() {
        var p = MinecraftClient.getInstance().player;
        Vec3d pos = p.getPos().add(0, 0.08, 0);
        if (history.isEmpty() || history.peekFirst().squaredDistanceTo(pos) > 0.01) history.addFirst(pos);
        while (history.size() > (int) tlen.value) history.removeLast();
    }

    @Override
    public void onRender3D(Matrix4f m, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        boolean first = mc.options.getPerspective().isFirstPerson();
        Vec3d base = p.getLerpedPos(delta);
        double time = System.currentTimeMillis() / 1000.0;

        RenderUtil.begin3D();
        if (halo.on() && !first) {
            List<Vec3d> pts = new ArrayList<>();
            double y = base.y + p.getHeight() + 0.3 + Math.sin(time * 2) * 0.03;
            for (int i = 0; i <= 40; i++) {
                double a = i * Math.PI * 2 / 40;
                pts.add(new Vec3d(base.x + Math.cos(a) * 0.32, y, base.z + Math.sin(a) * 0.32));
            }
            RenderUtil.stripColored(m, pts, i -> RenderUtil.alpha(RenderUtil.accent(i * 0.03f), 1f));
        }
        if (orbit.on() && !first) {
            for (int g = 0; g < 3; g++) {
                List<Vec3d> pts = new ArrayList<>();
                for (int k = 0; k < 10; k++) {
                    double ph = time * 2.2 + g * 2.094 - k * 0.12;
                    pts.add(new Vec3d(base.x + Math.cos(ph) * 0.75,
                            base.y + p.getHeight() * 0.55 + Math.sin(ph * 1.6 + g) * p.getHeight() * 0.4,
                            base.z + Math.sin(ph) * 0.75));
                }
                final int gg = g;
                RenderUtil.stripColored(m, pts, i -> RenderUtil.alpha(RenderUtil.accent(gg * 0.2f), 1f - i / 10f));
            }
        }
        if (trail.on() && history.size() > 1) {
            List<Vec3d> pts = new ArrayList<>(history);
            int n = pts.size();
            RenderUtil.stripColored(m, pts, i -> RenderUtil.alpha(RenderUtil.accent(i * 0.02f), 1f - i / (float) n));
        }
        RenderUtil.end3D();
    }
}
