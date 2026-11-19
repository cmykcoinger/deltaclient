package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClickGui extends Screen {
    private static final int CAT = 0, TOGGLE = 1, BIND = 2, BOOL = 3, SLIDER = 4, SEARCH = 5, THEME = 6, HEADER = 7;

    private static int selected = -1;
    private static int ox, oy;
    private static float scroll, scrollTarget, maxScroll;
    private static final float[] catAnim = new float[8];

    private static class Hit { int x, y, w, h, type, idx; Module m; Setting s; }
    private final List<Hit> hits = new ArrayList<>();
    private Hit slider;
    private Module binding;
    private boolean searching, dragging;
    private String search = "";
    private float anim;
    private int contentY, contentH;

    public ClickGui() { super(Text.literal("Rockstar")); }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        anim += (1f - anim) * 0.26f;
        float a = anim;
        int pad = 8;
        int winW = Math.min((int) (width * 0.92f), 680);
        int winH = Math.min((int) (height * 0.90f), 405);
        int winX = (width - winW) / 2 + ox;
        int winY = (height - winH) / 2 + oy + (int) ((1f - a) * 10);

        int[] T = RenderUtil.THEMES[RenderUtil.theme];
        int acc = T[0] & 0xFFFFFF;
        int acc2 = T[1] & 0xFFFFFF;
        hits.clear();

        g.fill(0, 0, width, height, RenderUtil.alpha(0x05070F, 0.42f * a));
        Draw.rr(g, winX - 2, winY - 2, winW + 4, winH + 4, 16, c(0x000000, 0.45f));
        Draw.rr(g, winX - 1, winY - 1, winW + 2, winH + 2, 15, c(acc, 0.38f));
        Draw.rr(g, winX, winY, winW, winH, 14, c(0x0E1320, 0.92f));
        add(HEADER, winX, winY, winW, pad + 24, 0, null, null);

        int sbW = MathHelper.clamp((int) (winW * 0.22f), 96, 142);
        int sx = winX + pad, sy = winY + pad, sh = winH - 2 * pad;
        Draw.rr(g, sx, sy, sbW, sh, 10, c(0x131929, 0.90f));

        Draw.rr(g, sx + 8, sy + 8, 20, 20, 6, c(acc, 0.26f));
        scaled(g, "R", sx + 13.5f, sy + 11f, 1.75f, c(acc2, 1f));
        scaled(g, "Rockstar", sx + 34, sy + 12.5f, 1.25f, c(0xFFFFFF, 1f));

        Draw.text(g, "Categories", sx + 10, sy + 38, c(0x7C88A4, 1f));
        Category[] cats = Category.values();
        int step = sh < 250 ? 19 : 23;
        for (int i = -1; i < cats.length; i++) {
            int iy = sy + 50 + (i + 1) * step;
            String nm = i < 0 ? "All" : disp(cats[i]);
            int count = i < 0 ? ModuleManager.modules.size() : ModuleManager.byCategory(cats[i]).size();
            boolean sel = selected == i;
            boolean hov = in(mx, my, sx + 6, iy, sbW - 12, 19);
            catAnim[i + 1] += ((sel ? 1f : hov ? 0.45f : 0f) - catAnim[i + 1]) * 0.25f;
            float k = catAnim[i + 1];

            Draw.rr(g, sx + 6, iy, sbW - 12, 18, 6, c(0x1C253B, k * 0.92f));
            if (k > 0.05f) {
                g.fill(sx + 6, iy + 3, sx + 8, iy + 15, c(acc, k));
            }
            Draw.text(g, nm, sx + 14, iy + 5, c(RenderUtil.mix(0xAAB5CC, 0xFFFFFF, k), 1f));

            String cs = String.valueOf(count);
            int pw = Draw.tw(cs) + 8;
            Draw.rr(g, sx + sbW - 10 - pw, iy + 3, pw, 12, 5, c(sel ? acc : 0x171F30, sel ? 0.35f : 0.85f));
            Draw.text(g, cs, sx + sbW - 6 - pw, iy + 5, c(sel ? 0xFFFFFF : 0x7E8AA4, 1f));
            add(CAT, sx + 6, iy, sbW - 12, 18, i, null, null);
        }

        int setY = sy + 50 + (cats.length + 1) * step + 4;
        if (setY + 34 <= sy + sh - 4) {
            Draw.text(g, "Theme", sx + 10, setY, c(0x7C88A4, 1f));
            int ty = setY + 12;
            boolean hov = in(mx, my, sx + 6, ty, sbW - 12, 18);
            Draw.rr(g, sx + 6, ty, sbW - 12, 18, 6, c(0x1C253B, hov ? 0.90f : 0.60f));
            g.fill(sx + 11, ty + 5, sx + 17, ty + 13, c(acc, 1f));
            Draw.text(g, "Palette #" + (RenderUtil.theme + 1), sx + 22, ty + 5, c(0xE8EEFA, 1f));
            add(THEME, sx + 6, ty, sbW - 12, 18, 0, null, null);
        }

        if (sh >= 250) {
            int uy = sy + sh - 32;
            Draw.rr(g, sx + 6, uy, sbW - 12, 26, 7, c(0x151C2D, 0.90f));
            String user = MinecraftClient.getInstance().getSession().getUsername();
            Draw.text(g, user.length() > 13 ? user.substring(0, 13) : user, sx + 12, uy + 5, c(0xFFFFFF, 1f));
            Draw.text(g, "Rockstar 1.21.4", sx + 12, uy + 15, c(0x7E8AA4, 1f));
        }

        int mx0 = sx + sbW + pad, mw = winX + winW - pad - mx0;
        g.fill(mx0 + 2, winY + pad + 4, mx0 + 5, winY + pad + 14, c(acc, 1f));
        scaled(g, selected < 0 ? "All Modules" : disp(cats[selected]), mx0 + 10, winY + pad + 3, 1.15f, c(0xFFFFFF, 1f));

        String ms = ping() + " ms";
        int msW = Draw.tw(ms) + 8;
        int pw = Math.max(64, Math.min(150, mw - msW - 95));
        int px = mx0 + mw - msW - pw;
        Draw.rr(g, px, winY + pad + 1, pw, 16, 6, c(searching ? 0x222E46 : 0x161E30, 0.90f));
        String st = search.isEmpty() && !searching ? "Search..." : search + (searching && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        Draw.text(g, st, px + 7, winY + pad + 5, c(search.isEmpty() ? 0x67738E : 0xFFFFFF, 1f));
        Draw.text(g, ms, px + pw + 6, winY + pad + 5, c(0x78849E, 1f));
        add(SEARCH, px, winY + pad + 1, pw, 16, 0, null, null);

        int cx = mx0, cy = winY + pad + 24, cw = mw, ch = winH - pad - 24 - pad;
        contentY = cy; contentH = ch;
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.modules) {
            if (!search.isEmpty()) { if (!m.name.toLowerCase().contains(search.toLowerCase())) continue; }
            else if (selected >= 0 && m.category != cats[selected]) continue;
            list.add(m);
        }
        list.sort(Comparator.comparing(m -> m.name.toLowerCase()));

        scrollTarget = MathHelper.clamp(scrollTarget, 0, maxScroll);
        scroll += (scrollTarget - scroll) * 0.3f;

        int cols = cw >= 330 ? 3 : cw >= 215 ? 2 : 1, gap = 8;
        int colW = (cw - 6 - gap * (cols - 1)) / cols;
        float[] colY = new float[cols];
        g.enableScissor(cx, cy, cx + cw, cy + ch);
        for (Module m : list) {
            m.animOn += ((m.enabled ? 1f : 0f) - m.animOn) * 0.25f;
            int c0 = 0;
            for (int i = 1; i < cols; i++) if (colY[i] < colY[c0]) c0 = i;
            int x = cx + c0 * (colW + gap);
            int y = (int) (cy + colY[c0] - scroll);
            int h = cardH(m);
            if (y + h >= cy && y <= cy + ch) drawCard(g, m, x, y, colW, h, acc, acc2);
            colY[c0] += h + gap;
        }
        g.disableScissor();

        float total = 0;
        for (float v : colY) total = Math.max(total, v);
        maxScroll = Math.max(0, total - ch);
        if (maxScroll > 0) {
            int bh = Math.max(14, (int) (ch * (ch / total)));
            int by = cy + (int) ((ch - bh) * (scroll / maxScroll));
            Draw.rr(g, cx + cw - 3, by, 2, bh, 1, c(acc, 0.85f));
        }
        if (slider != null) setSlider(slider, mx);
    }

    private void drawCard(DrawContext g, Module m, int x, int y, int w, int h, int acc, int acc2) {
        float k = m.animOn;
        Draw.rr(g, x, y, w, h, 8, c(0x131929, 0.90f));
        if (k > 0.04f) {
            g.fill(x, y + 3, x + 2, y + h - 3, c(acc, k));
        }
        Draw.text(g, m.name, x + 9, y + 9, c(RenderUtil.mix(0xAAB5CC, 0xFFFFFF, k), 1f));

        int right = x + w - 8;
        if (m.action) {
            Draw.rr(g, right - 26, y + 7, 26, 12, 4, c(acc, 0.45f));
            Draw.text(g, "USE", right - 20, y + 9, c(0xFFFFFF, 1f));
            right -= 32;
        } else {
            Draw.rr(g, right - 22, y + 7, 22, 12, 4, c(RenderUtil.mix(0x252F45, acc, k), 0.92f));
            g.fill(right - 20 + (int) (k * 10), y + 8, right - 12 + (int) (k * 10), y + 18, c(0xF6F8FF, 1f));
            right -= 28;
        }
        add(TOGGLE, x, y, w, 26, 0, m, null);

        String bl = binding == m ? "..." : (m.key > 0 ? keyName(m.key) : "None");
        int bw = Draw.tw(bl) + 10;
        Draw.rr(g, right - bw, y + 7, bw, 12, 4, c(0x1A2234, 0.90f));
        Draw.text(g, bl, right - bw + 5, y + 9, c(binding == m ? 0xFFFFFF : 0x7E8AA4, 1f));
        add(BIND, right - bw, y + 7, bw, 12, 0, m, null);

        int ry = y + 26;
        for (Setting s : m.settings) {
            if (s.bool) {
                Draw.text(g, s.name, x + 8, ry + 4, c(0xE8EEF8, 1f));
                box(g, x + w - 20, ry + 2, s.on(), acc);
                add(BOOL, x, ry, w, 16, 0, m, s);
                ry += 16;
            } else {
                String v = s.options != null ? s.options[Math.max(0, Math.min(s.options.length - 1, (int) Math.round(s.value)))] : String.format("%.1f", s.value);
                Draw.text(g, s.name, x + 8, ry + 3, c(0xE8EEF8, 1f));
                Draw.text(g, v, x + w - 8 - Draw.tw(v), ry + 3, c(acc2, 1f));
                int tx = x + 8, tw = w - 16, ty = ry + 16;
                float f = (float) ((s.value - s.min) / (s.max - s.min));
                int fw = (int) (tw * f);
                Draw.rr(g, tx, ty, tw, 3, 1, c(0x202A3E, 0.92f));
                Draw.rr(g, tx, ty, Math.max(fw, 3), 3, 1, c(acc, 1f));
                g.fill(tx + fw - 2, ty - 2, tx + fw + 2, ty + 5, c(0xFFFFFF, 1f));
                add(SLIDER, tx, ry, tw, 24, 0, m, s);
                ry += 24;
            }
        }
    }

    private static int cardH(Module m) {
        int h = 26;
        if (!m.settings.isEmpty()) {
            for (Setting s : m.settings) h += s.bool ? 16 : 24;
            h += 6;
        }
        return h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        searching = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            boolean content = h.type == TOGGLE || h.type == BIND || h.type == BOOL || h.type == SLIDER;
            if (content && (my < contentY || my > contentY + contentH)) continue;
            switch (h.type) {
                case CAT -> { selected = h.idx; search = ""; scroll = 0; scrollTarget = 0; }
                case THEME -> RenderUtil.theme = (RenderUtil.theme + 1) % RenderUtil.THEMES.length;
                case SEARCH -> searching = true;
                case HEADER -> dragging = true;
                case TOGGLE -> { if (btn == 2) binding = h.m; else h.m.trigger(); }
                case BIND -> binding = h.m;
                case BOOL -> h.s.toggle();
                case SLIDER -> { slider = h; setSlider(h, (float) mx); }
            }
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging) { ox += (int) dx; oy += (int) dy; return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragging = false; slider = null;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scrollTarget -= (float) vAmount * 18f;
        return true;
    }

    @Override
    public boolean charTyped(char chr, int mod) {
        if (searching && chr >= 32) { search += chr; scroll = 0; scrollTarget = 0; return true; }
        return super.charTyped(chr, mod);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (binding != null) {
            binding.key = key == GLFW.GLFW_KEY_ESCAPE ? -1 : key;
            binding = null;
            return true;
        }
        if (searching) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) search = search.substring(0, search.length() - 1);
            else if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) searching = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(key, sc, mod);
    }

    private int c(int rgb, float k) { return RenderUtil.alpha(rgb, Math.max(k * anim, 0.04f)); }

    private void scaled(DrawContext g, String t, float x, float y, float s, int argb) {
        g.getMatrices().push();
        g.getMatrices().translate(x, y, 0f);
        g.getMatrices().scale(s, s, 1f);
        Draw.text(g, t, 0, 0, argb);
        g.getMatrices().pop();
    }

    private void box(DrawContext g, int x, int y, boolean on, int acc) {
        Draw.rr(g, x, y, 12, 12, 4, c(on ? acc : 0x222C42, 0.92f));
        if (on) {
            Draw.line(g, x + 3, y + 6, x + 5, y + 8, c(0xFFFFFF, 1f));
            Draw.line(g, x + 5, y + 8, x + 9, y + 3, c(0xFFFFFF, 1f));
        }
    }

    private static int ping() {
        var mc = MinecraftClient.getInstance();
        var nh = mc.getNetworkHandler();
        if (nh == null || mc.player == null) return 0;
        var e = nh.getPlayerListEntry(mc.player.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    private static String disp(Category c) {
        return switch (c) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case VISUALS -> "Visuals";
            case PLAYER -> "Assist";
            case SERVER -> "Server";
            case MISC -> "Misc";
        };
    }

    private void setSlider(Hit h, float mx) {
        double k = MathHelper.clamp((mx - h.x) / (double) h.w, 0, 1);
        double raw = h.s.min + (h.s.max - h.s.min) * k;
        h.s.value = h.s.options != null ? Math.round(raw) : Math.round(raw * 100) / 100.0;
    }

    private void add(int type, int x, int y, int w, int h, int idx, Module m, Setting s) {
        Hit t = new Hit(); t.type = type; t.x = x; t.y = y; t.w = w; t.h = h; t.idx = idx; t.m = m; t.s = s; hits.add(t);
    }

    private static boolean in(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            default -> "K" + key;
        };
    }
}
