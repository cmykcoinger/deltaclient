package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** По бинду меняет предмет в левой руке: First <-> Second. Берёт из инвентаря или хотбара. */
public class AutoSwap extends Module {
    private static final String[] OPTS = {"Totem", "Sphere", "Golden apple", "Ench. apple", "Shield", "Ender pearl"};

    private final Setting first  = add(Setting.mode("First item", 0, OPTS));
    private final Setting second = add(Setting.mode("Second item", 1, OPTS));

    public AutoSwap() { super("AutoSwap", Category.COMBAT, true); key = GLFW.GLFW_KEY_H; }

    private static boolean match(ItemStack s, int opt) {
        if (s.isEmpty()) return false;
        return switch (opt) {
            case 0 -> s.isOf(Items.TOTEM_OF_UNDYING);
            case 1 -> { String n = s.getName().getString().toLowerCase(); yield n.contains("сфер") || n.contains("sphere"); }
            case 2 -> s.isOf(Items.GOLDEN_APPLE);
            case 3 -> s.isOf(Items.ENCHANTED_GOLDEN_APPLE);
            case 4 -> s.isOf(Items.SHIELD);
            default -> s.isOf(Items.ENDER_PEARL);
        };
    }

    @Override
    public void run() {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        int a = (int) Math.round(first.value), b = (int) Math.round(second.value);
        int want = match(p.getOffHandStack(), a) ? b : a;    // в руке First -> берём Second, иначе -> First

        int slot = -1;
        for (int i = 9; i < 36 && slot < 0; i++) if (match(p.getInventory().getStack(i), want)) slot = i;
        for (int i = 0; i < 9 && slot < 0; i++) if (match(p.getInventory().getStack(i), want)) slot = 36 + i;
        if (slot < 0) { p.sendMessage(Text.literal("§cНет в инвентаре: " + OPTS[want]), true); return; }

        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
    }
}
