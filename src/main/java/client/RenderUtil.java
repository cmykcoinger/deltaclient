package client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.List;

public class RenderUtil {
    public static int rainbow(float offset) {
        float h = ((System.currentTimeMillis() % 4000L) / 4000f + offset) % 1f;
        return Color.HSBtoRGB(h, 0.55f, 1f);
    }
    public static int alpha(int rgb, float a) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF);
    }
    public static Vec3d cam() { return MinecraftClient.getInstance().gameRenderer.getCamera().getPos(); }

    public static void begin3D() {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.5f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }
    public static void end3D() {
        RenderSystem.lineWidth(1f);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /** Точки в мировых координатах, переводятся относительно камеры. */
    public static void strip(Matrix4f m, List<Vec3d> pts, int argb) {
        Vec3d c = cam();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (Vec3d p : pts) b.vertex(m, (float) (p.x - c.x), (float) (p.y - c.y), (float) (p.z - c.z)).color(argb);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }
}
