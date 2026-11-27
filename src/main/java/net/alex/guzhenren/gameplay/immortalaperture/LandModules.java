package net.alex.guzhenren.gameplay.immortalaperture;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmModule;
import net.alex.guzhenren.gameplay.immortalaperture.gu.GuModule;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdModule;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapModule;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.Page;
import net.alex.guzhenren.gameplay.immortalaperture.member.OverviewModule;
import net.alex.guzhenren.gameplay.immortalaperture.soulsundermountain.MountainModule;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageModule;
import net.alex.guzhenren.gameplay.immortalaperture.territory.TerritoryModule;
import org.jetbrains.annotations.NotNull;

/**
 * 福地经营模块登记表：{@link #ALL} 的下标即快照段与操作的线上类型序号，{@link #on} 按页取模块（同页按 {@link #ALL}
 * 顺序）。
 * <ul>
 *   <li>信息页各小节的顺序（总览、养殖、种植、蛊虫、疆域）就是 {@link #ALL} 里这几个模块的顺序。</li>
 * </ul>
 */

public final class LandModules {

    private LandModules() {}

    public static final List<LandModule<?>> ALL = List.of(MapModule.INSTANCE, StorageModule.INSTANCE,
            MountainModule.INSTANCE, OverviewModule.INSTANCE, HerdModule.INSTANCE, FarmModule.INSTANCE,
            GuModule.INSTANCE, TerritoryModule.INSTANCE);
    private static final Map<Page, List<LandModule<?>>> BY_PAGE = byPage();

    private static Map<Page, List<LandModule<?>>> byPage() {
        Map<Page, List<LandModule<?>>> byPage = new EnumMap<>(Page.class);
        for (Page page : Page.values()) {
            byPage.put(page, ALL.stream().filter(module -> module.page() == page).toList());
        }
        return byPage;
    }

    public static @NotNull List<LandModule<?>> on(@NotNull Page page) { return BY_PAGE.get(page); }
}
