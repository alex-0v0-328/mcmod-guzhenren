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
 * 只发给施放者的服务端通知：杀招[Killing Move]命中，提示滑入。
 * 带杀招 id 与本次各蛊物品堆，主蛊在前（{@link KillingMoveService.Cast}）；配合蛊或在客户端不可见的空窍[Aperture]存储。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
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
