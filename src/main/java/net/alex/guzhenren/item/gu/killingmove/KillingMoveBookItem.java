package net.alex.guzhenren.item.gu.killingmove;

import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * A killing move book [杀招书]: a right click learns [学会] its {@link KillingMove} and uses up the book, a creative
 * player's excepted ({@link ItemStack#consume}); a move already known refuses and keeps the book. Learning asks for
 * nothing else, not even an aperture.
 *
 * <p>Both sides decide from the same synced {@code KillingMoveData}, so the client's swing matches the server's
 * answer. The tooltip lists the row: its main Gu, its partner and the blade factor; whether this player knows it is
 * a client-side line ({@code KillingMoveInput}), since an item's own tooltip has no player.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveService#learn
 * @since 1.0.0
 */

public final class KillingMoveBookItem extends Item {

    private static final String LEARNED = "guzhenren.item.killing_move.learned";
    private static final String FAILED_KNOWN = "guzhenren.item.failed.killing_move_known";
    private static final String TOOLTIP_MAINS = "guzhenren.item.killing_move.mains";
    private static final String TOOLTIP_PARTNER = "guzhenren.item.killing_move.partner";
    private static final String TOOLTIP_FACTOR = "guzhenren.item.killing_move.blade_factor";
    private final KillingMove move;

    public KillingMoveBookItem(Properties properties, KillingMove move) {
        super(properties.stacksTo(1));
        this.move = move;
    }

    public KillingMove getMove() { return move; }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Component name = Component.translatable(move.getTranslationKey());
        if (KillingMoveService.knows(player, move)) {
            if (player instanceof ServerPlayer server) ActionBar.refuse(server, FAILED_KNOWN, name);
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer server) {
            KillingMoveService.learn(server, move);
            ActionBar.inform(server, LEARNED, name);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        List<Component> mains = move.getMains().stream().map(main -> main.asItem().getDescription()).toList();
        tooltip.add(Component.translatable(TOOLTIP_MAINS, ComponentUtils.formatList(mains, Component.literal(" / ")))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(TOOLTIP_PARTNER, move.getPartner().asItem().getDescription())
                .withStyle(ChatFormatting.GRAY));
        float factor = move.getDamageFactor();
        String shown = factor == (int) factor ? String.valueOf((int) factor) : String.valueOf(factor);
        tooltip.add(Component.translatable(TOOLTIP_FACTOR, shown).withStyle(ChatFormatting.GRAY));
    }
}
