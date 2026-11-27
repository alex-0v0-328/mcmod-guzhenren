package net.alex.guzhenren.gameplay.immortalaperture;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.Page;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelAction;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelContext;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 福地的一个经营模块，即地灵面板某一页里一段的服务端：这段显示数据、这段的操作、可选的托管一步。
 * <ul>
 *   <li>{@link #page} 是这段落在哪一页；同页多个模块按 {@link LandModules#ALL} 的顺序各出一段。加一种经营玩法 = 新子包
 *       里写一个实现、在 {@link LandModules#ALL} 末尾登记、指定所在的 {@link Page}，再写客户端绘制；面板路由、快照、操作
 *       编码都不用改。</li>
 *   <li>{@link #section} 只在有人开着所在页时每秒算一次，可以扫描实体与方块；清点优先走 {@link LandCensus}。段记录的
 *       {@code module()} 返回本模块的 {@code INSTANCE}。</li>
 *   <li>{@link #opened}：主人每次打开面板时调用，按人记着的页面状态在这里回到初始（库藏清搜索与页码）。</li>
 *   <li>托管不在这里：各功能的托管任务是 {@code LandHostingJob}，由组装层排序调用。</li>
 * </ul>
 */

public interface LandModule<S extends PanelSection> {

    @NotNull Page page();

    @NotNull StreamCodec<RegistryFriendlyByteBuf, S> sectionCodec();

    @NotNull S section(@NotNull PanelContext context);

    @NotNull List<PanelAction.Type<?>> actions();

    default void opened(@NotNull UUID viewer) {}
}
