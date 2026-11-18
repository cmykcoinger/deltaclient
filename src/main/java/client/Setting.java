package client;

public class Setting {
    public final String name;
    public double value, min, max;
    public final boolean bool;

    private Setting(String n, double v, double mi, double ma, boolean b) { name = n; value = v; min = mi; max = ma; bool = b; }
    public static Setting num(String n, double v, double min, double max) { return new Setting(n, v, min, max, false); }
    public static Setting bool(String n, boolean v) { return new Setting(n, v ? 1 : 0, 0, 1, true); }
    public boolean on() { return value > 0.5; }
    public void toggle() { value = on() ? 0 : 1; }
}
