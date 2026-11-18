package client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/** Ходьба, прыжок и бег с открытым инвентарём/контейнером и ClickGui. */
public class InvMove extends Module {
    public InvMove() { super("InvMove", Category.MOVEMENT); enabled = true; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof HandledScreen) && !(mc.currentScreen instanceof ClickGui)) return;
        long w = mc.getWindow().getHandle();
        KeyBinding[] keys = { mc.options.forwardKey, mc.options.backKey, mc.options.leftKey,
                mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey };
        for (KeyBinding k : keys) {
            int code = KeyBindingHelper.getBoundKeyOf(k).getCode();
            k.setPressed(InputUtil.isKeyPressed(w, code));
        }
    }
}
