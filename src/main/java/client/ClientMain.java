package client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ClientMain implements ClientModInitializer {
    private boolean rsDown;
    private long lastFrame;

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
            TargetInfo.update(mc);
        });

        // Каждый кадр (плавная наводка)
        WorldRenderEvents.START.register(ctx -> {
            long now = System.nanoTime();
            float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
            lastFrame = now;
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;
            for (Module m : ModuleManager.modules) if (m.enabled) m.onFrame(dt);
        });

        WorldRenderEvents.LAST.register(ctx -> {
            if (ctx.matrixStack() == null) return;
            var m = ctx.matrixStack().peek().getPositionMatrix();
            float delta = ctx.tickCounter().getTickDelta(false);
            Draw.setMatrices(ctx.projectionMatrix(), m, ctx.camera().getPos(), delta);
            for (Module mod : ModuleManager.modules) if (mod.enabled) mod.onRender3D(m, delta);
        });

        HudRenderCallback.EVENT.register(Hud::render);

        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen instanceof ChatScreen)
                ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> Hud.editor(g, mx, my));
        });
    }
}
