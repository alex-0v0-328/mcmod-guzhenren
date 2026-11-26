package net.alex.guzhenren.gameplay.blissful;

import java.util.List;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelContext;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地的一个经营模块，即地灵面板一页的服务端：本页显示数据、本页操作、可选的托管一步。
 * <ul>
 *   <li>加一种经营玩法 = 新子包里写一个实现、在 {@link LandModules#ALL} 末尾登记、{@link Page} 末尾加一页，再写客户端
 *       页面；面板路由、快照、操作编码都不用改。</li>
 *   <li>{@link #section} 只在有人开着本页时每秒算一次，可以扫描实体与方块；清点优先走 {@link LandCensus}。</li>
 *   <li>{@link #host} 在托管轮里按 {@link LandModules#HOSTED} 的顺序调用；每个模块用自己的预算（种植按田、养殖按只），
 *       互不挤占，返回本模块本轮处理的对象数；本模块的经营状态自己存在 {@link BlissfulLandData} 里对应的那一段。</li>
 * </ul>
 */

public interface LandModule<S extends PanelSection> {

    @NotNull Page page();

    @NotNull StreamCodec<RegistryFriendlyByteBuf, S> sectionCodec();

    @NotNull S section(@NotNull PanelContext context);

    @NotNull List<PanelAction.Type<?>> actions();

    default int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        return 0;
    }
}
