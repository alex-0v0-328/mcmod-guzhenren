package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板上行的一个操作意图：每个操作是一条带类型化参数的记录，由所属面板段定义并在 {@link #perform} 里执行。
 * <ul>
 *   <li>{@link EarthSpiritPanel#handle} 先复判主人与面板，再调 {@link #perform}；参数仍来自客户端，各操作自己校验范围。
 *       </li>
 *   <li>返回值是操作结果提示，空组件 = 不提示；提示随下一份快照下发。</li>
 *   <li>线上编码按 {@link PanelActions} 里的类型序号；新增操作只在面板段的 {@code actions()} 末尾追加，并递增
 *       {@code ModPayloads.VERSION}。</li>
 * </ul>
 */

public interface PanelAction {

    @NotNull Component perform(@NotNull ActionContext context);

    record Type<A extends PanelAction>(@NotNull Class<A> type,
                                       @NotNull StreamCodec<? super RegistryFriendlyByteBuf, A> codec) {}
}
