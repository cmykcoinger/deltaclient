package client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    private static final int PW = 118, HEAD = 22, ROW = 18;
    private static final int HEADER = 0, MODULE = 1, BOOL = 2, SLIDER = 3;

    private static class Panel { Category cat; float x, y; boolean open = true; }
    private static class Hit { float x, y, w, h; int type; Panel p; Module m; Setting s; }

    private static final List<Panel> panels = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private Panel dragPanel;
    private Hit slider;
    private Module binding;
    private float anim = 0f;

    public ClickGui() {
        super(Text.literal("ClickGui"));
        if (panels.isEmpty()) {
            float x = 25;
            for (Category c : Category.values()) {
                Panel p = new Panel(); p.cat = c; p.x = x; p.y = 25; panels.add(p);
                x += PW + 12;
            }
        }
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        anim += (1f - anim) * 0.18f;
        g.fillGradient(0, 0, width, height, RenderUtil.alpha(0x000000, 0.55f * anim), RenderUtil.alpha(0x101030, 0.78f * anim));
        hits.clear();
        int accent = RenderUtil.rainbow(0f);

        for (Panel p : panels) {
            int px = (int) p.x;
            int y = (int) (p.y + (1f - anim) * -30f);
            g.fill(px - 1, y - 1, px + PW + 1, y + HEAD, RenderUtil.alpha(0x000000, 0.35f * anim)); // тень
            g.fillGradient(px, y, px + PW, y + HEAD, RenderUtil.alpha(accent, anim), RenderUtil.alpha(RenderUtil.rainbow(0.2f), anim));
            text(g, p.cat.name(), px + 7, y + 7, 0x000000, anim);
            text(g, p.open ? "-" : "+", px + PW - 12, y + 7, 0x000000, anim);
            add(HEADER, px, y, PW, HEAD, p, null, null);
            y += HEAD;
            if (!p.open) continue;

            for (Module m : ModuleManager.byCategory(p.cat)) {
                boolean hov = in(mx, my, px, y, PW, ROW);
                int bg = m.enabled ? RenderUtil.alpha(accent, 0.85f * anim)
                        : RenderUtil.alpha(hov ? 0x2c2c3a : 0x16161e, 0.93f * anim);
                g.fill(px, y, px + PW, y + ROW, bg);
                text(g, m.name, px + 7, y + 5, m.enabled ? 0x000000 : 0xE0E0E0, anim);

                String tag = binding == m ? "[...]" : (m.key > 0 ? "[" + keyName(m.key) + "]" : "");
                if (!tag.isEmpty()) text(g, tag, px + PW - 8 - client().textRenderer.getWidth(tag), y + 5, m.enabled ? 0x202020 : 0x8888AA, anim);
                else if (!m.settings.isEmpty()) text(g, m.expanded ? "<" : ">", px + PW - 11, y + 5, m.enabled ? 0x000000 : 0x888888, anim);

                add(MODULE, px, y, PW, ROW, p, m, null);
                y += ROW;

                if (m.expanded) {
                    for (Setting s : m.settings) {
                        g.fill(px, y, px + PW, y + ROW, RenderUtil.alpha(0x0c0c12, 0.96f * anim));
                        if (s.bool) {
                            text(g, s.name, px + 10, y + 5, 0xBBBBBB, anim);
                            g.fill(px + PW - 20, y + 4, px + PW - 9, y + 14,
                                    s.on() ? RenderUtil.alpha(accent, anim) : RenderUtil.alpha(0x34343e, anim));
                            add(BOOL, px, y, PW, ROW, p, m, s);
                        } else {
                            float k = (float) ((s.value - s.min) / (s.max - s.min));
                            g.fill(px, y, (int) (px + PW * k), y + ROW, RenderUtil.alpha(accent, 0.35f * anim));
                            text(g, s.name + ": " + String.format("%.1f", s.value), px + 10, y + 5, 0xBBBBBB, anim);
                            add(SLIDER, px, y, PW, ROW, p, m, s);
                        }
                        y += ROW;
                    }
                }
            }
            g.fill(px, y, px + PW, y + 2, RenderUtil.alpha(accent, anim));
        }
        text(g, "ЛКМ - вкл/использовать   ПКМ - настройки   СКМ - бинд", 8, height - 14, 0xAAAAAA, anim);
        if (slider != null) setSlider(slider, mx);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            switch (h.type) {
                case HEADER -> { if (btn == 0) dragPanel = h.p; else if (btn == 1) h.p.open = !h.p.open; }
                case MODULE -> {
                    if (btn == 0) h.m.trigger();
                    else if (btn == 1) h.m.expanded = !h.m.expanded;
                    else if (btn == 2) binding = h.m;
                }
                case BOOL -> h.s.toggle();
                case SLIDER -> { slider = h; setSlider(h, (float) mx); }
            }
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragPanel != null) { dragPanel.x += dx; dragPanel.y += dy; return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragPanel = null; slider = null;
        return super.mouseReleased(mx, my, btn);
    }

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

    private static net.minecraft.client.MinecraftClient client() { return net.minecraft.client.MinecraftClient.getInstance(); }

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
    private void setSlider(Hit h, float mx) {
        double k = MathHelper.clamp((mx - h.x) / h.w, 0, 1);
        h.s.value = Math.round((h.s.min + (h.s.max - h.s.min) * k) * 10) / 10.0;
    }
    private void add(int type, float x, float y, float w, float h, Panel p, Module m, Setting s) {
        Hit t = new Hit(); t.type = type; t.x = x; t.y = y; t.w = w; t.h = h; t.p = p; t.m = m; t.s = s; hits.add(t);
    }
    private static boolean in(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    private void text(DrawContext g, String t, int x, int y, int rgb, float a) {
        g.drawTextWithShadow(textRenderer, t, x, y, RenderUtil.alpha(rgb, Math.max(a, 0.05f)));
    }
}
