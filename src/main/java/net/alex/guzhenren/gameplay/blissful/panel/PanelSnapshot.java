package net.alex.guzhenren.gameplay.blissful.panel;

import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.LandModules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板的一次下发：页眉（福地名、上一次操作的提示）加当前那一页的 {@link PanelSection}。
 * <ul>
 *   <li>服务端只算主人正在看的那一页（{@link EarthSpiritMenu#page}）；客户端按页缓存各页最近一份，切页时先显示缓存。</li>
 *   <li>{@link #notice} 空组件 = 无提示。</li>
 *   <li>编码：页眉、页序号、再交给该页模块的 {@code sectionCodec}。</li>
 * </ul>
 */

public record PanelSnapshot(@NotNull String landNameKey, @NotNull Component notice, @NotNull PanelSection section) {

    public static final StreamCodec<RegistryFriendlyByteBuf, PanelSnapshot> STREAM_CODEC =
            StreamCodec.of(PanelSnapshot::encode, PanelSnapshot::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, PanelSnapshot snapshot) {
        buffer.writeUtf(snapshot.landNameKey);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, snapshot.notice);
        Page page = snapshot.section.page();
        Page.STREAM_CODEC.encode(buffer, page);
        writeSection(buffer, LandModules.of(page), snapshot.section);
    }

    @SuppressWarnings("unchecked")
    private static <S extends PanelSection> void writeSection(RegistryFriendlyByteBuf buffer, LandModule<S> module,
                                                              PanelSection section) {
        module.sectionCodec().encode(buffer, (S) section);
    }

    private static PanelSnapshot decode(RegistryFriendlyByteBuf buffer) {
        String landNameKey = buffer.readUtf();
        Component notice = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer);
        Page page = Page.STREAM_CODEC.decode(buffer);
        return new PanelSnapshot(landNameKey, notice, LandModules.of(page).sectionCodec().decode(buffer));
    }
}
