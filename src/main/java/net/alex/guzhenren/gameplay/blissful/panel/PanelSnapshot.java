package net.alex.guzhenren.gameplay.blissful.panel;

import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.blissful.LandModule;
import net.alex.guzhenren.gameplay.blissful.LandModules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板的一次下发：页眉（福地名、上一次操作的提示）、当前页，再加这一页各模块的 {@link PanelSection}（多段）。
 * <ul>
 *   <li>服务端只算主人正在看的那一页（{@link EarthSpiritMenu#page}）的各段；客户端按模块缓存各段最近一份，切页时先显示
 *       缓存。</li>
 *   <li>{@link #notice} 空组件 = 无提示。</li>
 *   <li>编码：页眉、页序号、段数，每段先写模块在 {@link LandModules#ALL} 的下标，再交给该模块的 {@code sectionCodec}；
 *       下标越界或段数超过 {@value #MAX_SECTIONS} 即解码失败。</li>
 * </ul>
 */

public record PanelSnapshot(@NotNull String landNameKey, @NotNull Component notice, @NotNull Page page,
                            @NotNull List<PanelSection> sections) {

    public static final int MAX_SECTIONS = 32;
    public static final StreamCodec<RegistryFriendlyByteBuf, PanelSnapshot> STREAM_CODEC =
            StreamCodec.of(PanelSnapshot::encode, PanelSnapshot::decode);

    public PanelSnapshot {
        sections = List.copyOf(sections);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PanelSnapshot snapshot) {
        buffer.writeUtf(snapshot.landNameKey);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, snapshot.notice);
        Page.STREAM_CODEC.encode(buffer, snapshot.page);
        writeSections(buffer, snapshot.sections, LandModules.ALL);
    }

    private static PanelSnapshot decode(RegistryFriendlyByteBuf buffer) {
        String landNameKey = buffer.readUtf();
        Component notice = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer);
        Page page = Page.STREAM_CODEC.decode(buffer);
        return new PanelSnapshot(landNameKey, notice, page, readSections(buffer, LandModules.ALL));
    }

    //region Sections [多段]
    public static void writeSections(@NotNull RegistryFriendlyByteBuf buffer, @NotNull List<PanelSection> sections,
                                     @NotNull List<? extends LandModule<?>> modules) {
        buffer.writeVarInt(sections.size());
        for (PanelSection section : sections) {
            int index = modules.indexOf(section.module());
            if (index < 0) throw new IllegalArgumentException("unregistered panel section " + section.getClass());
            buffer.writeVarInt(index);
            writeSection(buffer, section.module(), section);
        }
    }

    @SuppressWarnings("unchecked")
    private static <S extends PanelSection> void writeSection(RegistryFriendlyByteBuf buffer, LandModule<S> module,
                                                              PanelSection section) {
        module.sectionCodec().encode(buffer, (S) section);
    }

    public static @NotNull List<PanelSection> readSections(@NotNull RegistryFriendlyByteBuf buffer,
                                                          @NotNull List<? extends LandModule<?>> modules) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_SECTIONS) throw new DecoderException("bad panel section count " + count);
        List<PanelSection> sections = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int index = buffer.readVarInt();
            if (index < 0 || index >= modules.size()) throw new DecoderException("unknown panel section " + index);
            sections.add(modules.get(index).sectionCodec().decode(buffer));
        }
        return sections;
    }
    //endregion
}
