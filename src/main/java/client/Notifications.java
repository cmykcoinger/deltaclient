package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Всплывающие уведомления справа снизу. */
public class Notifications {
    private static class N { String text; boolean on; long t0; }
    private static final List<N> list = new ArrayList<>();
    private static final long LIFE = 2600;

    public static void push(String text, boolean on) {
        N n = new N(); n.text = text; n.on = on; n.t0 = System.currentTimeMillis();
        list.add(n);
        if (list.size() > 6) list.remove(0);
    }

    public static void render(DrawContext g) {
        var win = MinecraftClient.getInstance().getWindow();
        int sw = win.getScaledWidth(), sh = win.getScaledHeight();
        long now = System.currentTimeMillis();
        list.removeIf(n -> now - n.t0 > LIFE);
        int y = sh - 34;
        for (int i = list.size() - 1; i >= 0; i--) {
            N n = list.get(i);
            long age = now - n.t0;
            float in = Math.min(1f, age / 250f);
            float out = age > LIFE - 300 ? (LIFE - age) / 300f : 1f;
            float p = 1f - (float) Math.pow(1f - Math.min(in, out), 3);
            int w = Draw.tw(n.text) + 30;
            int x = sw - (int) ((w + 8) * p);
            int col = n.on ? 0xFF4CE08A : 0xFFFF5C6C;
            Draw.rr(g, x, y, w, 20, 4, 0xE0101018);
            Draw.rr(g, x, y + 3, 2, 14, 1, col);
            g.fill(x + 8, y + 7, x + 14, y + 13, col);
            Draw.text(g, n.text, x + 19, y + 6, 0xFFFFFFFF);
            int bar = (int) ((w - 4) * (1f - age / (float) LIFE));
            g.fill(x + 2, y + 18, x + 2 + Math.max(bar, 0), y + 19, (col & 0xFFFFFF) | 0x99000000);
            y -= 24;
        }
    }
}
