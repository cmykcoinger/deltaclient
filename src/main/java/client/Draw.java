package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Draw {
    public static Matrix4f proj, view;
    public static Vec3d cam = Vec3d.ZERO;
    public static float delta;

    public static void setMatrices(Matrix4f p, Matrix4f v, Vec3d c, float d) {
        proj = new Matrix4f(p); view = new Matrix4f(v); cam = c; delta = d;
    }

    /** Мировая точка -> координаты на экране (в масштабе GUI), либо null, если позади камеры. */
    public static float[] project(Vec3d p) {
        if (proj == null || view == null) return null;
        Vector4f v = new Vector4f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z), 1f);
        v.mul(view);
        v.mul(proj);
        if (v.w <= 0.05f) return null;
        var w = MinecraftClient.getInstance().getWindow();
        float sx = (v.x / v.w * 0.5f + 0.5f) * w.getScaledWidth();
        float sy = (1f - (v.y / v.w * 0.5f + 0.5f)) * w.getScaledHeight();
        return new float[]{sx, sy};
    }

    public static int tw(String s) { return MinecraftClient.getInstance().textRenderer.getWidth(s); }

    public static void text(DrawContext g, String t, int x, int y, int argb) {
        g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, t, x, y, argb);
    }

    public static void line(DrawContext g, int x1, int y1, int x2, int y2, int c) {
        int n = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (n == 0) { g.fill(x1, y1, x1 + 1, y1 + 1, c); return; }
        for (int i = 0; i <= n; i++) {
            int x = x1 + (x2 - x1) * i / n, y = y1 + (y2 - y1) * i / n;
            g.fill(x, y, x + 1, y + 1, c);
        }
    }

    public static void rr(DrawContext g, int x, int y, int w, int h, int r, int c) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < r; i++) {
            double d = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - d * d));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, c);
        }
        g.fill(x, y + r, x + w, y + h - r, c);
    }
}
