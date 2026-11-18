package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.function.Predicate;

public class ItemUtil {
    /** Использует предмет: сначала ищет в хотбаре, потом в инвентаре. Возвращает false, если не нашёл. */
    public static boolean use(Predicate<ItemStack> match) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        PlayerInventory inv = p.getInventory();

        for (int i = 0; i < 9; i++) {
            if (!match.test(inv.getStack(i))) continue;
            int prev = inv.selectedSlot;
            inv.selectedSlot = i;
            mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            inv.selectedSlot = prev;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prev));
            return true;
        }
        for (int j = 9; j < 36; j++) {
            if (!match.test(inv.getStack(j))) continue;
            int sel = inv.selectedSlot;
            int sync = p.playerScreenHandler.syncId;
            mc.interactionManager.clickSlot(sync, j, sel, SlotActionType.SWAP, p);   // предмет в руку
            mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            mc.interactionManager.clickSlot(sync, j, sel, SlotActionType.SWAP, p);   // обратно
            return true;
        }
        return false;
    }
}
