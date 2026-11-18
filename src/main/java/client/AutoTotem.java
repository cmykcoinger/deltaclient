package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** Держит тотем в левой руке. */
public class AutoTotem extends Module {
    private final Setting always = add(Setting.bool("Always", true));
    private final Setting health = add(Setting.num("Health below", 10, 2, 20));
    private final Setting delay  = add(Setting.num("Delay (ticks)", 1, 0, 10));
    private int timer;

    public AutoTotem() { super("AutoTotem", Category.COMBAT); }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (mc.currentScreen instanceof HandledScreen && !(mc.currentScreen instanceof InventoryScreen)) return;
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (!always.on() && p.getHealth() + p.getAbsorptionAmount() > health.value) return;
        if (timer > 0) { timer--; return; }

        int slot = -1;
        for (int i = 9; i < 36 && slot < 0; i++) if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) slot = i;
        for (int i = 0; i < 9 && slot < 0; i++) if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) slot = 36 + i;
        if (slot < 0) return;

        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
        timer = (int) delay.value;
    }
}
