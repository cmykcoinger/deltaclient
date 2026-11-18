package client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ClientMain implements ClientModInitializer {
    private boolean rsDown;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) return;
            long w = mc.getWindow().getHandle();

            boolean rs = InputUtil.isKeyPressed(w, GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (rs && !rsDown && mc.currentScreen == null) mc.setScreen(new ClickGui());
            rsDown = rs;

            for (Module m : ModuleManager.modules) {
                if (m.key > 0) {
                    boolean d = InputUtil.isKeyPressed(w, m.key);
                    if (d && !m.wasDown && mc.currentScreen == null) m.trigger();
                    m.wasDown = d;
                }
                if (m.enabled) m.onTick();
            }
        });

        WorldRenderEvents.LAST.register(ctx -> {
            if (ctx.matrixStack() == null) return;
            var m = ctx.matrixStack().peek().getPositionMatrix();
            float delta = ctx.tickCounter().getTickDelta(false);
            for (Module mod : ModuleManager.modules) if (mod.enabled) mod.onRender3D(m, delta);
        });

        HudRenderCallback.EVENT.register(Hud::render);
    }
}
