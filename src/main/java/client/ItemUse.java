package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/** Server helper: по кнопке находит предмет в хотбаре по названию, использует его и возвращает слот. */
public class ItemUse extends Module {
    private final String match;

    public ItemUse(String title, String match, int key) {
        super(title, Category.SERVER, true);
        this.match = match.toLowerCase();
        this.key = key;
    }

    @Override
    public void run() {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty() || !s.getName().getString().toLowerCase().contains(match)) continue;
            int prev = inv.selectedSlot;
            inv.selectedSlot = i;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            inv.selectedSlot = prev;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prev));
            return;
        }
        mc.player.sendMessage(Text.literal("§cНет в хотбаре: " + name), true);
    }
}
