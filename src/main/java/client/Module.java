package client;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final Category category;
    public final boolean action;
    public boolean enabled, expanded, wasDown;
    public float animOn, animExp;
    public int key = -1;
    public final List<Setting> settings = new ArrayList<>();

    public Module(String name, Category category) { this(name, category, false); }
    public Module(String name, Category category, boolean action) { this.name = name; this.category = category; this.action = action; }

    protected Setting add(Setting s) { settings.add(s); return s; }

    public void trigger() {
        if (action) { run(); return; }
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
        Notifications.push(name + (enabled ? " enabled" : " disabled"), enabled);
    }

    public void run() {}
    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    /** Вызывается каждый кадр (dt - секунды). */
    public void onFrame(float dt) {}
    public void onRender3D(Matrix4f m, float delta) {}
    public void onRender2D(DrawContext g, float delta) {}
}
