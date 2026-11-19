package client;

/** Убирает лишнее с экрана. Эффекты применяются через миксины (client.mixin). */
public class Removals extends Module {
    public static Removals I;

    public final Setting fire  = add(Setting.bool("Fire overlay", true));
    public final Setting water = add(Setting.bool("Water overlay", true));
    public final Setting wall  = add(Setting.bool("Wall overlay", true));
    public final Setting glow  = add(Setting.bool("Glowing outline", true));

    public Removals() { super("Removals", Category.VISUALS); I = this; }

    public static boolean fire()  { return I != null && I.enabled && I.fire.on(); }
    public static boolean water() { return I != null && I.enabled && I.water.on(); }
    public static boolean wall()  { return I != null && I.enabled && I.wall.on(); }
    public static boolean glow()  { return I != null && I.enabled && I.glow.on(); }
}
