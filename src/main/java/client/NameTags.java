package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Ники над игроками, видны через стены. Легко для FPS: лимит по дальности и количеству. */
public class NameTags extends Module {
    private final Setting range  = add(Setting.num("Range", 80, 10, 200));
    private final Setting scale  = add(Setting.num("Scale", 1.0, 0.5, 2.0));
    private final Setting max    = add(Setting.num("Max tags", 24, 4, 64));
    private final Setting hp     = add(Setting.bool("Health", true));
    private final Setting dist   = add(Setting.bool("Distance", true));

    public NameTags() { super("NameTags", Category.VISUALS); enabled = true; }

    @Override
    public void onRender2D(DrawContext g, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int count = 0;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double d = mc.player.distanceTo(p);
            if (d > range.value) continue;
            float[] s = Draw.project(p.getLerpedPos(Draw.delta).add(0, p.getHeight() + 0.45, 0));
            if (s == null) continue;
            if (++count > (int) max.value) break;

            String name = p.getName().getString();
            String hpS = hp.on() ? " " + Math.round(p.getHealth() + p.getAbsorptionAmount()) : "";
            String dS = dist.on() ? " " + Math.round(d) + "m" : "";
            float ratio = MathHelper.clamp(p.getHealth() / Math.max(1f, p.getMaxHealth()), 0, 1);
            int hpCol = RenderUtil.mix(0xFF4444, 0x44FF66, ratio);

            float sc = (float) scale.value * MathHelper.clamp(1.25f - (float) d / 70f, 0.6f, 1.25f);
            int w = Draw.tw(name) + Draw.tw(hpS) + Draw.tw(dS);
            g.getMatrices().push();
            g.getMatrices().translate(s[0], s[1], 0);
            g.getMatrices().scale(sc, sc, 1f);
            Draw.rr(g, -w / 2 - 4, -3, w + 8, 13, 3, 0xA0101018);
            g.fill(-w / 2 - 4, 9, -w / 2 - 4 + (int) ((w + 8) * ratio), 10, 0xFF000000 | hpCol);
            int x = -w / 2;
            Draw.text(g, name, x, 0, 0xFFFFFFFF); x += Draw.tw(name);
            if (!hpS.isEmpty()) { Draw.text(g, hpS, x, 0, 0xFF000000 | hpCol); x += Draw.tw(hpS); }
            if (!dS.isEmpty()) Draw.text(g, dS, x, 0, 0xFFB0B0C0);
            g.getMatrices().pop();
        }
    }
}
