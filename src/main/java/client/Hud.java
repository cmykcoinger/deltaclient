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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Hud {
    public static class Elem {
        final String name; public int x, y, w, h; public boolean on;
        Elem(String n, int w, int h) { name = n; this.w = w; this.h = h; }
    }
    public static final Elem INFO = new Elem("Info", 240, 31);
    public static final Elem TARGET = new Elem("Target-hud", 150, 36);
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
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void render(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        var win = mc.getWindow();
        int sw = win.getScaledWidth(), sh = win.getScaledHeight();
        if (!init) {
            INFO.x = sw / 2 - 120; INFO.y = 4;
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

        if (ui.info.on()) info(g, mc);
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

    // ---------------------------------------------------------------- info
    private static void info(DrawContext g, MinecraftClient mc) {
        int acc = RenderUtil.THEMES[RenderUtil.theme][0];
        int ping = 0;
        var nh = mc.getNetworkHandler();
        if (nh != null) { var e = nh.getPlayerListEntry(mc.player.getUuid()); if (e != null) ping = e.getLatency(); }
        String[] r1 = {RenderUtil.NAME, mc.getSession().getUsername(), mc.getCurrentFps() + " FPS", ping + " ms", LocalTime.now().format(TIME)};
        var p = mc.player;
        String[] r2 = {"x " + (int) Math.floor(p.getX()) + " y " + (int) Math.floor(p.getY()) + " z " + (int) Math.floor(p.getZ()),
                String.format("%.2f BPS", Interface.bps)};
        int w1 = rowW(r1), w2 = rowW(r2);
        INFO.w = Math.max(w1, w2); INFO.h = 31; INFO.on = true;
        row(g, r1, INFO.x + (INFO.w - w1) / 2, INFO.y, acc);
        row(g, r2, INFO.x + (INFO.w - w2) / 2, INFO.y + 17, acc);
    }
    private static int rowW(String[] r) { int w = 0; for (String s : r) w += Draw.chipW(s) + 4; return w - 4; }
    private static void row(DrawContext g, String[] r, int x, int y, int acc) {
        for (int i = 0; i < r.length; i++) {
            int col = (i == 0) ? (0xFF000000 | acc) : 0xFFFFFFFF;
            x += Draw.chip(g, x, y, r[i], acc, col) + 4;
        }
    }

    // ---------------------------------------------------------------- panels
    private static void panel(DrawContext g, Elem e, String title, List<String> left, List<String> right, boolean edit) {
        boolean has = !left.isEmpty();
        if (!has && !edit) return;
        e.on = true;
        int acc = RenderUtil.THEMES[RenderUtil.theme][0];
        int maxw = Draw.chipW(title);
        for (int i = 0; i < left.size(); i++) maxw = Math.max(maxw, Draw.tw(left.get(i)) + Draw.tw(right.get(i)) + 24);
        Draw.chip(g, e.x, e.y, title, acc, 0xFFFFFFFF);
        int y = e.y + 17;
        if (has) {
            Draw.rr(g, e.x, y, maxw, left.size() * 11 + 6, 9, 0xC8141420);
            for (int i = 0; i < left.size(); i++) {
                Draw.text(g, left.get(i), e.x + 7, y + 4 + i * 11, 0xFFE6E6F2);
                Draw.text(g, right.get(i), e.x + maxw - 7 - Draw.tw(right.get(i)), y + 4 + i * 11, 0xFF000000 | acc);
            }
        }
        e.w = maxw; e.h = 17 + (has ? left.size() * 11 + 6 : 0);
    }

    private static void potions(DrawContext g, MinecraftClient mc, boolean edit) {
        List<String> l = new ArrayList<>(), r = new ArrayList<>();
        for (StatusEffectInstance ef : mc.player.getStatusEffects()) {
            String n = ef.getEffectType().value().getName().getString();
            if (ef.getAmplifier() > 0) n += " " + (ef.getAmplifier() + 1);
            int s = ef.getDuration() / 20;
            l.add(n); r.add(ef.isInfinite() ? "inf" : (s / 60) + ":" + String.format("%02d", s % 60));
        }
        panel(g, POTIONS, "Potion-list", l, r, edit);
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
        panel(g, HOTKEYS, "Hot-keys", l, r, edit);
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
        panel(g, ENV, "Окружение", l, r, edit);
    }

    // ---------------------------------------------------------------- target hud
    private static void targetHud(DrawContext g, MinecraftClient mc) {
        LivingEntity t = TargetInfo.target;
        if (t == null) return;
        if (t != lastT) { shownHp = t.getHealth(); lastT = t; }
        float maxHp = Math.max(1f, t.getMaxHealth());
        float hp = t.getHealth() + t.getAbsorptionAmount();
        shownHp += (hp - shownHp) * 0.15f;
        float ratio = MathHelper.clamp(shownHp / maxHp, 0, 1);
        int acc = RenderUtil.THEMES[RenderUtil.theme][0];

        int x = TARGET.x, y = TARGET.y, w = TARGET.w, h = TARGET.h;
        TARGET.on = true;
        Draw.rr(g, x - 1, y - 1, w + 2, h + 2, 12, 0x33000000);
        Draw.rr(g, x, y, w, h, 11, 0xE6141420);
        if (t instanceof AbstractClientPlayerEntity pl) {
            PlayerSkinDrawer.draw(g, pl.getSkinTextures(), x + 6, y + 6, 24);
        } else {
            Draw.rr(g, x + 6, y + 6, 24, 24, 12, 0xFF1E1E2A);
            Draw.text(g, t.getName().getString().substring(0, 1), x + 16, y + 14, 0xFFFFFFFF);
        }
        Draw.text(g, t.getName().getString(), x + 36, y + 8, 0xFFFFFFFF);
        Draw.text(g, Math.round(mc.player.distanceTo(t)) + "m  A" + t.getArmor(), x + 36, y + 20, 0xFF7A7A92);

        int cx = x + w - 20, cy = y + h / 2;
        for (int d = 0; d < 360; d += 5) {
            double a = Math.toRadians(d);
            int px = cx + (int) Math.round(Math.sin(a) * 11), py = cy - (int) Math.round(Math.cos(a) * 11);
            g.fill(px, py, px + 2, py + 2, d / 360f <= ratio ? (0xFF000000 | acc) : 0xFF2A2A38);
        }
        String num = String.valueOf(Math.round(hp));
        Draw.text(g, num, cx + 1 - Draw.tw(num) / 2, cy - 3, 0xFFFFFFFF);
        g.fill(x + 8, y + h - 1, x + w - 8, y + h, 0xCC000000 | (acc & 0xFFFFFF));
    }

    // ---------------------------------------------------------------- items / armor / keys
    private static void items(DrawContext g, MinecraftClient mc) {
        int acc = RenderUtil.THEMES[RenderUtil.theme][0];
        var inv = mc.player.getInventory();
        int x = ITEMS.x;
        for (Item it : COUNT_ITEMS) {
            int n = 0;
            for (int i = 0; i < inv.size(); i++) { ItemStack s = inv.getStack(i); if (s.isOf(it)) n += s.getCount(); }
            String t = n > 0 ? String.valueOf(n) : "N/A";
            int w = 30 + Draw.tw(t);
            Draw.rr(g, x - 1, ITEMS.y - 1, w + 2, 18, 9, 0x33000000);
            Draw.rr(g, x, ITEMS.y, w, 16, 8, 0xE6141420);
            g.getMatrices().push();
            g.getMatrices().translate(x + 3f, ITEMS.y + 2f, 0f);
            g.getMatrices().scale(0.75f, 0.75f, 1f);
            g.drawItem(new ItemStack(it), 0, 0);
            g.getMatrices().pop();
            g.fill(x + 17, ITEMS.y + 3, x + 18, ITEMS.y + 13, 0xFF30303C);
            Draw.text(g, t, x + 22, ITEMS.y + 4, n > 0 ? 0xFFFFFFFF : 0xFF7A7A92);
            g.fill(x + 6, ITEMS.y + 15, x + w - 6, ITEMS.y + 16, 0xCC000000 | (acc & 0xFFFFFF));
            x += w + 4;
        }
        ITEMS.w = x - ITEMS.x - 4; ITEMS.h = 16; ITEMS.on = true;
    }

    private static void armor(DrawContext g, MinecraftClient mc) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        Draw.rr(g, ARMOR.x - 1, ARMOR.y - 1, ARMOR.w + 2, ARMOR.h + 2, 11, 0x33000000);
        Draw.rr(g, ARMOR.x, ARMOR.y, ARMOR.w, ARMOR.h, 10, 0xE6141420);
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getEquippedStack(slots[i]);
            int ix = ARMOR.x + 4 + i * 20, iy = ARMOR.y + 3;
            if (s.isEmpty()) { Draw.rr(g, ix + 2, iy + 2, 12, 12, 6, 0xFF22222E); continue; }
            g.drawItem(s, ix, iy);
            if (s.getMaxDamage() > 0) {
                float f = 1f - s.getDamage() / (float) s.getMaxDamage();
                g.fill(ix, iy + 17, ix + 16, iy + 19, 0xFF22222E);
                g.fill(ix, iy + 17, ix + (int) (16 * f), iy + 19, RenderUtil.mix(0xFF4444, 0x44FF66, f));
            }
        }
        ARMOR.on = true;
    }

    private static void keys(DrawContext g, MinecraftClient mc) {
        int acc = RenderUtil.THEMES[RenderUtil.theme][0];
        var o = mc.options;
        int x = KEYS.x, y = KEYS.y;
        key(g, x + 18, y, 16, "W", o.forwardKey.isPressed(), acc);
        key(g, x, y + 18, 16, "A", o.leftKey.isPressed(), acc);
        key(g, x + 18, y + 18, 16, "S", o.backKey.isPressed(), acc);
        key(g, x + 36, y + 18, 16, "D", o.rightKey.isPressed(), acc);
        keyW(g, x, y + 36, 25, "LMB", o.attackKey.isPressed(), acc);
        keyW(g, x + 27, y + 36, 25, "RMB", o.useKey.isPressed(), acc);
        KEYS.on = true;
    }
    private static void key(DrawContext g, int x, int y, int s, String t, boolean on, int acc) { keyW(g, x, y, s, t, on, acc); }
    private static void keyW(DrawContext g, int x, int y, int w, String t, boolean on, int acc) {
        Draw.rr(g, x, y, w, 16, 8, on ? (0xCC000000 | (acc & 0xFFFFFF)) : 0xC8141420);
        Draw.text(g, t, x + (w - Draw.tw(t)) / 2, y + 4, on ? 0xFF000000 : 0xFFB0B0C4);
    }

    // ---------------------------------------------------------------- editor (чат открыт)
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
            int c = e == dragging ? (0xFF000000 | RenderUtil.THEMES[RenderUtil.theme][0]) : 0xAAFFFFFF;
            g.drawBorder(e.x - 1, e.y - 1, e.w + 2, e.h + 2, c);
        }
    }
}
