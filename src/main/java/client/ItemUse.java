package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Server helper: использует предмет по названию (хотбар, потом инвентарь). */
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
        boolean ok = ItemUtil.use(s -> !s.isEmpty() && s.getName().getString().toLowerCase().contains(match));
        if (!ok) mc.player.sendMessage(Text.literal("§cНет предмета: " + name), true);
    }
}
