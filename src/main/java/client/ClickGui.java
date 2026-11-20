package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClickGui extends Screen {
    private static int selected = -1;
    private static int ox, oy;
    private static float scroll, scrollTarget, maxScroll;
    
    private final List<Hit> hits = new ArrayList<>();
    private Module binding;
    private boolean dragging;
    private int contentY, contentH;

    public ClickGui() { super(Text.literal("Mikger Client")); }
    @Override public boolean shouldPause() { return false; }

    private static class Hit { int x, y, w, h, type; Module m; Setting s; }
    private static final int CAT = 0, TOGGLE = 1, BIND = 2, BOOL = 3, NUM_MINUS = 4, NUM_PLUS = 5, MODE = 6, HEADER = 7;

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        int pad = 8;
        int winW = Math.min((int) (width * 0.92f), 680);
        int winH = Math.min((int) (height * 0.90f), 405);
        int winX = (width - winW) / 2 + ox;
        int winY = (height - winH) / 2 + oy;

        // Фон (полупрозрачный)
        g.fill(0, 0, width, height, 0x88000000);
        // Окно GUI
        g.fill(winX - 2, winY - 2, winX + winW + 2, winY + winH + 2, 0xFF1E1E24); // Рамка
        g.fill(winX, winY, winX + winW, winY + winH, 0xFF121216); // Фон

        hits.clear();
        add(HEADER, winX, winY, winW, 30, 0, null, null);

        // Левая панель (категории)
        int sbW = 120;
        int sx = winX + pad, sy = winY + pad, sh = winH - 2 * pad;
        g.fill(sx, sy, sx + sbW, sy + sh, 0xFF1A1A22);
        
        g.drawTextWithShadow(textRenderer, "Mikger Client", sx + 10, sy + 10, 0xFFA855F7);
        g.drawTextWithShadow(textRenderer, "Categories", sx + 10, sy + 26, 0xFF888888);

        Category[] cats = Category.values();
        int step = 20;
        for (int i = -1; i < cats.length; i++) {
            int iy = sy + 40 + (i + 1) * step;
            String nm = i < 0 ? "All Modules" : disp(cats[i]);
            boolean sel = selected == i;
            boolean hov = in(mx, my, sx + 6, iy, sbW - 12, 18);

            if (sel) g.fill(sx + 6, iy, sx + sbW - 6, iy + 18, 0xFF252535);
            else if (hov) g.fill(sx + 6, iy, sx + sbW - 6, iy + 18, 0xFF1E1E24);

            g.drawTextWithShadow(textRenderer, nm, sx + 12, iy + 5, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
            add(CAT, sx + 6, iy, sbW - 12, 18, i, null, null);
        }

        // Основная область (модули)
        int cx = sx + sbW + pad, cy = winY + 40, cw = winW - sbW - 3 * pad, ch = winH - 40 - pad;
        contentY = cy; contentH = ch;
        g.fill(cx, cy, cx + cw, cy + ch, 0xFF0E0E12);
        
        g.drawTextWithShadow(textRenderer, selected < 0 ? "All Modules" : disp(cats[selected]), cx + 10, winY + 15, 0xFFFFFFFF);

        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.modules) {
            if (selected >= 0 && m.category != cats[selected]) continue;
            list.add(m);
        }
        list.sort(Comparator.comparing(m -> m.name.toLowerCase()));

        int cols = 2, gap = 8;
        int colW = (cw - gap) / cols;
        float[] colY = new float[cols];

        g.enableScissor(cx, cy, cx + cw, cy + ch);
        for (Module m : list) {
            int c0 = 0;
            for (int i = 1; i < cols; i++) if (colY[i] < colY[c0]) c0 = i;
            int x = cx + c0 * (colW + gap);
            int y = (int) (cy + colY[c0] - scroll);
            int h = cardH(m);
            if (y + h >= cy && y <= cy + ch) drawCard(g, m, x, y, colW, h);
            colY[c0] += h + gap;
        }
        g.disableScissor();

        float total = 0;
        for (float v : colY) total = Math.max(total, v);
        maxScroll = Math.max(0, total - ch);
    }

    private void drawCard(DrawContext g, Module m, int x, int y, int w, int h) {
        // Фон карточки
        g.fill(x, y, x + w, y + h, 0xFF1A1A22);
        if (m.enabled) {
            g.fill(x, y, x + 2, y + h, 0xFFA855F7); // Фиолетовая полоска слева
        }
        
        g.drawTextWithShadow(textRenderer, m.name, x + 10, y + 8, 0xFFFFFFFF);

        // Переключатель (Вкл/Выкл)
        int togX = x + w - 40, togY = y + 6;
        g.fill(togX, togY, togX + 30, togY + 14, m.enabled ? 0xFFA855F7 : 0xFF333333);
        g.drawTextWithShadow(textRenderer, m.enabled ? "ON" : "OFF", togX + (m.enabled ? 6 : 4), togY + 3, 0xFFFFFFFF);
        add(TOGGLE, x, y, w, 20, 0, m, null);

        // Бинд
        String bl = binding == m ? "..." : (m.key > 0 ? keyName(m.key) : "BIND");
        int bw = textRenderer.getWidth(bl) + 8;
        g.fill(togX - bw - 6, togY, togX - 6, togY + 14, 0xFF252535);
        g.drawTextWithShadow(textRenderer, bl, togX - bw - 2, togY + 3, 0xFFAAAAAA);
        add(BIND, togX - bw - 6, togY, bw, 14, 0, m, null);

        int ry = y + 22;
        for (Setting s : m.settings) {
            if (s.bool) {
                g.drawTextWithShadow(textRenderer, s.name, x + 10, ry + 4, 0xFFAAAAAA);
                // Квадратик для bool
                g.fill(x + w - 20, ry + 4, x + w - 10, ry + 14, s.on() ? 0xFFA855F7 : 0xFF333333);
                add(BOOL, x, ry, w, 16, 0, m, s);
                ry += 18;
            } else {
                // Числовые настройки (кнопки +/-)
                String v = s.options != null ? s.options[Math.max(0, Math.min(s.options.length - 1, (int) Math.round(s.value)))] : String.format("%.1f", s.value);
                g.drawTextWithShadow(textRenderer, s.name, x + 10, ry + 4, 0xFFAAAAAA);
                g.drawTextWithShadow(textRenderer, v, x + w - 45, ry + 4, 0xFFA855F7);
                
                // Кнопки - и +
                g.fill(x + w - 25, ry + 2, x + w - 15, ry + 14, 0xFF252535);
                g.drawTextWithShadow(textRenderer, "-", x + w - 21, ry + 4, 0xFFFFFFFF);
                add(NUM_MINUS, x + w - 25, ry + 2, 10, 12, 0, m, s);
                
                g.fill(x + w - 12, ry + 2, x + w - 2, ry + 14, 0xFF252535);
                g.drawTextWithShadow(textRenderer, "+", x + w - 9, ry + 4, 0xFFFFFFFF);
                add(NUM_PLUS, x + w - 12, ry + 2, 10, 12, 0, m, s);
                
                // Кнопка смены режима (если есть options)
                if (s.options != null) {
                    add(MODE, x + 10, ry, w - 40, 16, 0, m, s);
                }
                ry += 18;
            }
        }
    }

    private static int cardH(Module m) {
        int h = 22;
        for (Setting s : m.settings) h += 18;
        return h + 6;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            boolean content = h.type != CAT && h.type != HEADER;
            if (content && (my < contentY || my > contentY + contentH)) continue;
            
            switch (h.type) {
                case CAT -> { selected = h.type; scroll = 0; scrollTarget = 0; }
                case HEADER -> dragging = true;
                case TOGGLE -> { if (btn == 2) binding = h.m; else h.m.trigger(); }
                case BIND -> binding = h.m;
                case BOOL -> h.s.toggle();
                case NUM_MINUS -> h.s.value = Math.max(h.s.min, h.s.value - 0.1);
                case NUM_PLUS -> h.s.value = Math.min(h.s.max, h.s.value + 0.1);
                case MODE -> { if (h.s.options != null) h.s.value = (h.s.value + 1) % h.s.options.length; }
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

    @Override public boolean mouseReleased(double mx, double my, int btn) { dragging = false; return super.mouseReleased(mx, my, btn); }
    @Override public boolean mouseScrolled(double mx, double my, double hA, double vA) { scrollTarget -= (float) vA * 20f; return true; }
    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (binding != null) {
            binding.key = key == GLFW.GLFW_KEY_ESCAPE ? -1 : key;
            binding = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(key, sc, mod);
    }

    private void add(int type, int x, int y, int w, int h, int idx, Module m, Setting s) {
        Hit t = new Hit(); t.type = type; t.x = x; t.y = y; t.w = w; t.h = h; t.m = m; t.s = s; hits.add(t);
    }
    private static boolean in(double mx, double my, double x, double y, double w, double h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    private static String disp(Category c) { return c.name().charAt(0) + c.name().substring(1).toLowerCase(); }
    private static String keyName(int key) { String n = GLFW.glfwGetKeyName(key, 0); return n != null ? n.toUpperCase() : "K" + key; }
}
