package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.immortalaperture.LandModule;
import net.alex.guzhenren.gameplay.immortalaperture.LandModules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 全部面板操作的类型表与线上编解码：先是面板自己的 {@link View}，再按 {@code LandModules.ALL} 的顺序接各模块的
 * {@code actions()}。
 * <ul>
 *   <li>编码写类型序号再写该类型的参数；序号越界即解码失败（客户端伪造的包被断开）。</li>
 *   <li>{@link View}：客户端切页时发（信息页内切小节不发），服务端记下这位主人正在看的页，之后的快照只算这一页的各段。
 *       </li>
 * </ul>
 */

public final class PanelActions {

    private PanelActions() {}

    public record View(@NotNull Page page) implements PanelAction {

        public static final PanelAction.Type<View> TYPE = new PanelAction.Type<>(View.class,
                Page.STREAM_CODEC.map(View::new, View::page));

        @Override
        public @NotNull Component perform(@NotNull ActionContext context) {
            context.menu().setPage(page);
            EarthSpiritPanel.remember(context.player(), page);
            return Component.empty();
        }
    }

    private static final List<PanelAction.Type<?>> TYPES = collect();
    public static final StreamCodec<RegistryFriendlyByteBuf, PanelAction> STREAM_CODEC =
            StreamCodec.of(PanelActions::encode, PanelActions::decode);

    private static List<PanelAction.Type<?>> collect() {
        List<PanelAction.Type<?>> types = new ArrayList<>();
        types.add(View.TYPE);
        for (LandModule<?> module : LandModules.ALL) types.addAll(module.actions());
        return List.copyOf(types);
    }

    public static @NotNull List<PanelAction.Type<?>> types() { return TYPES; }

    private static void encode(RegistryFriendlyByteBuf buffer, PanelAction action) {
        for (int i = 0; i < TYPES.size(); i++) {
            if (TYPES.get(i).type() == action.getClass()) {
                buffer.writeVarInt(i);
                write(buffer, TYPES.get(i), action);
                return;
            }
        }
        throw new IllegalArgumentException("unregistered panel action " + action.getClass().getName());
    }

    private static <A extends PanelAction> void write(RegistryFriendlyByteBuf buffer, PanelAction.Type<A> type,
                                                      PanelAction action) {
        type.codec().encode(buffer, type.type().cast(action));
    }

    private static PanelAction decode(RegistryFriendlyByteBuf buffer) {
        int index = buffer.readVarInt();
        if (index < 0 || index >= TYPES.size()) throw new DecoderException("unknown panel action " + index);
        return TYPES.get(index).codec().decode(buffer);
    }
}
