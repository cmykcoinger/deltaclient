package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Hud {
    public static void render(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;

        // Ватермарка
        String wm = "Delta  |  " + mc.getCurrentFps() + " fps";
        int w = mc.textRenderer.getWidth(wm) + 10;
        g.fill(4, 4, 4 + w, 18, RenderUtil.alpha(0x000000, 0.55f));
        g.fill(4, 4, 6, 18, RenderUtil.rainbow(0f));
        g.drawTextWithShadow(mc.textRenderer, wm, 10, 8, RenderUtil.rainbow(0.1f));

        // Список включённых модулей справа
        List<Module> on = new ArrayList<>();
        for (Module m : ModuleManager.modules) if (m.enabled) on.add(m);
        on.sort(Comparator.comparingInt((Module m) -> -mc.textRenderer.getWidth(m.name)));
        int y = 4, sw = mc.getWindow().getScaledWidth(), i = 0;
        for (Module m : on) {
            int tw = mc.textRenderer.getWidth(m.name);
            g.fill(sw - tw - 10, y, sw, y + 11, RenderUtil.alpha(0x000000, 0.5f));
            g.fill(sw - 2, y, sw, y + 11, RenderUtil.rainbow(i * 0.06f));
            g.drawTextWithShadow(mc.textRenderer, m.name, sw - tw - 6, y + 2, RenderUtil.rainbow(i * 0.06f));
            y += 11; i++;
        }
    }
}
