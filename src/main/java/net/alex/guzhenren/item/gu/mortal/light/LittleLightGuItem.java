package net.alex.guzhenren.item.gu.mortal.light;

import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.item.gu.GuHunger;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.RefinedGuState;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Little Light Gu [小光蛊]: Light Path [光道], rank I. A tended Gu whose own use is a switch, not a payout: it glows
 * in the hand, and a refined one can be turned bright, which costs hunger while it is held.
 *
 * <p>The glow is not code: {@code assets/guzhenren/dynamiclights/item/} holds two LambDynamicLights definitions (light
 * level 10 for any stack, 15 for one carrying {@code LITTLE_LIGHT_BRIGHT}; the mod takes the larger of the matching
 * ones). Without that mod nothing glows. The bright component is synced to the client and present only while on,
 * never {@code false}, so the definition can match it and a switched-off stack equals a fresh one.
 *
 * <p>⚠ The switch bills no essence and no hunger: {@code performOwnUse} replaces {@code drive}, and {@code
 * useThreshold} is 0. The spec's use cost is what the killing move bills this Gu as a partner. A wild one has no
 * switch, so it can only glow at 10.
 *
 * <p>⚠ The drain is one hunger point per {@link #DRAIN_TICKS} held ticks while bright and in a player's main hand
 * or offhand, on server game time (the day clock's own ticks, not scaled by self-time). It never writes a component
 * per tick, since every write resyncs the slot: {@code LITTLE_LIGHT_HELD_AT} is stamped once when holding starts,
 * advanced by the period at each point, and removed when the stack is no longer held or bright. A stamp older
 * than a period plus {@link #STALE_TICKS} (logged out, chunk unloaded, stack in a chest) restarts instead of
 * charging the gap.
 *
 * <p>⚠ The drain is a drop like any other: after each point it eats from the bag back to full ({@code feed}), and the
 * switch eats first like any use. It never starves it: when a point would leave the bar hungry ({@code isHungry},
 * hunger at or below the daily drain) and the bag holds none of its food, it turns bright off instead, shows {@code
 * little_light_dimmed} and keeps the point. Turning on while hungry with no food in the bag is refused. Creative
 * follows {@code TendedGuItem}: the bar still drains for show down to 0, never eats and never dims.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
 */

public final class LittleLightGuItem extends TendedGuItem {

    public static final int DRAIN_TICKS = 2_400;
    public static final int STALE_TICKS = 20;
    private static final String FAILED_HUNGRY = "guzhenren.item.failed.little_light_hungry";
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
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        if (isBright(stack) || player.getAbilities().instabuild) return null;
        if (!(player instanceof ServerPlayer server) || !isHungry(server, stack)) return null;
        return hunger.carriesFood(this, player) ? null : new Refusal(FAILED_HUNGRY);
    }

    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        feed(player, stack);
        if (isBright(stack)) {
            turnOff(stack);
            ActionBar.inform(player, MSG_OFF);
            return 0;
        }
        stack.set(ModDataComponents.LITTLE_LIGHT_BRIGHT, true);
        ActionBar.inform(player, MSG_ON);
        return 0;
    }

    @Override
    protected long useThreshold(ItemStack stack) { return 0L; }

    private static void turnOff(ItemStack stack) {
        stack.remove(ModDataComponents.LITTLE_LIGHT_BRIGHT);
        stack.remove(ModDataComponents.LITTLE_LIGHT_HELD_AT);
    }
    //endregion

    //region the drain
    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot,
                              boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;

        boolean held = stack == player.getMainHandItem() || stack == player.getOffhandItem();
        if (!held || !isBright(stack) || !isRefined(stack)) {
            if (stack.has(ModDataComponents.LITTLE_LIGHT_HELD_AT)) {
                stack.remove(ModDataComponents.LITTLE_LIGHT_HELD_AT);
            }
            return;
        }
        long now = level.getGameTime();
        Long since = stack.get(ModDataComponents.LITTLE_LIGHT_HELD_AT);
        if (since == null || now < since || now - since > DRAIN_TICKS + STALE_TICKS) {
            stack.set(ModDataComponents.LITTLE_LIGHT_HELD_AT, now);
            return;
        }
        if (now - since < DRAIN_TICKS) return;

        stack.set(ModDataComponents.LITTLE_LIGHT_HELD_AT, since + DRAIN_TICKS);
        drain(player, stack);
    }

    private void drain(ServerPlayer player, ItemStack stack) {
        RefinedGuState state = getState(stack);
        if (player.getAbilities().instabuild) {
            stack.set(ModDataComponents.REFINED_GU_STATE, state.withHunger(Math.max(0, state.hunger() - 1)));
            return;
        }
        if (leavesHungry(stack) && !hunger.carriesFood(this, player)) {
            turnOff(stack);
            ActionBar.inform(player, MSG_DIMMED);
            return;
        }
        stack.set(ModDataComponents.REFINED_GU_STATE, state.withHunger(Math.max(0, state.hunger() - 1)));
        feed(player, stack);
    }

    private boolean leavesHungry(ItemStack stack) {
        return getState(stack).hunger() - 1 <= ((GuHunger.HungerBar) hunger).perDay();
    }
    //endregion

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isBright(stack)) tooltip.add(Component.translatable(TOOLTIP_BRIGHT).withStyle(ChatFormatting.YELLOW));
    }
}
