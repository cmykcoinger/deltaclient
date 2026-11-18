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
    /** Название клиента (меняется тут). */
    public static final String NAME = "Delta";

    public static final int[][] THEMES = {
        {0x7B5CFF, 0x3CC8FF}, {0xFF4D6D, 0xFFA14D}, {0x2EE59D, 0x2E8BFF}, {0xFF5CE1, 0x8A5CFF}, {0xFFFFFF, 0x9AA0B5}
    };
    public static int theme = 0;

    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
    /** Плавно переливающийся цвет текущей темы. */
    public static int accent(float off) {
        float k = (float) (Math.sin(System.currentTimeMillis() / 900.0 + off * 6) * 0.5 + 0.5);
        return mix(THEMES[theme][0], THEMES[theme][1], k);
    }

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

    /** Линия, у которой цвет (и прозрачность) каждой точки задаётся функцией. */
    public static void stripColored(Matrix4f m, List<Vec3d> pts, java.util.function.IntUnaryOperator col) {
        Vec3d c = cam();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < pts.size(); i++) {
            Vec3d p = pts.get(i);
            b.vertex(m, (float) (p.x - c.x), (float) (p.y - c.y), (float) (p.z - c.z)).color(col.applyAsInt(i));
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }
}
