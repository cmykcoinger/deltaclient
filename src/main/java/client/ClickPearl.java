package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** По кнопке бросает эндер-перл (хотбар, потом инвентарь). */
public class ClickPearl extends Module {
    public ClickPearl() { super("ClickPearl", Category.COMBAT, true); key = GLFW.GLFW_KEY_G; }

    @Override
    public void run() {
        if (!ItemUtil.use(s -> s.isOf(Items.ENDER_PEARL)))
            MinecraftClient.getInstance().player.sendMessage(Text.literal("§cНет эндер-перлов"), true);
    }
}
