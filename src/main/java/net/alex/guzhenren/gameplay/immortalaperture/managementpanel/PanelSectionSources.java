package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * 福地经营面板段登记表：{@link #ALL} 的下标即快照段与操作的线上类型序号，{@link #on} 按页取面板段（同页按 {@link #ALL}
 * 顺序）。
 * <ul>
 *   <li>信息页各小节的顺序（总览、养殖、种植、蛊虫、疆域）就是 {@link #ALL} 里这几个面板段的顺序。</li>
 * </ul>
 */

public final class PanelSectionSources {

    private PanelSectionSources() {}

    public static final List<PanelSectionSource<?>> ALL = List.of(MapPart.INSTANCE, StoragePart.INSTANCE,
            SecretRealmPart.INSTANCE, OverviewPart.INSTANCE, HerdPart.INSTANCE, FarmPart.INSTANCE,
            GuPart.INSTANCE, TerritoryPart.INSTANCE);
    private static final Map<Page, List<PanelSectionSource<?>>> BY_PAGE = byPage();

    private static Map<Page, List<PanelSectionSource<?>>> byPage() {
        Map<Page, List<PanelSectionSource<?>>> byPage = new EnumMap<>(Page.class);
        for (Page page : Page.values()) {
            byPage.put(page, ALL.stream().filter(module -> module.page() == page).toList());
        }
        return byPage;
    }

    public static @NotNull List<PanelSectionSource<?>> on(@NotNull Page page) { return BY_PAGE.get(page); }
}
