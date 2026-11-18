package net.alex.guzhenren.network.payload;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 仅下发给施放者的杀招命中滑入提示通知载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册），携带杀招 ID 与本
 * 次消耗或参与的各蛊物品堆列表（主蛊居首，见 {@link KillingMoveService.Cast}），以补齐客户端无法直接读取服务端空窍存储
 * 中配合蛊的问题。
 */

public record KillingMoveTooltipPayload(String move, List<KillingMoveService.Used> used)
        implements CustomPacketPayload {

    public static final int MAX_USED = 8;
    public static final Type<KillingMoveTooltipPayload> TYPE = new Type<>(Guzhenren.id("killing_move_tooltip"));
    private static final StreamCodec<RegistryFriendlyByteBuf, KillingMoveService.Used> USED = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, KillingMoveService.Used::stack,
            ByteBufCodecs.BOOL, KillingMoveService.Used::spent,
            KillingMoveService.Used::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, KillingMoveTooltipPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(KillingMovePayload.MAX_ID_LENGTH), KillingMoveTooltipPayload::move,
                    USED.apply(ByteBufCodecs.list(MAX_USED)), KillingMoveTooltipPayload::used,
                    KillingMoveTooltipPayload::new);

    public static @NotNull KillingMoveTooltipPayload of(@NotNull KillingMoveService.Cast cast) {
        return new KillingMoveTooltipPayload(cast.move().getId(), cast.used());
    }

    @Override
    public @NotNull Type<KillingMoveTooltipPayload> type() { return TYPE; }
}
