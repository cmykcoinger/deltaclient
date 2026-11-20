package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class TargetESP extends Module {
    private final Setting orbCount = add(Setting.num("Orbs", 4, 1, 8));
    private final Setting radius = add(Setting.num("Orbit radius", 1.2, 0.5, 2.0));
    private final Setting size = add(Setting.num("Orb size", 0.15, 0.05, 0.4));

    public TargetESP() {
        super("MikgerTargetESP", Category.VISUALS);
        key = org.lwjgl.glfw.GLFW.GLFW_KEY_V;
    }

    // Безопасный орб из линий (как в твоем старом коде)
    private void drawOrb(Matrix4f m, Vec3d center, float r, int col) {
        List<Vec3d> circle = new ArrayList<>();
        int segments = 12;
        for (int i = 0; i <= segments; i++) {
            double angle = i * Math.PI * 2.0 / segments;
            circle.add(center.add(Math.cos(angle) * r, Math.sin(angle) * r, 0));
        }
        RenderUtil.stripColored(m, circle, i -> RenderUtil.alpha(col, 0.8f));
        
        List<Vec3d> circle2 = new ArrayList<>();
        for (int i = 0; i <= segments; i++) {
            double angle = i * Math.PI * 2.0 / segments;
            circle2.add(center.add(Math.cos(angle) * r, 0, Math.sin(angle) * r));
        }
        RenderUtil.stripColored(m, circle2, i -> RenderUtil.alpha(col, 0.6f));
    }

    @Override
    public void onRender3D(Matrix4f m, float delta) {
        LivingEntity t = MikgerKillAura.target;
        if (t == null) return;

        double x = MathHelper.lerp(delta, t.prevX, t.getX());
        double y = MathHelper.lerp(delta, t.prevY, t.getY());
        double z = MathHelper.lerp(delta, t.prevZ, t.getZ());
        double h = t.getHeight();
        double time = System.currentTimeMillis() / 1000.0;
        int count = Math.max(1, (int) Math.round(orbCount.value));

        RenderUtil.begin3D();

        for (int i = 0; i < count; i++) {
            double phase = time * 2.5 + i * (Math.PI * 2.0 / count);
            double orbitR = t.getWidth() * radius.value;
            double orbY = y + h * 0.5 + Math.sin(phase * 1.5) * (h * 0.4);
            Vec3d pos = new Vec3d(x + Math.cos(phase) * orbitR, orbY, z + Math.sin(phase) * orbitR);

            int color = t.hurtTime > 0 ? 0xFF3B52 : 0xFFAA00;
            
            drawOrb(m, pos, size.value.floatValue(), color);
            
            for (int k = 1; k <= 5; k++) {
                double tp = phase - k * 0.1;
                double tr = t.getWidth() * radius.value;
                double ty = y + h * 0.5 + Math.sin(tp * 1.5) * (h * 0.4);
                Vec3d tPos = new Vec3d(x + Math.cos(tp) * tr, ty, z + Math.sin(tp) * tr);
                drawOrb(m, tPos, size.value.floatValue() * (1f - k * 0.15f), color);
            }
        }

        RenderUtil.end3D();
    }
}
