package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Hud {
    public static class Elem {
        final String name; public int x, y, w, h; public boolean on;
        Elem(String n, int w, int h) { name = n; this.w = w; this.h = h; }
    }
    public static final Elem INFO = new Elem("Island", 190, 16);
    public static final Elem TARGET = new Elem("Target-hud", 160, 40);
    public static final Elem POTIONS = new Elem("Potion-list", 90, 14);
    public static final Elem COOLDOWNS = new Elem("Cooldowns", 90, 14);
    public static final Elem HOTKEYS = new Elem("Hot-keys", 90, 14);
    public static final Elem ENV = new Elem("Environment", 90, 14);
    public static final Elem ITEMS = new Elem("Items", 130, 16);
    public static final Elem ARMOR = new Elem("Armor", 84, 26);
    public static final Elem KEYS = new Elem("Keys", 52, 52);
    private static final Elem[] ALL = {INFO, TARGET, POTIONS, COOLDOWNS, HOTKEYS, ENV, ITEMS, ARMOR, KEYS};

    private static final Item[] CD_ITEMS = {Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE, Items.SHIELD, Items.FIREWORK_ROCKET, Items.TRIDENT, Items.WIND_CHARGE};
    private static final Item[] COUNT_ITEMS = {Items.ENDER_PEARL, Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE};

    private static boolean init;
    private static float shownHp;
    private static LivingEntity lastT;
    private static Elem dragging;
    private static int offX, offY;
    private static boolean wasDown;

    private static int bgAlpha() {
        Interface ui = Interface.I;
        double op = ui != null ? ui.opacity.value : 0.82;
        return ((int) MathHelper.clamp(op * 255.0, 60, 248)) << 24;
    }

    private static int panelBg() {
        return bgAlpha() | 0x0E1320;
    }

    public static void render(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        var win = mc.getWindow();
        int sw = win.getScaledWidth(), sh = win.getScaledHeight();
        if (!init) {
            INFO.x = sw / 2 - 90; INFO.y = 6;
            TARGET.x = sw / 2 + 24; TARGET.y = sh / 2 + 24;
            ENV.x = 6; ENV.y = 40;
            POTIONS.x = sw / 2 - 150; POTIONS.y = 40;
            COOLDOWNS.x = sw - 110; COOLDOWNS.y = 40;
            HOTKEYS.x = sw - 110; HOTKEYS.y = 100;
            ITEMS.x = sw / 2 - 65; ITEMS.y = sh - 50;
            ARMOR.x = sw / 2 + 100; ARMOR.y = sh - 44;
            KEYS.x = 8; KEYS.y = sh - 64;
            init = true;
        }
        float delta = tc.getTickDelta(false);
        for (Module m : ModuleManager.modules) if (m.enabled) m.onRender2D(g, delta);

        for (Elem e : ALL) e.on = false;
        Interface ui = Interface.I;
        if (ui == null || !ui.enabled) return;
        boolean edit = mc.currentScreen instanceof ChatScreen;

        if (ui.info.on()) dynamicIsland(g, mc);
        if (ui.target.on()) targetHud(g, mc);
        if (ui.potions.on()) potions(g, mc, edit);
        if (ui.cooldowns.on()) cooldowns(g, mc, edit);
        if (ui.hotkeys.on()) hotkeys(g, edit);
        if (ui.env.on()) environment(g, mc, edit);
        if (ui.items.on()) items(g, mc);
        if (ui.armor.on()) armor(g, mc);
        if (ui.keys.on()) keys(g, mc);
        if (ui.notif.on()) Notifications.render(g);
    }

    private static void dynamicIsland(DrawContext g, MinecraftClient mc) {
        int acc = RenderUtil.THEMES[RenderUtil.theme][0] & 0xFFFFFF;
        int ping = 0;
        var nh = mc.getNetworkHandler();
        if (nh != null) {
            var e = nh.getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String label = "Rockstar  |  " + mc.getSession().getUsername() + "  |  " + ping + "ms  |  " + mc.getCurrentFps() + "fps";
        int w = Draw.tw(label) + 18;
        int h = 15;
        INFO.w = w;
        INFO.h = h;
        INFO.on = true;

        int x = INFO.x, y = INFO.y;
        Draw.rr(g, x, y, w, h, 6, panelBg());
        g.fill(x + 1, y + 3, x + 3, y + h - 3, 0xFF000000 | acc);
        Draw.text(g, label, x + 8, y + 4, 0xFFF2F6FF);
    }

    private static void panel(DrawContext g, Elem e, String title, List<String> left, List<String> right, boolean edit) {
        boolean has = !left.isEmpty();
        if (!has && !edit) return;
        e.on = true;
        int acc = RenderUtil.THEMES[RenderUtil.theme][0] & 0xFFFFFF;
        int maxw = Draw.tw(title) + 16;
        for (int i = 0; i < left.size(); i++) maxw = Math.max(maxw, Draw.tw(left.get(i)) + Draw.tw(right.get(i)) + 18);
        Draw.rr(g, e.x, e.y, maxw, 14, 6, panelBg());
        g.fill(e.x + 1, e.y + 3, e.x + 3, e.y + 11, 0xFF000000 | acc);
        Draw.text(g, title, e.x + 7, e.y + 3, 0xFFFFFFFF);
        int y = e.y + 16;
        if (has) {
            Draw.rr(g, e.x, y, maxw, left.size() * 11 + 6, 6, panelBg());
            for (int i = 0; i < left.size(); i++) {
                Draw.text(g, left.get(i), e.x + 7, y + 4 + i * 11, 0xFFE6ECF8);
                Draw.text(g, right.get(i), e.x + maxw - 7 - Draw.tw(right.get(i)), y + 4 + i * 11, 0xFF000000 | acc);
            }
        }
        e.w = maxw; e.h = 16 + (has ? left.size() * 11 + 6 : 0);
    }

    private static void potions(DrawContext g, MinecraftClient mc, boolean edit) {
        List<String> l = new ArrayList<>(), r = new ArrayList<>();
        for (StatusEffectInstance ef : mc.player.getStatusEffects()) {
            String n = ef.getEffectType().value().getName().getString();
            if (ef.getAmplifier() > 0) n += " " + (ef.getAmplifier() + 1);
            int s = ef.getDuration() / 20;
            l.add(n); r.add(ef.isInfinite() ? "inf" : (s / 60) + ":" + String.format("%02d", s % 60));
        }
        panel(g, POTIONS, "Potions", l, r, edit);
    }

    private static void cooldowns(DrawContext g, MinecraftClient mc, boolean edit) {
        List<String> l = new ArrayList<>(), r = new ArrayList<>();
        var cm = mc.player.getItemCooldownManager();
        for (Item it : CD_ITEMS) {
            ItemStack s = new ItemStack(it);
            float pr = cm.getCooldownProgress(s, 0f);
            if (pr > 0f) { l.add(s.getName().getString()); r.add(Math.round(pr * 100) + "%"); }
        }
        panel(g, COOLDOWNS, "Cooldowns", l, r, edit);
    }

    private static void hotkeys(DrawContext g, boolean edit) {
        List<String> l = new ArrayList<>(), r = new ArrayList<>();
        for (Module m : ModuleManager.modules) if (m.enabled && m.key > 0) { l.add(m.name); r.add(Draw.keyName(m.key)); }
        panel(g, HOTKEYS, "Binds", l, r, edit);
    }

    private static void environment(DrawContext g, MinecraftClient mc, boolean edit) {
        List<PlayerEntity> near = new ArrayList<>();
        for (PlayerEntity p : mc.world.getPlayers()) if (p != mc.player && mc.player.distanceTo(p) <= 64) near.add(p);
        near.sort((a, b) -> Float.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)));
        List<String> l = new ArrayList<>(), r = new ArrayList<>();
        for (int i = 0; i < Math.min(8, near.size()); i++) {
            l.add(near.get(i).getName().getString());
            r.add(Math.round(mc.player.distanceTo(near.get(i))) + "m");
        }
        panel(g, ENV, "Players", l, r, edit);
    }

    private static void targetHud(DrawContext g, MinecraftClient mc) {
        LivingEntity t = TargetInfo.target;
        if (t == null) return;
        if (t != lastT) { shownHp = t.getHealth(); lastT = t; }
        float maxHp = Math.max(1f, t.getMaxHealth());
        float hp = t.getHealth() + t.getAbsorptionAmount();
        shownHp += (hp - shownHp) * 0.16f;
        float ratio = MathHelper.clamp(shownHp / maxHp, 0, 1);
        int acc = RenderUtil.THEMES[RenderUtil.theme][0] & 0xFFFFFF;
        int acc2 = RenderUtil.THEMES[RenderUtil.theme][1] & 0xFFFFFF;

        int x = TARGET.x, y = TARGET.y, w = TARGET.w, h = TARGET.h;
        TARGET.on = true;
        Draw.rr(g, x, y, w, h, 8, panelBg());
        g.fill(x + 1, y + 4, x + 3, y + h - 4, 0xFF000000 | acc);

        if (t instanceof AbstractClientPlayerEntity pl) {
            PlayerSkinDrawer.draw(g, pl.getSkinTextures(), x + 7, y + 7, 24);
        } else {
            Draw.rr(g, x + 7, y + 7, 24, 24, 6, 0x88162034);
            Draw.text(g, t.getName().getString().substring(0, 1), x + 17, y + 15, 0xFFFFFFFF);
        }

        Draw.text(g, t.getName().getString(), x + 37, y + 7, 0xFFFFFFFF);
        Draw.text(g, String.format("%.1f HP  %dm", hp, Math.round(mc.player.distanceTo(t))), x + 37, y + 18, 0xFF9EB0CC);

        int barX = x + 37, barY = y + 30, barW = w - 45;
        Draw.rr(g, barX, barY, barW, 4, 2, 0x66222E44);
        Draw.rr(g, barX, barY, Math.max(4, (int) (barW * ratio)), 4, 2, 0xFF000000 | RenderUtil.mix(acc, acc2, ratio));
    }

    private static void items(DrawContext g, MinecraftClient mc) {
        int acc = RenderUtil.THEMES[RenderUtil.theme][0] & 0xFFFFFF;
        var inv = mc.player.getInventory();
        int x = ITEMS.x;
        for (Item it : COUNT_ITEMS) {
            int n = 0;
            for (int i = 0; i < inv.size(); i++) { ItemStack s = inv.getStack(i); if (s.isOf(it)) n += s.getCount(); }
            String t = n > 0 ? String.valueOf(n) : "0";
            int w = 28 + Draw.tw(t);
            Draw.rr(g, x, ITEMS.y, w, 15, 6, panelBg());
            g.getMatrices().push();
            g.getMatrices().translate(x + 3f, ITEMS.y + 2f, 0f);
            g.getMatrices().scale(0.75f, 0.75f, 1f);
            g.drawItem(new ItemStack(it), 0, 0);
            g.getMatrices().pop();
            Draw.text(g, t, x + 18, ITEMS.y + 4, n > 0 ? 0xFFFFFFFF : 0xFF7E8AA4);
            g.fill(x + 5, ITEMS.y + 14, x + w - 5, ITEMS.y + 15, 0xCC000000 | acc);
            x += w + 4;
        }
        ITEMS.w = x - ITEMS.x - 4; ITEMS.h = 15; ITEMS.on = true;
    }

    private static void armor(DrawContext g, MinecraftClient mc) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        Draw.rr(g, ARMOR.x, ARMOR.y, ARMOR.w, ARMOR.h, 7, panelBg());
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getEquippedStack(slots[i]);
            int ix = ARMOR.x + 4 + i * 20, iy = ARMOR.y + 3;
            if (s.isEmpty()) { Draw.rr(g, ix + 2, iy + 2, 12, 12, 4, 0x551A2438); continue; }
            g.drawItem(s, ix, iy);
            if (s.getMaxDamage() > 0) {
                float f = 1f - s.getDamage() / (float) s.getMaxDamage();
                g.fill(ix, iy + 17, ix + 16, iy + 19, 0x661A2438);
                g.fill(ix, iy + 17, ix + (int) (16 * f), iy + 19, RenderUtil.mix(0xFF4455, 0x44FF88, f));
            }
        }
        ARMOR.on = true;
    }

    private static void keys(DrawContext g, MinecraftClient mc) {
        int acc = RenderUtil.THEMES[RenderUtil.theme][0] & 0xFFFFFF;
        var o = mc.options;
        int x = KEYS.x, y = KEYS.y;
        keyW(g, x + 18, y, 16, "W", o.forwardKey.isPressed(), acc);
        keyW(g, x, y + 18, 16, "A", o.leftKey.isPressed(), acc);
        keyW(g, x + 18, y + 18, 16, "S", o.backKey.isPressed(), acc);
        keyW(g, x + 36, y + 18, 16, "D", o.rightKey.isPressed(), acc);
        keyW(g, x, y + 36, 25, "LMB", o.attackKey.isPressed(), acc);
        keyW(g, x + 27, y + 36, 25, "RMB", o.useKey.isPressed(), acc);
        KEYS.on = true;
    }

    private static void keyW(DrawContext g, int x, int y, int w, String t, boolean on, int acc) {
        Draw.rr(g, x, y, w, 16, 5, on ? (0xCC000000 | acc) : panelBg());
        Draw.text(g, t, x + (w - Draw.tw(t)) / 2, y + 4, on ? 0xFF060A12 : 0xFFC4D0E6);
    }

    public static void editor(DrawContext g, int mx, int my) {
        var mc = MinecraftClient.getInstance();
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            for (Elem e : ALL) if (e.on && mx >= e.x && mx <= e.x + e.w && my >= e.y && my <= e.y + e.h) {
                dragging = e; offX = mx - e.x; offY = my - e.y;
            }
        }
        if (!down) dragging = null;
        if (dragging != null) {
            var win = mc.getWindow();
            dragging.x = MathHelper.clamp(mx - offX, 0, win.getScaledWidth() - dragging.w);
            dragging.y = MathHelper.clamp(my - offY, 0, win.getScaledHeight() - dragging.h);
        }
        wasDown = down;
        for (Elem e : ALL) {
            if (!e.on) continue;
            int c = e == dragging ? (0xFF000000 | (RenderUtil.THEMES[RenderUtil.theme][0] & 0xFFFFFF)) : 0xAAFFFFFF;
            g.drawBorder(e.x - 1, e.y - 1, e.w + 2, e.h + 2, c);
        }
    }
}
