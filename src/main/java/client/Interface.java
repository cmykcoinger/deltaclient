package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Главный переключатель HUD + выбор элементов интерфейса и прозрачности. */
public class Interface extends Module {
    public static Interface I;
    public static double bps;
    private Vec3d last;
    private int lastTheme = -1;

    public final Setting colorTheme = add(Setting.num("Color theme", 0, 0, 4));
    public final Setting opacity    = add(Setting.num("Glass opacity", 0.72, 0.25, 0.95));
    public final Setting info       = add(Setting.bool("Info panel", true));
    public final Setting target     = add(Setting.bool("Target-hud", true));
    public final Setting potions    = add(Setting.bool("Potions", true));
    public final Setting cooldowns  = add(Setting.bool("Cooldowns", true));
    public final Setting hotkeys    = add(Setting.bool("Hot-keys", true));
    public final Setting env        = add(Setting.bool("Environment", true));
    public final Setting items      = add(Setting.bool("Items", true));
    public final Setting armor      = add(Setting.bool("Armor", true));
    public final Setting keys       = add(Setting.bool("Keys", true));
    public final Setting notif      = add(Setting.bool("Notifications", true));

    public Interface() { super("Interface", Category.VISUALS); enabled = true; I = this; }

    @Override
    public void onTick() {
        var p = MinecraftClient.getInstance().player;
        if (p == null) return;
        Vec3d pos = p.getPos();
        if (last != null) bps += (Math.hypot(pos.x - last.x, pos.z - last.z) * 20 - bps) * 0.2;
        last = pos;

        int v = (int) colorTheme.value;
        if (v != lastTheme && lastTheme != -1) RenderUtil.theme = v;
        else colorTheme.value = RenderUtil.theme;
        lastTheme = (int) colorTheme.value;
    }
}
