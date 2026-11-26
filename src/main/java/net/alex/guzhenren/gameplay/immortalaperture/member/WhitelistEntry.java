package net.alex.guzhenren.gameplay.immortalaperture.member;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 面板上一行玩家：在场玩家与白名单共用；名字只供显示（离线的白名单成员取存档里记的名字）。
 */

public record WhitelistEntry(@NotNull UUID id, @NotNull String name, boolean whitelisted, boolean owner) {

    public static final StreamCodec<ByteBuf, WhitelistEntry> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, WhitelistEntry::id,
            ByteBufCodecs.STRING_UTF8, WhitelistEntry::name,
            ByteBufCodecs.BOOL, WhitelistEntry::whitelisted,
            ByteBufCodecs.BOOL, WhitelistEntry::owner,
            WhitelistEntry::new);
    public static final StreamCodec<ByteBuf, List<WhitelistEntry>> LIST_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());
}
