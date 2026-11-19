package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Уведомления по центру: мини-переключатель + текст. */
public class Notifications {
    private static class N { String text; boolean on; long t0; }
    private static final List<N> list = new ArrayList<>();
    private static final long LIFE = 2600;

    public static void push(String text, boolean on) {
        N n = new N(); n.text = text; n.on = on; n.t0 = System.currentTimeMillis();
        list.add(n);
        if (list.size() > 5) list.remove(0);
    }

    public static void render(DrawContext g) {
        var win = MinecraftClient.getInstance().getWindow();
        int sw = win.getScaledWidth(), sh = win.getScaledHeight();
        long now = System.currentTimeMillis();
        list.removeIf(n -> now - n.t0 > LIFE);
        int acc = RenderUtil.THEMES[RenderUtil.theme][0] & 0xFFFFFF;
        int y = sh / 2 + 70;
        for (int i = list.size() - 1; i >= 0; i--) {
            N n = list.get(i);
            long age = now - n.t0;
            float in = Math.min(1f, age / 250f);
            float out = age > LIFE - 300 ? (LIFE - age) / 300f : 1f;
            float p = 1f - (float) Math.pow(1f - Math.min(in, out), 3);
            int w = Draw.tw(n.text) + 34;
            int x = sw / 2 - w / 2;
            int yy = y + (int) ((1f - p) * 10);
            Draw.rr(g, x - 1, yy - 1, w + 2, 20, 10, 0x33000000);
            Draw.rr(g, x, yy, w, 18, 9, 0xE6141420);
            Draw.rr(g, x + 6, yy + 5, 16, 8, 4, n.on ? (0xFF000000 | acc) : 0xFF3A3947);
            Draw.rr(g, x + (n.on ? 14 : 7), yy + 6, 6, 6, 3, 0xFFFFFFFF);
            Draw.text(g, n.text, x + 28, yy + 5, 0xFFFFFFFF);
            g.fill(x + 8, yy + 17, x + w - 8, yy + 18, 0xCC000000 | acc);
            y -= 22;
        }
    }
}
