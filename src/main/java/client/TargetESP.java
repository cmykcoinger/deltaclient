package client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class TargetESP extends Module {
    private final Setting orbCount = add(Setting.num("Orbs", 4, 1, 8));
    private final Setting radius = add(Setting.num("Orbit radius", 1.2, 0.5, 2.0));
    private final Setting size = add(Setting.num("Orb size", 0.15, 0.05, 0.4));

    public TargetESP() {
        super("MikgerTargetESP", Category.VISUALS);
        key = org.lwjgl.glfw.GLFW.GLFW_KEY_V;
    }

    private void drawOrb(Matrix4f matrix, Vec3d pos, float size, int innerColor, int outerColor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        
        // Строим билборд (плоскость, которая смотрит на камеру)
        Vec3d dir = camPos.subtract(pos).normalize();
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = dir.crossProduct(up).normalize();
        if (right.lengthSquared() < 0.001) right = new Vec3d(1, 0, 0);
        up = right.crossProduct(dir).normalize();

        Vec3d p1 = pos.add(right.multiply(size)).add(up.multiply(size));
        Vec3d p2 = pos.add(right.multiply(size)).subtract(up.multiply(size));
        Vec3d p3 = pos.subtract(right.multiply(size)).subtract(up.multiply(size));
        Vec3d p4 = pos.subtract(right.multiply(size)).add(up.multiply(size));

        // Внутренний яркий шар
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(innerColor);
        buffer.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(innerColor);
        buffer.vertex(matrix, (float)p3.x, (float)p3.y, (float)p3.z).color(innerColor);
        buffer.vertex(matrix, (float)p4.x, (float)p4.y, (float)p4.z).color(innerColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // Внешний тусклый ореол (свечение)
        Vec3d o1 = pos.add(right.multiply(size * 1.8)).add(up.multiply(size * 1.8));
        Vec3d o2 = pos.add(right.multiply(size * 1.8)).subtract(up.multiply(size * 1.8));
        Vec3d o3 = pos.subtract(right.multiply(size * 1.8)).subtract(up.multiply(size * 1.8));
        Vec3d o4 = pos.subtract(right.multiply(size * 1.8)).add(up.multiply(size * 1.8));

        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, (float)o1.x, (float)o1.y, (float)o1.z).color(outerColor);
        buffer.vertex(matrix, (float)o2.x, (float)o2.y, (float)o2.z).color(outerColor);
        buffer.vertex(matrix, (float)o3.x, (float)o3.y, (float)o3.z).color(outerColor);
        buffer.vertex(matrix, (float)o4.x, (float)o4.y, (float)o4.z).color(outerColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
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

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // Аддитивное свечение
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        for (int i = 0; i < count; i++) {
            double phase = time * 2.5 + i * (Math.PI * 2.0 / count);
            double orbitR = t.getWidth() * radius.value;
            double orbY = y + h * 0.5 + Math.sin(phase * 1.5) * (h * 0.4);
            Vec3d pos = new Vec3d(x + Math.cos(phase) * orbitR, orbY, z + Math.sin(phase) * orbitR);

            int inner = 0xFFFFAA00; 
            int outer = 0x44FF3300; 
            
            drawOrb(m, pos, size.value.floatValue(), inner, outer);
            
            // Шлейф (5 затухающих копий)
            for (int k = 1; k <= 5; k++) {
                double tp = phase - k * 0.1;
                double tr = t.getWidth() * radius.value;
                double ty = y + h * 0.5 + Math.sin(tp * 1.5) * (h * 0.4);
                Vec3d tPos = new Vec3d(x + Math.cos(tp) * tr, ty, z + Math.sin(tp) * tr);
                
                int trailCol = 0x22FF3300;
                drawOrb(m, tPos, size.value.floatValue() * 0.8f, trailCol, 0x00FF3300);
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
