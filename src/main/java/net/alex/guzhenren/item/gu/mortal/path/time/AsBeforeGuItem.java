package net.alex.guzhenren.item.gu.mortal.path.time;

import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindService;
import net.alex.guzhenren.gameplay.rewind.RewindClock;
import net.alex.guzhenren.item.gu.tending.GuHunger;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * An As Before Gu [如故蛊]: a Time Path [宙道] Gu that puts something back the way it was. The middle class of the
 * three -- {@link ManAsBeforeGuItem} puts the carrier's body back a day, {@link TerrainAsBeforeGuItem} puts the land
 * or the waters around him back to the bare landform the world first generated -- owning what they share: the bar
 * that refills with the world clock, the body's memory that records only while a refined Man As Before Gu is carried,
 * and the refusals.
 *
 * <p>Extends {@link TendedGuItem} on a {@link GuHunger.RecoverBar}: never fed, never starves, and a use short of its
 * points is warned, then forced, like any use short of hunger ({@code TendedGuItem.drive}). {@link #remember} is the
 * heartbeat's step: once per {@code RewindClock.INTERVAL_TICKS} of game time, a carrier of a refined Man As Before Gu
 * has his body recorded. Carried means the bag, an aperture's store or a Vital [本命] slot. The land needs no memory:
 * the seed is it.
 *
 * <p>⚠ The client never holds the body's memory (it is not synced), so every gate on them runs for a {@code
 * ServerPlayer} only and the client-side gate passes. ⚠ The bar is settled on the server right before a use is
 * checked short and spends ({@link #performOwnUse}), never in a gate: a gate runs on the client too, and a client
 * write to the stack is a lie the server has to undo. A spend from a full bar restarts the stamp at the spend, since
 * a full bar's stamp is stale ({@code GuHunger.RecoverBar}).
 *
 * @author Alex
 * @version 1.0.0
 * @see RewindClock
 * @since 1.0.0
 */

public abstract class AsBeforeGuItem extends TendedGuItem {

    protected static final String FAILED_TOO_YOUNG = "guzhenren.item.failed.as_before_too_young";
    private static final String TOOLTIP_NEXT_POINT = "guzhenren.item.gu.next_point";
    protected final GuHunger.RecoverBar bar;

    protected AsBeforeGuItem(GuProperties guProperties) {
        super(guProperties);
        if (!(hunger instanceof GuHunger.RecoverBar recover)) {
            throw new IllegalArgumentException("as before gu needs a recover bar");
        }
        this.bar = recover;
    }

    //region the bar [恢复条]
    public GuHunger.RecoverBar getBar() { return bar; }

    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        bar.settle(stack, now);
        boolean wasFull = bar.getPoints(stack) >= bar.max();
        int result = super.performOwnUse(player, stack);
        if (wasFull) bar.restart(stack, now);
        return result;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Level level = context.level();
        if (level == null || !isRefined(stack)) return;

        int ticks = bar.getTicksToNextPoint(stack, level.getGameTime());
        if (ticks <= 0) return;

        tooltip.add(Component.translatable(TOOLTIP_NEXT_POINT, ModDisplayText.getCountdownSeconds(ticks))
                .withStyle(ChatFormatting.GRAY));
    }
    //endregion

    //region the memory [记忆]
    public static void remember(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now % RewindClock.INTERVAL_TICKS >= Ticks.SECOND) return;

        if (isCarriedRefined(player, ManAsBeforeGuItem.class)) PlayerRewindService.record(player, now);
    }

    public static boolean isCarriedRefined(ServerPlayer player, Class<? extends AsBeforeGuItem> kind) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isRefinedOf(inventory.getItem(slot), kind)) return true;
        }
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES; aperture++) {
            if (isRefinedOf(ApertureStorageService.getVital(player, aperture), kind)) return true;
            for (ItemStack stack : ApertureStorageService.getView(player, aperture)) {
                if (isRefinedOf(stack, kind)) return true;
            }
        }
        return false;
    }

    private static boolean isRefinedOf(ItemStack stack, Class<? extends AsBeforeGuItem> kind) {
        return kind.isInstance(stack.getItem()) && ((AsBeforeGuItem) stack.getItem()).isRefined(stack);
    }
    //endregion
}
