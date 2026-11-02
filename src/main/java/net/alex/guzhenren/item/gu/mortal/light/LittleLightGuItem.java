package net.alex.guzhenren.item.gu.mortal.light;

import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.RefinedGuState;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Little Light Gu [小光蛊]: Light Path [光道], rank I. A tended Gu whose own use is a switch, not a payout: it glows
 * in the hand, and a refined one can be turned bright for a paid round of two minutes.
 *
 * <p>The glow is not code: {@code assets/guzhenren/dynamiclights/item/} holds two LambDynamicLights definitions (light
 * level 10 for any stack, 15 for one carrying {@code LITTLE_LIGHT_BRIGHT}; the mod takes the larger of the matching
 * ones). Without that mod nothing glows. The bright component is synced to the client and present only while on,
 * never {@code false}, so the definition can match it and a switched-off stack equals a fresh one.
 *
 * <p>⚠ The switch bills no essence: {@code performOwnUse} replaces {@code drive}, and {@code useThreshold} is 0.
 * Turning bright on is one round: it pays the spec's use hunger once, eating from the bag before and after the drop
 * like any use, and stays bright for {@link #BRIGHT_TICKS} of server game time (the day clock's own ticks, not
 * scaled by self-time), held or not; a second press turns it off early and refunds nothing. The same use hunger is
 * what a killing move takes from this Gu as a partner when it starts. A wild one has no switch, so it can only glow
 * at 10.
 *
 * <p>⚠ {@code LITTLE_LIGHT_BRIGHT_UNTIL} is written once at the switch, never per tick, since every write resyncs
 * the slot. Only a carried stack ticks, so one left bright in a chest or on the ground goes out the moment a player
 * carries it again. Creative meals never use the food up ({@code GuHunger}).
 *
 * <p>⚠ Turning on short of the use hunger is a forced use ({@code GuForcedUse}): the first press warns, the same Gu
 * pressed again within five seconds turns bright for the full round, and the Gu dies of exhaustion when it goes out,
 * by the deadline or by a press. {@code LITTLE_LIGHT_FORCED} marks that round; it is written and removed with
 * {@code LITTLE_LIGHT_BRIGHT}, never per tick. Turning off is never gated; a Vital one short is only refused, and
 * creative is never warned and never dies.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
 */

public final class LittleLightGuItem extends TendedGuItem {

    public static final int BRIGHT_TICKS = 2_400;
    private static final String MSG_ON = "guzhenren.item.gu.little_light_bright_on";
    private static final String MSG_OFF = "guzhenren.item.gu.little_light_bright_off";
    private static final String MSG_DIMMED = "guzhenren.item.gu.little_light_dimmed";
    private static final String TOOLTIP_BRIGHT = "guzhenren.item.gu.little_light_bright";

    public LittleLightGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    public static boolean isBright(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LITTLE_LIGHT_BRIGHT, false);
    }

    //region the switch
    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        feed(player, stack);
        if (isBright(stack)) {
            ActionBar.inform(player, MSG_OFF);
            turnOff(player, stack);
            return 0;
        }
        if (stopsShortUse(player, stack)) return WARNED;

        boolean forced = isShortOfUse(player, stack);
        payRound(player, stack);
        stack.set(ModDataComponents.LITTLE_LIGHT_BRIGHT, true);
        stack.set(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL, player.level().getGameTime() + BRIGHT_TICKS);
        if (forced) stack.set(ModDataComponents.LITTLE_LIGHT_FORCED, true);
        ActionBar.inform(player, MSG_ON);
        return 0;
    }

    private void payRound(ServerPlayer player, ItemStack stack) {
        RefinedGuState state = getState(stack);
        stack.set(ModDataComponents.REFINED_GU_STATE,
                state.withHunger(Math.max(0, state.hunger() - spec.getHungerPerUse())));
        feedAndWarn(player, stack);
    }

    @Override
    protected long useThreshold(ItemStack stack) { return 0L; }

    private static void turnOff(ServerPlayer player, ItemStack stack) {
        boolean forced = stack.has(ModDataComponents.LITTLE_LIGHT_FORCED);
        stack.remove(ModDataComponents.LITTLE_LIGHT_BRIGHT);
        stack.remove(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL);
        stack.remove(ModDataComponents.LITTLE_LIGHT_FORCED);
        if (!forced || player.getAbilities().instabuild) return;

        exhaust(player, stack);
        stack.shrink(1);
    }
    //endregion

    //region the round
    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot,
                              boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !isBright(stack)) return;

        Long until = stack.get(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL);
        if (until != null && level.getGameTime() < until) return;

        ActionBar.inform(player, MSG_DIMMED);
        turnOff(player, stack);
    }
    //endregion

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isBright(stack)) tooltip.add(Component.translatable(TOOLTIP_BRIGHT).withStyle(ChatFormatting.YELLOW));
    }
}
