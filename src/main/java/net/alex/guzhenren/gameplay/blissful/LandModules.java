package net.alex.guzhenren.gameplay.blissful;

import java.util.List;
import net.alex.guzhenren.gameplay.blissful.farm.FarmModule;
import net.alex.guzhenren.gameplay.blissful.herd.HerdModule;
import net.alex.guzhenren.gameplay.blissful.member.OverviewModule;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainModule;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.storage.StorageModule;
import net.alex.guzhenren.gameplay.blissful.territory.TerritoryModule;
import org.jetbrains.annotations.NotNull;

/**
 * 福地经营模块登记表：{@link #ALL} 与 {@link Page} 一一对应、顺序相同（类加载时校验）。
 * <ul>
 *   <li>{@link #HOSTED} 是托管轮的调用顺序：种植先、养殖后，共用 {@link BlissfulLands#HOSTING_LIMIT}；各模块分开预算待
 *       Alex 定。</li>
 * </ul>
 */

public final class LandModules {

    private LandModules() {}

    public static final List<LandModule<?>> ALL = List.of(OverviewModule.INSTANCE, MountainModule.INSTANCE,
            HerdModule.INSTANCE, FarmModule.INSTANCE, TerritoryModule.INSTANCE, StorageModule.INSTANCE);
    public static final List<LandModule<?>> HOSTED = List.of(FarmModule.INSTANCE, HerdModule.INSTANCE);

    static {
        if (ALL.size() != Page.values().length) throw new IllegalStateException("one module per panel page");
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).page().ordinal() != i) throw new IllegalStateException("modules out of page order");
        }
    }

    public static @NotNull LandModule<?> of(@NotNull Page page) { return ALL.get(page.ordinal()); }
}
