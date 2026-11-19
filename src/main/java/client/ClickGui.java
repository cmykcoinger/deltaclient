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

    private static int selected = -1;            // -1 = All
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

    public ClickGui() { super(Text.literal("Mikger")); }

    @Override public boolean shouldPause() { return false; }

    // ------------------------------------------------------------------ render
    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        anim += (1f - anim) * 0.2f;
        float a = anim;
        int pad = 8;
        int winW = Math.min((int) (width * 0.92f), 640);
        int winH = Math.min((int) (height * 0.9f), 380);
        int winX = (width - winW) / 2 + ox;
        int winY = (height - winH) / 2 + oy + (int) ((1f - a) * 14);
        int[] T = RenderUtil.THEMES[RenderUtil.theme];
        int acc = T[0] & 0xFFFFFF;
        hits.clear();

        g.fill(0, 0, width, height, RenderUtil.alpha(0x000000, .38f * a));
        Draw.rr(g, winX - 2, winY - 2, winW + 4, winH + 4, 18, c(0x000000, .3f));
        Draw.rr(g, winX, winY, winW, winH, 16, c(0x15141C, .95f));
        add(HEADER, winX, winY, winW, pad + 24, 0, null, null);

        // ---------- боковая панель ----------
        int sbW = MathHelper.clamp((int) (winW * 0.22f), 86, 132);
        int sx = winX + pad, sy = winY + pad, sh = winH - 2 * pad;
        Draw.rr(g, sx, sy, sbW, sh, 12, c(0x1C1B26, 1f));

        // логотип: красная M
        Draw.rr(g, sx + 8, sy + 8, 22, 22, 11, c(0x3A1219, 1f));
        scaled(g, "M", sx + 13.2f, sy + 11.5f, 1.8f, c(0xFF3B4D, 1f));
        scaled(g, "M", sx + 13.9f, sy + 11.5f, 1.8f, c(0xFF3B4D, 1f));
        scaled(g, RenderUtil.NAME, sx + 36, sy + 13, 1.3f, c(0xFFFFFF, 1f));

        Draw.text(g, "Client categories", sx + 10, sy + 42, c(0x5E5D72, 1f));
        Category[] cats = Category.values();
        int step = sh < 250 ? 19 : 24;
        for (int i = -1; i < cats.length; i++) {
            int iy = sy + 54 + (i + 1) * step;
            String nm = i < 0 ? "All" : disp(cats[i]);
            int count = i < 0 ? ModuleManager.modules.size() : ModuleManager.byCategory(cats[i]).size();
            boolean sel = selected == i;
            boolean hov = in(mx, my, sx + 6, iy, sbW - 12, 20);
            catAnim[i + 1] += ((sel ? 1f : hov ? .4f : 0f) - catAnim[i + 1]) * .25f;
            float k = catAnim[i + 1];
            Draw.rr(g, sx + 6, iy, sbW - 12, 18, 9, c(0x2B2A3A, k));
            Draw.rr(g, sx + 12, iy + 5, 8, 8, 4, c(RenderUtil.mix(0x55546B, acc, k), 1f));
            Draw.text(g, nm, sx + 25, iy + 5, c(RenderUtil.mix(0x9A99B0, 0xFFFFFF, k), 1f));
            String cs = String.valueOf(count);
            int pw = Draw.tw(cs) + 8;
            Draw.rr(g, sx + sbW - 10 - pw, iy + 3, pw, 12, 6, c(sel ? 0x3A3950 : 0x25242F, 1f));
            Draw.text(g, cs, sx + sbW - 6 - pw, iy + 5, c(sel ? 0xFFFFFF : 0x6A6980, 1f));
            add(CAT, sx + 6, iy, sbW - 12, 18, i, null, null);
        }
        int setY = sy + 54 + (cats.length + 1) * step + 4;
        if (setY + 34 <= sy + sh - 4) {
            Draw.text(g, "Settings", sx + 10, setY, c(0x5E5D72, 1f));
            int ty = setY + 12;
            boolean hov = in(mx, my, sx + 6, ty, sbW - 12, 18);
            Draw.rr(g, sx + 6, ty, sbW - 12, 18, 9, c(0x2B2A3A, hov ? 1f : 0f));
            Draw.rr(g, sx + 12, ty + 5, 8, 8, 4, c(acc, 1f));
            Draw.text(g, "Theme", sx + 25, ty + 5, c(0x9A99B0, 1f));
            add(THEME, sx + 6, ty, sbW - 12, 18, 0, null, null);
        }
        if (sh >= 250) {
            int uy = sy + sh - 34;
            Draw.rr(g, sx + 6, uy, sbW - 12, 28, 13, c(0x25242F, 1f));
            Draw.rr(g, sx + 12, uy + 7, 14, 14, 7, c(0x3A3950, 1f));
            String user = MinecraftClient.getInstance().getSession().getUsername();
            Draw.text(g, user.substring(0, 1).toUpperCase(), sx + 16, uy + 10, c(0xFFFFFF, 1f));
            Draw.text(g, user.length() > 10 ? user.substring(0, 10) : user, sx + 32, uy + 6, c(0xFFFFFF, 1f));
            Draw.text(g, RenderUtil.NAME, sx + 32, uy + 16, c(0x6A6980, 1f));
        }

        // ---------- заголовок и поиск ----------
        int mx0 = sx + sbW + pad, mw = winX + winW - pad - mx0;
        Draw.rr(g, mx0 + 2, winY + pad + 4, 10, 10, 5, c(acc, 1f));
        scaled(g, selected < 0 ? "All" : disp(cats[selected]), mx0 + 17, winY + pad + 3, 1.2f, c(0xFFFFFF, 1f));
        String ms = ping() + " ms";
        int msW = Draw.tw(ms) + 8;
        int pw = Math.max(60, Math.min(150, mw - msW - 70));
        int px = mx0 + mw - msW - pw;
        Draw.rr(g, px, winY + pad + 1, pw, 16, 8, c(searching ? 0x2E2D3E : 0x22212D, 1f));
        String st = search.isEmpty() && !searching ? "Search" : search + (searching && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        Draw.text(g, st, px + 7, winY + pad + 5, c(search.isEmpty() ? 0x4E4D62 : 0xFFFFFF, 1f));
        Draw.text(g, ms, px + pw + 6, winY + pad + 5, c(0x5E5D72, 1f));
        add(SEARCH, px, winY + pad + 1, pw, 16, 0, null, null);

        // ---------- карточки ----------
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
        scroll += (scrollTarget - scroll) * .3f;

        int cols = cw >= 330 ? 3 : cw >= 215 ? 2 : 1, gap = 8;
        int colW = (cw - 6 - gap * (cols - 1)) / cols;
        float[] colY = new float[cols];
        g.enableScissor(cx, cy, cx + cw, cy + ch);
        for (Module m : list) {
            m.animOn += ((m.enabled ? 1f : 0f) - m.animOn) * .25f;
            int c0 = 0;
            for (int i = 1; i < cols; i++) if (colY[i] < colY[c0]) c0 = i;
            int x = cx + c0 * (colW + gap);
            int y = (int) (cy + colY[c0] - scroll);
            int h = cardH(m);
            if (y + h >= cy && y <= cy + ch) drawCard(g, m, x, y, colW, h, acc);
            colY[c0] += h + gap;
        }
        g.disableScissor();
        float total = 0;
        for (float v : colY) total = Math.max(total, v);
        maxScroll = Math.max(0, total - ch);
        if (maxScroll > 0) {
            int bh = Math.max(14, (int) (ch * (ch / total)));
            int by = cy + (int) ((ch - bh) * (scroll / maxScroll));
            Draw.rr(g, cx + cw - 3, by, 2, bh, 1, c(acc, .7f));
        }
        if (slider != null) setSlider(slider, mx);
    }

    private void drawCard(DrawContext g, Module m, int x, int y, int w, int h, int acc) {
        Draw.rr(g, x, y, w, h, 12, c(0x1D1C27, 1f));
        float k = m.animOn;
        Draw.rr(g, x + 8, y + 9, 8, 8, 4, c(RenderUtil.mix(0x46455C, acc, k), 1f));
        Draw.text(g, m.name, x + 21, y + 9, c(RenderUtil.mix(0x8F8EA6, 0xFFFFFF, k), 1f));

        int right = x + w - 8;
        if (m.action) {
            Draw.rr(g, right - 26, y + 7, 26, 12, 6, c(acc, .35f));
            Draw.text(g, "USE", right - 20, y + 9, c(0xFFFFFF, 1f));
            right -= 32;
        } else {
            Draw.rr(g, right - 22, y + 7, 22, 12, 6, c(RenderUtil.mix(0x3A3947, acc, k), 1f));
            Draw.rr(g, right - 21 + (int) (k * 10), y + 8, 10, 10, 5, c(0xE8E8F2, 1f));
            right -= 28;
        }
        add(TOGGLE, x, y, w, 26, 0, m, null);

        String bl = binding == m ? "..." : (m.key > 0 ? keyName(m.key) : "None");
        int bw = Draw.tw(bl) + 10;
        Draw.rr(g, right - bw, y + 7, bw, 12, 6, c(0x272634, 1f));
        Draw.text(g, bl, right - bw + 5, y + 9, c(binding == m ? 0xFFFFFF : 0x67667E, 1f));
        add(BIND, right - bw, y + 7, bw, 12, 0, m, null);

        int ry = y + 26;
        for (Setting s : m.settings) {
            if (s.bool) {
                Draw.text(g, s.name, x + 8, ry + 4, c(0xE0E0EC, 1f));
                box(g, x + w - 20, ry + 2, s.on(), acc);
                add(BOOL, x, ry, w, 16, 0, m, s);
                ry += 16;
            } else {
                String v = s.options != null ? s.options[Math.max(0, Math.min(s.options.length - 1, (int) Math.round(s.value)))] : String.format("%.1f", s.value);
                Draw.text(g, s.name, x + 8, ry + 3, c(0xE0E0EC, 1f));
                Draw.text(g, v, x + w - 8 - Draw.tw(v), ry + 3, c(0xE0E0EC, 1f));
                int tx = x + 8, tw = w - 16, ty = ry + 16;
                float f = (float) ((s.value - s.min) / (s.max - s.min));
                int fw = (int) (tw * f);
                Draw.rr(g, tx, ty, tw, 3, 1, c(0x2D2C3B, 1f));
                Draw.rr(g, tx, ty, Math.max(fw, 3), 3, 1, c(acc, 1f));
                Draw.rr(g, tx + fw - 3, ty - 2, 7, 7, 3, c(0xF2F2FA, 1f));
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

    // ------------------------------------------------------------------ input
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

    // ------------------------------------------------------------------ utils
    private int c(int rgb, float k) { return RenderUtil.alpha(rgb, Math.max(k * anim, .06f)); }

    private void scaled(DrawContext g, String t, float x, float y, float s, int argb) {
        g.getMatrices().push();
        g.getMatrices().translate(x, y, 0f);
        g.getMatrices().scale(s, s, 1f);
        Draw.text(g, t, 0, 0, argb);
        g.getMatrices().pop();
    }
    private void check(DrawContext g, int x, int y, int col) {
        Draw.line(g, x, y + 4, x + 2, y + 6, col); Draw.line(g, x + 2, y + 6, x + 7, y, col);
        Draw.line(g, x, y + 5, x + 2, y + 7, col); Draw.line(g, x + 2, y + 7, x + 7, y + 1, col);
    }
    private void cross(DrawContext g, int x, int y, int col) {
        Draw.line(g, x, y, x + 6, y + 6, col); Draw.line(g, x + 6, y, x, y + 6, col);
        Draw.line(g, x + 1, y, x + 7, y + 6, col); Draw.line(g, x + 7, y, x + 1, y + 6, col);
    }
    /** Круглая отметка вместо старых палочек. */
    private void box(DrawContext g, int x, int y, boolean on, int acc) {
        Draw.rr(g, x, y, 12, 12, 6, c(on ? acc : 0x2D2C3B, 1f));
        if (on) {
            Draw.line(g, x + 3, y + 6, x + 5, y + 8, c(0xFFFFFF, 1f));
            Draw.line(g, x + 5, y + 8, x + 9, y + 3, c(0xFFFFFF, 1f));
        } else {
            Draw.rr(g, x + 4, y + 4, 4, 4, 2, c(0x55546B, 1f));
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
            case VISUALS -> "Render";
            case PLAYER -> "Player";
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
