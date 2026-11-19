package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

public class Draw {
    public static Matrix4f proj, view;
    public static Vec3d cam = Vec3d.ZERO;
    public static float delta;

    public static void setMatrices(Matrix4f p, Matrix4f v, Vec3d c, float d) {
        proj = new Matrix4f(p); view = new Matrix4f(v); cam = c; delta = d;
    }

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

    /** Текст без «тяжёлой» тени - выглядит чище. */
    public static void text(DrawContext g, String t, int x, int y, int argb) {
        g.drawText(MinecraftClient.getInstance().textRenderer, t, x, y, argb, false);
    }

    public static void line(DrawContext g, int x1, int y1, int x2, int y2, int c) {
        int n = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (n == 0) { g.fill(x1, y1, x1 + 1, y1 + 1, c); return; }
        for (int i = 0; i <= n; i++) {
            int x = x1 + (x2 - x1) * i / n, y = y1 + (y2 - y1) * i / n;
            g.fill(x, y, x + 1, y + 1, c);
        }
    }

    /** Скруглённый прямоугольник со сглаженными (полупрозрачными) краями углов. */
    public static void rr(DrawContext g, int x, int y, int w, int h, int r, int c) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { g.fill(x, y, x + w, y + h, c); return; }
        int a = (c >>> 24) & 255, rgb = c & 0xFFFFFF;
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            double inset = r - Math.sqrt(r * (double) r - dy * dy);
            int in = (int) Math.ceil(inset);
            float cov = (float) (in - inset);
            int top = y + i, bot = y + h - 1 - i;
            g.fill(x + in, top, x + w - in, top + 1, c);
            g.fill(x + in, bot, x + w - in, bot + 1, c);
            if (in > 0 && cov > 0.03f) {
                int ec = ((int) (a * cov) << 24) | rgb;
                g.fill(x + in - 1, top, x + in, top + 1, ec);
                g.fill(x + w - in, top, x + w - in + 1, top + 1, ec);
                g.fill(x + in - 1, bot, x + in, bot + 1, ec);
                g.fill(x + w - in, bot, x + w - in + 1, bot + 1, ec);
            }
        }
        g.fill(x, y + r, x + w, y + h - r, c);
    }

    public static void dot(DrawContext g, int cx, int cy, int r, int c) { rr(g, cx - r, cy - r, r * 2, r * 2, r, c); }

    public static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            default -> "K" + key;
        };
    }

    public static int chipW(String t) { return tw(t) + 26; }

    /** Плашка-«таблетка»: точка | текст, снизу тонкая линия цвета темы. */
    public static int chip(DrawContext g, int x, int y, String t, int accRgb, int textArgb) {
        int w = chipW(t);
        rr(g, x - 1, y - 1, w + 2, 16, 8, 0x33000000);
        rr(g, x, y, w, 14, 7, 0xE6141420);
        dot(g, x + 9, y + 7, 3, 0xFF000000 | (accRgb & 0xFFFFFF));
        g.fill(x + 16, y + 4, x + 17, y + 10, 0xFF30303C);
        text(g, t, x + 21, y + 3, textArgb);
        g.fill(x + 9, y + 13, x + w - 9, y + 14, 0x99000000 | (accRgb & 0xFFFFFF));
        return w;
    }
}
