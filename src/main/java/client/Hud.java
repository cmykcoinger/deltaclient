package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Hud {
    public static class Elem {
        final String name; public int x, y, w, h;
        Elem(String n, int x, int y, int w, int h) { name = n; this.x = x; this.y = y; this.w = w; this.h = h; }
    }
    public static final Elem WATERMARK = new Elem("Watermark", 6, 6, 200, 20);
    public static final Elem TARGET = new Elem("TargetHud", -1, -1, 140, 46);
    public static final Elem MODULES = new Elem("Modules", -1, 6, 110, 12);
    private static final Elem[] ALL = {WATERMARK, TARGET, MODULES};

    private static boolean init;
    private static float shownHp;
    private static LivingEntity lastT;
    private static Elem dragging;
    private static int offX, offY;
    private static boolean wasDown;

    public static void render(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        var win = mc.getWindow();
        int sw = win.getScaledWidth(), sh = win.getScaledHeight();
        if (!init) { TARGET.x = sw / 2 + 24; TARGET.y = sh / 2 + 24; MODULES.x = sw - MODULES.w - 6; init = true; }

        float delta = tc.getTickDelta(false);
        for (Module m : ModuleManager.modules) if (m.enabled) m.onRender2D(g, delta);

        watermark(g, mc);
        targetHud(g);
        moduleList(g, mc);
        Notifications.render(g);
    }

    private static void watermark(DrawContext g, MinecraftClient mc) {
        String user = mc.getSession().getUsername();
        String ip = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
        int ping = 0;
        var nh = mc.getNetworkHandler();
        if (nh != null) { var e = nh.getPlayerListEntry(mc.player.getUuid()); if (e != null) ping = e.getLatency(); }
        String[] parts = {user, mc.getCurrentFps() + " fps", ping > 0 ? ping + " ms" : "-", ip};

        int w = 12 + Draw.tw(RenderUtil.NAME);
        for (String s : parts) w += 11 + Draw.tw(s);
        w += 6;
        int x = WATERMARK.x, y = WATERMARK.y;
        WATERMARK.w = w; WATERMARK.h = 20;

        Draw.rr(g, x - 1, y - 1, w + 2, 22, 5, 0x55000000);
        Draw.rr(g, x, y, w, 20, 4, 0xDD101018);
        for (int i = 0; i < w - 6; i += 3)
            g.fill(x + 3 + i, y + 18, x + 6 + i, y + 19, RenderUtil.alpha(RenderUtil.accent(i * 0.004f), 0.9f));
        int tx = x + 7, ci = 0;
        for (char c : RenderUtil.NAME.toCharArray()) {
            String s = String.valueOf(c);
            Draw.text(g, s, tx, y + 6, RenderUtil.accent(ci * 0.1f)); tx += Draw.tw(s); ci++;
        }
        tx += 6;
        for (String s : parts) {
            g.fill(tx, y + 5, tx + 1, y + 14, 0xFF34343F);
            tx += 5;
            Draw.text(g, s, tx, y + 6, 0xFFC8C8DC);
            tx += Draw.tw(s) + 6;
        }
    }

    private static void targetHud(DrawContext g) {
        LivingEntity t = TargetInfo.target;
        if (t == null) return;
        if (t != lastT) { shownHp = t.getHealth(); lastT = t; }
        float maxHp = Math.max(1f, t.getMaxHealth());
        float hp = t.getHealth() + t.getAbsorptionAmount();
        shownHp += (hp - shownHp) * 0.15f;
        float ratio = MathHelper.clamp(shownHp / maxHp, 0, 1);

        int x = TARGET.x, y = TARGET.y, w = TARGET.w, h = TARGET.h;
        Draw.rr(g, x - 1, y - 1, w + 2, h + 2, 6, 0x55000000);
        Draw.rr(g, x, y, w, h, 5, 0xE0101018);
        for (int i = 0; i < w - 8; i += 3)
            g.fill(x + 4 + i, y + 1, x + 7 + i, y + 2, RenderUtil.alpha(RenderUtil.accent(i * 0.005f), 1f));

        if (t instanceof AbstractClientPlayerEntity pl) {
            PlayerSkinDrawer.draw(g, pl.getSkinTextures(), x + 6, y + 7, 32);
        } else {
            Draw.rr(g, x + 6, y + 7, 32, 32, 4, 0xFF1E1E2A);
            Draw.text(g, t.getName().getString().substring(0, 1), x + 19, y + 19, 0xFFFFFFFF);
        }
        String name = t.getName().getString();
        Draw.text(g, name, x + 44, y + 8, 0xFFFFFFFF);
        String info = String.format("%.1f", hp) + " HP";
        Draw.text(g, info, x + 44, y + 19, RenderUtil.mix(0xFF4444, 0x44FF66, ratio) | 0xFF000000);
        MinecraftClient mc = MinecraftClient.getInstance();
        String right = Math.round(mc.player.distanceTo(t)) + "m  A" + t.getArmor();
        Draw.text(g, right, x + w - 6 - Draw.tw(right), y + 19, 0xFF9A9AB0);

        int bx = x + 44, bw = w - 52;
        Draw.rr(g, bx, y + 32, bw, 6, 3, 0xFF262633);
        int fw = (int) (bw * ratio);
        for (int i = 0; i < fw; i++)
            g.fill(bx + i, y + 33, bx + i + 1, y + 37, RenderUtil.accent(i * 0.01f));
    }

    private static void moduleList(DrawContext g, MinecraftClient mc) {
        List<Module> on = new ArrayList<>();
        for (Module m : ModuleManager.modules) if (m.enabled) on.add(m);
        on.sort(Comparator.comparingInt((Module m) -> -Draw.tw(m.name)));
        int right = MODULES.x + MODULES.w, y = MODULES.y, i = 0;
        for (Module m : on) {
            int tw = Draw.tw(m.name);
            g.fill(right - tw - 10, y, right, y + 11, 0xA0101018);
            g.fill(right - 2, y, right, y + 11, RenderUtil.accent(i * 0.06f));
            Draw.text(g, m.name, right - tw - 6, y + 2, RenderUtil.accent(i * 0.06f));
            y += 11; i++;
        }
        MODULES.h = Math.max(12, y - MODULES.y);
    }

    /** Режим редактирования HUD: когда открыт чат, элементы можно таскать мышкой. */
    public static void editor(DrawContext g, int mx, int my) {
        var mc = MinecraftClient.getInstance();
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            for (Elem e : ALL) if (mx >= e.x && mx <= e.x + e.w && my >= e.y && my <= e.y + e.h) {
                dragging = e; offX = mx - e.x; offY = my - e.y;
            }
        }
        if (!down) dragging = null;
        if (dragging != null) {
            var win = mc.getWindow();
            dragging.x = MathHelper.clamp(mx - offX, 0, win.getScaledWidth() - dragging.w);
            dragging.y = MathHelper.clamp(my - offY, 0, win.getScaledHeight() - dragging.h);
        }
        wasDown = down;
        for (Elem e : ALL) {
            int c = e == dragging ? RenderUtil.accent(0f) : 0xAAFFFFFF;
            g.drawBorder(e.x - 1, e.y - 1, e.w + 2, e.h + 2, c);
            Draw.text(g, e.name, e.x + 2, e.y - 11, c);
        }
    }
}
