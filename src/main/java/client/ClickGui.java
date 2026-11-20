package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    private int dragX = -1, dragY = -1;
    private boolean dragging = false;
    private final List<Panel> panels = new ArrayList<>();
    private Module selectedModule = null;

    public ClickGui() { super(Text.literal("Mikger")); }
    @Override public boolean shouldPause() { return false; }

    private static class Panel {
        Category cat;
        int x, y;
        boolean dragging;
        int dragOffX, dragOffY;
        Panel(Category c, int x, int y) { this.cat = c; this.x = x; this.y = y; }
    }

    @Override
    protected void init() {
        if (panels.isEmpty()) {
            int startX = width / 2 - (Category.values().length * 130) / 2;
            for (Category c : Category.values()) {
                panels.add(new Panel(c, startX, height / 2 - 150));
                startX += 135;
            }
        }
    }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        // Фон (затемнение)
        g.fill(0, 0, width, height, 0x99000000);

        // Отрисовка панелей (колонок)
        for (Panel p : panels) {
            drawPanel(g, p, mx, my);
        }

        // Если выбран модуль - рисуем его настройки поверх (всплывающее окно)
        if (selectedModule != null) {
            drawSettings(g, mx, my);
        }

        super.render(g, mx, my, delta);
    }

    private void drawPanel(DrawContext g, Panel p, int mx, int my) {
        // Заголовок панели
        Draw.rr(g, p.x, p.y, 125, 18, 4, 0xFF1E1E24); // Темный фон
        Draw.rr(g, p.x, p.y, 125, 18, 4, RenderUtil.alpha(RenderUtil.THEMES[RenderUtil.theme][0], 0.2f)); // Акцентный оттенок
        Draw.text(g, p.cat.name(), p.x + 6, p.y + 5, 0xFFFFFFFF);

        // Модули внутри панели
        List<Module> mods = ModuleManager.byCategory(p.cat);
        int yOff = p.y + 22;
        for (Module m : mods) {
            // Фон модуля
            boolean hover = mx >= p.x && mx <= p.x + 125 && my >= yOff && my <= yOff + 16;
            int bg = hover ? 0xFF2A2A35 : 0xFF16161C;
            Draw.rr(g, p.x, yOff, 125, 16, 3, bg);

            // Текст модуля
            int color = m.enabled ? RenderUtil.THEMES[RenderUtil.theme][0] : 0xFFAAAAAA;
            Draw.text(g, m.name, p.x + 6, yOff + 4, color);

            // Индикатор (галочка/крестик)
            if (!m.settings.isEmpty()) {
                Draw.text(g, "...", p.x + 115, yOff + 4, 0xFF888888);
            }

            yOff += 18;
        }
        // Нижняя граница
        Draw.rr(g, p.x, yOff, 125, 2, 0, RenderUtil.THEMES[RenderUtil.theme][0]);
    }

    private void drawSettings(DrawContext g, int mx, int my) {
        int sw = 160, sh = 20 + selectedModule.settings.size() * 20;
        int sx = width / 2 - sw / 2, sy = height / 2 - sh / 2;
        
        Draw.rr(g, sx, sy, sw, sh, 6, 0xFF1E1E24);
        Draw.text(g, selectedModule.name, sx + 6, sy + 6, 0xFFFFFFFF);
        Draw.line(g, sx + 4, sy + 18, sx + sw - 4, sy + 18, 0xFF333333);

        int y = sy + 24;
        for (Setting s : selectedModule.settings) {
            Draw.text(g, s.name, sx + 6, y + 2, 0xFFAAAAAA);
            
            if (s.bool) {
                // Кнопка Вкл/Выкл
                String b = s.on() ? "ON" : "OFF";
                Draw.rr(g, sx + sw - 30, y, 24, 12, 3, s.on() ? 0xFFA855F7 : 0xFF333333);
                Draw.text(g, b, sx + sw - 26, y + 2, 0xFFFFFFFF);
            } else if (s.options != null) {
                // Режим (Mode)
                String mode = s.options[(int) Math.round(s.value)];
                Draw.text(g, mode, sx + sw - 60, y + 2, 0xFFA855F7);
            } else {
                // Число (Кнопки +/-)
                String val = String.format("%.1f", s.value);
                Draw.text(g, val, sx + sw - 40, y + 2, 0xFFA855F7);
                
                // Кнопки -
                Draw.rr(g, sx + sw - 25, y - 1, 10, 14, 2, 0xFF333333);
                Draw.text(g, "-", sx + sw - 22, y + 2, 0xFFFFFFFF);
                // Кнопки +
                Draw.rr(g, sx + sw - 12, y - 1, 10, 14, 2, 0xFF333333);
                Draw.text(g, "+", sx + sw - 9, y + 2, 0xFFFFFFFF);
            }
            y += 20;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Проверка клика по настройкам
        if (selectedModule != null) {
            int sw = 160, sh = 20 + selectedModule.settings.size() * 20;
            int sx = width / 2 - sw / 2, sy = height / 2 - sh / 2;
            int y = sy + 24;
            for (Setting s : selectedModule.settings) {
                if (mx >= sx + sw - 30 && mx <= sx + sw - 6 && my >= y && my <= y + 12) {
                    if (s.bool) s.toggle();
                } else if (!s.options.equals(null)) {
                    if (mx >= sx + sw - 25 && mx <= sx + sw - 15 && my >= y - 1 && my <= y + 13) {
                        s.value = Math.max(s.min, s.value - 0.1);
                    } else if (mx >= sx + sw - 12 && mx <= sx + sw - 2 && my >= y - 1 && my <= y + 13) {
                        s.value = Math.min(s.max, s.value + 0.1);
                    }
                }
                y += 20;
            }
            // Если кликнули мимо окна - закрываем
            if (mx < sx || mx > sx + sw || my < sy || my > sy + sh) {
                selectedModule = null;
            }
            return true;
        }

        // Проверка клика по панелям
        for (Panel p : panels) {
            // Заголовок (перетаскивание)
            if (mx >= p.x && mx <= p.x + 125 && my >= p.y && my <= p.y + 18) {
                p.dragging = true;
                p.dragOffX = (int) (mx - p.x);
                p.dragOffY = (int) (my - p.y);
                return true;
            }

            // Модули
            List<Module> mods = ModuleManager.byCategory(p.cat);
            int yOff = p.y + 22;
            for (Module m : mods) {
                if (mx >= p.x && mx <= p.x + 125 && my >= yOff && my <= yOff + 16) {
                    if (btn == 1) { // ПКМ - настройки
                        selectedModule = m;
                    } else {
                        m.trigger(); // ЛКМ - вкл/выкл
                    }
                    return true;
                }
                yOff += 18;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        for (Panel p : panels) {
            if (p.dragging) {
                p.x = (int) (mx - p.dragOffX);
                p.y = (int) (my - p.dragOffY);
                return true;
            }
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        for (Panel p : panels) p.dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(key, sc, mod);
    }
}
