package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 地灵经营面板的分页：顺序即分页标签顺序与数字键 1–N；每页由若干
 * {@link net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSectionSource} 各出一段
 * （{@link net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSectionSources#on}）。
 * <ul>
 *   <li>{@link #MAP} 是主页（全屏地图），{@link #SECRET_REALM} 的标签写福地秘境的地名，{@link #INFO} 里按小节放总览与各经营面板段。
 *       </li>
 *   <li>新增一页 = 末尾追加一项、给面板段的 {@code page()} 指过来、写客户端页面，并递增 {@code ModPayloads.VERSION}（按序号
 *       编码）。</li>
 * </ul>
 */

public enum Page {
    MAP,
    STORAGE,
    SECRET_REALM,
    INFO;

    public static final StreamCodec<ByteBuf, Page> STREAM_CODEC = ByteBufCodecs.idMapper(index -> values()[index],
            Page::ordinal);
}
