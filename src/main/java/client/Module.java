package client;

import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final Category category;
    public final boolean action;          // true = кнопка (выполняется по бинду), false = переключатель
    public boolean enabled, expanded, wasDown;
    public int key = -1;
    public final List<Setting> settings = new ArrayList<>();

    public Module(String name, Category category) { this(name, category, false); }
    public Module(String name, Category category, boolean action) { this.name = name; this.category = category; this.action = action; }

    protected Setting add(Setting s) { settings.add(s); return s; }

    public void trigger() {
        if (action) { run(); return; }
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void run() {}
    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender3D(Matrix4f m, float delta) {}
}
