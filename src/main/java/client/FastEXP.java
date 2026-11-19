package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/** Быстрое кидание бутылочек опыта при зажатой ПКМ. */
public class FastEXP extends Module {
    private final Setting uses = add(Setting.num("Uses per tick", 2, 1, 4));

    public FastEXP() { super("FastEXP", Category.PLAYER); }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (mc.currentScreen != null || !mc.options.useKey.isPressed()) return;
        if (!p.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) return;
        for (int i = 0; i < (int) uses.value; i++) mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        p.swingHand(Hand.MAIN_HAND);
    }
}
