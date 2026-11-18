package net.alex.guzhenren.item.gu.killingmove;

import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.item.GuItem;
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
 * 杀招[Killing Move]秘籍物品：右键阅读以习得对应的 {@link KillingMove}。
 * <ul>
 *   <li>右键调用 {@link KillingMoveService#learn} 学会杀招并消耗书本（创造模式由 {@link ItemStack#consume} 保留）；若已
 *       习得则拒绝并保留书本；学习本身不要求已开窍[Awakening]。</li>
 *   <li>直接继承 {@link Item} 而非 {@link GuItem}：书本无转数、无流派、也不用蛊虫蓄力/轻点模板；双端均读取已同步的
 *       {@code KillingMoveData} 判断是否已学会，保证客户端挥手表现与服务端一致；物品提示框列出主蛊、辅蛊与伤害倍率，而
 *       「已学会」提示行因原版 {@link #appendHoverText} 拿不到玩家实例，改由客户端 {@code KillingMoveInput} 追加。</li>
 * </ul>
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
