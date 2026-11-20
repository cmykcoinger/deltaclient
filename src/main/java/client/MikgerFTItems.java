package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class MikgerFTItems extends Module {
    private final Setting itemMode = add(Setting.mode("Item", 0,
            "Дезориентация", "Трапка", "Явная пыль", "Огненный смерч", "Божья аура"));
    private final Setting useBind = add(Setting.bool("Use bind", true));

    private boolean wasPressed;

    public MikgerFTItems() {
        super("MikgerFTItems", Category.COMBAT);
        key = GLFW.GLFW_KEY_H;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (!useBind.on()) return;

        boolean pressed = GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        if (pressed && !wasPressed) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        wasPressed = pressed;
    }
}
