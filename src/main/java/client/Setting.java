package client;

public class Setting {
    public final String name;
    public double value, min, max;
    public final boolean bool;
    public String[] options;

    private Setting(String n, double v, double mi, double ma, boolean b) { name = n; value = v; min = mi; max = ma; bool = b; }
    public static Setting num(String n, double v, double min, double max) { return new Setting(n, v, min, max, false); }
    public static Setting bool(String n, boolean v) { return new Setting(n, v ? 1 : 0, 0, 1, true); }
    /** Выбор из списка (на слайдере показывается название). */
    public static Setting mode(String n, int def, String... opts) {
        Setting s = new Setting(n, def, 0, opts.length - 1, false);
        s.options = opts;
        return s;
    }
    public boolean on() { return value > 0.5; }
    public void toggle() { value = on() ? 0 : 1; }
}
