package client;

import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.add(new AimAssist());
        modules.add(new TriggerBot());
        modules.add(new Sprint());
        modules.add(new InvMove());
        modules.add(new TargetESP());
        modules.add(new Trajectories());

        // Server helper: поиск предмета в хотбаре по части названия, бинд меняется СКМ в GUI
        modules.add(new ItemUse("Дезориентация", "дезориентация", GLFW.GLFW_KEY_Z));
        modules.add(new ItemUse("Трапка", "трап", GLFW.GLFW_KEY_X));
        modules.add(new ItemUse("Явная пыль", "явная пыль", GLFW.GLFW_KEY_C));
        modules.add(new ItemUse("Огненный смерч", "огненный смерч", GLFW.GLFW_KEY_V));
        modules.add(new ItemUse("Пласт", "пласт", GLFW.GLFW_KEY_B));
        modules.add(new ItemUse("Снежок заморозка", "заморозка", GLFW.GLFW_KEY_N));
    }

    public static List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }
}
