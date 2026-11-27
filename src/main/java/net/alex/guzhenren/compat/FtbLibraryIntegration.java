package net.alex.guzhenren.compat;

import java.util.Arrays;
import net.alex.guzhenren.Guzhenren;
import net.neoforged.fml.ModList;

/**
 * FTB Library 联动：它给所有容器界面左上挂侧栏按钮组（FTB 任务、领地等），地灵面板不是背包，不该出现。
 * <ul>
 *   <li>不引入编译期依赖：反射调 {@value #API} 的 {@code get().addSidebarScreenBlacklist(String...)} 按界面类全名排除；
 *       它只比 {@code getClass().getName()}，子类不跟着排除，每个具体界面类都要传进来。</li>
 *   <li>⚠ 未安装或反射失败（接口改名）时只记一条警告，按钮照常出现。</li>
 * </ul>
 */

public final class FtbLibraryIntegration {

    public static final String MOD_ID = "ftblibrary";
    static final String API = "dev.ftb.mods.ftblibrary.api.client.FTBLibraryClientApi";

    private FtbLibraryIntegration() {}

    public static void hideSidebarOn(Class<?>... screens) {
        if (!ModList.get().isLoaded(MOD_ID)) return;

        String[] names = Arrays.stream(screens).map(Class::getName).toArray(String[]::new);
        try {
            Class<?> api = Class.forName(API);
            Object instance = api.getMethod("get").invoke(null);
            api.getMethod("addSidebarScreenBlacklist", String[].class).invoke(instance, (Object) names);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Guzhenren.LOGGER.warn("FTB Library changed its client API; its sidebar stays on the earth spirit panel",
                    exception);
        }
    }
}
