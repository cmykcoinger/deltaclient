package client;

import net.minecraft.client.MinecraftClient;

public class Sprint extends Module {
    private final Setting hungerCheck = add(Setting.bool("Check hunger", true));

    public Sprint() { super("Sprint", Category.MOVEMENT); enabled = true; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (mc.currentScreen != null || p.isSneaking() || p.horizontalCollision || p.isUsingItem()) return;
        if (hungerCheck.on() && p.getHungerManager().getFoodLevel() <= 6) return;
        if (mc.options.forwardKey.isPressed()) p.setSprinting(true);
    }
}
