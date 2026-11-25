package net.alex.guzhenren.compat;

import java.lang.reflect.Field;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Traveler's Titles 联动：给地名标题[Place Title]排队用，只读它此刻有没有群系或维度标题在显示。
 * <ul>
 *   <li>不引入编译期依赖：反射读 {@value #COMMON} 的静态字段 {@code titleManager}，再读其群系、维度两个渲染器的
 *       {@code titleTimer}；该计时在标题出现时置为淡入 + 停留 + 淡出刻数、逐刻减到 0，大于 0 即正在显示。</li>
 *   <li>它的标题画在自己的 GUI 图层 {@link #LAYER} 上，客户端可按图层名取消绘制。</li>
 *   <li>⚠ 未安装或反射失败（字段改名）时 {@link #isShowingTitle} 恒为 {@code false}，只记一条警告，地名标题照常立刻显
 *       示。</li>
 * </ul>
 */

public final class TravelersTitlesIntegration {

    public static final String MOD_ID = "travelerstitles";
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "overlay");
    static final String COMMON = "com.yungnickyoung.minecraft.travelerstitles.TravelersTitlesCommon";
    private static final String[] RENDERERS = { "biomeTitleRenderer", "dimensionTitleRenderer" };
    private static boolean resolved;
    private static @Nullable Field manager;
    private static Field @Nullable [] renderers;
    private static @Nullable Field timer;

    private TravelersTitlesIntegration() {}

    public static boolean isShowingTitle() {
        if (!resolved) resolve();
        if (manager == null || renderers == null || timer == null) return false;

        try {
            Object titles = manager.get(null);
            if (titles == null) return false;

            for (Field field : renderers) {
                Object renderer = field.get(titles);
                if (renderer != null && timer.getInt(renderer) > 0) return true;
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Guzhenren.LOGGER.warn("Cannot read Traveler's Titles state; place titles no longer wait for it", exception);
            manager = null;
            return false;
        }
    }

    private static void resolve() {
        resolved = true;
        if (!ModList.get().isLoaded(MOD_ID)) return;

        try {
            Field titleManager = Class.forName(COMMON).getField("titleManager");
            Class<?> managerType = titleManager.getType();
            Field[] fields = new Field[RENDERERS.length];
            for (int at = 0; at < RENDERERS.length; at++) fields[at] = managerType.getField(RENDERERS[at]);
            timer = fields[0].getType().getField("titleTimer");
            renderers = fields;
            manager = titleManager;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Guzhenren.LOGGER.warn("Traveler's Titles changed its fields; place titles no longer wait for it", exception);
        }
    }
}
