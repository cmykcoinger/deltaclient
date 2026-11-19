package client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;

/** Последняя цель: остаётся, пока не наведёшься на другую или она не пропадёт. */
public class TargetInfo {
    public static LivingEntity target;
    public static long lastSeen;

    public static void update(MinecraftClient mc) {
        LivingEntity c = null;
        if (mc.targetedEntity instanceof LivingEntity le && le != mc.player && !(le instanceof ArmorStandEntity)) c = le;
        if (c == null) c = KillAura.target != null ? KillAura.target : (TriggerBot.target != null ? TriggerBot.target : AimAssist.target);
        if (c != null) { target = c; lastSeen = System.currentTimeMillis(); }
        if (target != null && (target.isRemoved() || !target.isAlive() || mc.player.distanceTo(target) > 80)) target = null;
    }
}
