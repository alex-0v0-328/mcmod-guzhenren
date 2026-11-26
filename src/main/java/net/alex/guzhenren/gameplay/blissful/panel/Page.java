package net.alex.guzhenren.gameplay.blissful.panel;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 地灵经营面板的分页：顺序即左侧标签顺序与数字键 1–N，每页由一个
 * {@link net.alex.guzhenren.gameplay.blissful.LandModule} 负责
 * （{@link net.alex.guzhenren.gameplay.blissful.LandModules#of}）。
 * <ul>
 *   <li>新增一页 = 末尾追加一项、写它的模块与客户端页面，并递增 {@code ModPayloads.VERSION}（按序号编码）。</li>
 * </ul>
 */

public enum Page {
    OVERVIEW,
    MOUNTAIN,
    HUSBANDRY,
    FARMING,
    TERRITORY,
    STORAGE,
    GU;

    public static final StreamCodec<ByteBuf, Page> STREAM_CODEC = ByteBufCodecs.idMapper(index -> values()[index],
            Page::ordinal);
}
