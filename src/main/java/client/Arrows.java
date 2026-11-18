package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Стрелки вокруг прицела, указывающие, где находятся игроки. */
public class Arrows extends Module {
    private final Setting radius = add(Setting.num("Radius", 60, 30, 140));
    private final Setting range  = add(Setting.num("Range", 100, 10, 200));

    public Arrows() { super("Arrows", Category.VISUALS); }

    @Override
    public void onRender2D(DrawContext g, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var win = mc.getWindow();
        int cx = win.getScaledWidth() / 2, cy = win.getScaledHeight() / 2;
        Vec3d me = mc.player.getLerpedPos(delta);

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double dist = mc.player.distanceTo(p);
            if (dist > range.value) continue;
            Vec3d d = p.getLerpedPos(delta).subtract(me);
            float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
            float diff = (float) Math.toRadians(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
            float dx = (float) Math.sin(diff), dy = (float) -Math.cos(diff);

            int tipX = (int) (cx + dx * radius.value), tipY = (int) (cy + dy * radius.value);
            float bx = tipX - dx * 9, by = tipY - dy * 9;
            int lx = (int) (bx - dy * 5), ly = (int) (by + dx * 5);
            int rx = (int) (bx + dy * 5), ry = (int) (by - dx * 5);
            int col = 0xFF000000 | RenderUtil.mix(0xFF4444, 0x44FF66, (float) (dist / range.value));

            Draw.line(g, tipX, tipY, lx, ly, col);
            Draw.line(g, tipX, tipY, rx, ry, col);
            Draw.line(g, lx, ly, (int) bx, (int) by, col);
            Draw.line(g, rx, ry, (int) bx, (int) by, col);
            String t = Math.round(dist) + "m";
            Draw.text(g, t, (int) (tipX + dx * 8) - Draw.tw(t) / 2, (int) (tipY + dy * 8) - 4, 0xFFFFFFFF);
        }
    }
}
