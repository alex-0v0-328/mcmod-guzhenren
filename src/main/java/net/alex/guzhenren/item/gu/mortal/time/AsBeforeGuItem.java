package net.alex.guzhenren.item.gu.mortal.time;

import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.rewind.ChunkRewindService;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindService;
import net.alex.guzhenren.gameplay.rewind.RewindClock;
import net.alex.guzhenren.item.gu.GuClock;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An As Before Gu [如故蛊]: a Time Path [宙道] Gu that puts something back the way it was a day ago. The middle
 * class of the three -- {@link ManAsBeforeGuItem} for the carrier's body, {@link TerrainAsBeforeGuItem} for the land
 * or the waters around him -- owning what they share: the bar that refills with the world clock, the memory that
 * records only while a refined one is carried, and the refusals.
 *
 * <p>Extends {@link TendedGuItem} on a {@link GuClock.RecoverBar}: never fed, never starves, and refuses a use
 * ({@link #checkRecoverGate}) until a point has come back. {@link #remember} is the heartbeat's step: once per
 * {@code RewindClock.INTERVAL_TICKS} of game time, a carrier of a refined Man As Before Gu has his body recorded and a
 * carrier of a refined Mountain or River As Before Gu has the chunks around him recorded. Carried means the bag, an
 * aperture's store or a Vital [本命] slot.
 *
 * <p>⚠ The client never holds the memories (neither is synced), so every gate on them runs for a {@code ServerPlayer}
 * only and the client-side gate passes; the bar's gate runs on both, since the bar rides the stack. ⚠ The bar is
 * settled on the server right before a use spends ({@link #useApply}), never in a gate: a gate runs on the client too,
 * and a client write to the stack is a lie the server has to undo. A spend from a full bar restarts the stamp at the
 * spend, since a full bar's stamp is stale ({@code GuClock.RecoverBar}).
 *
 * @author Alex
 * @version 1.0.0
 * @see RewindClock
 * @since 1.0.0
 */

public abstract class AsBeforeGuItem extends TendedGuItem {

    protected static final String FAILED_RECOVERING = "guzhenren.item.failed.as_before_recovering";
    protected static final String FAILED_TOO_YOUNG = "guzhenren.item.failed.as_before_too_young";
    private static final String TOOLTIP_NEXT_POINT = "guzhenren.item.gu.next_point";
    protected final GuClock.RecoverBar bar;

    protected AsBeforeGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
        if (!(clock instanceof GuClock.RecoverBar recover)) {
            throw new IllegalArgumentException("as before gu needs a recover bar");
        }
        this.bar = recover;
    }

    //region the bar
    public GuClock.RecoverBar getBar() { return bar; }

    protected final @Nullable Refusal checkRecoverGate(Player player, ItemStack stack) {
        long now = player.level().getGameTime();
        if (bar.getPoints(stack, now) >= bar.perUse()) return null;

        long seconds = (bar.getTicksToNextPoint(stack, now) + Ticks.SECOND - 1) / Ticks.SECOND;
        return new Refusal(FAILED_RECOVERING, Component.literal(String.valueOf(seconds)));
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        bar.settle(stack, now);
        boolean wasFull = bar.getPoints(stack) >= bar.max();
        int result = super.useApply(player, stack);
        if (wasFull) bar.restart(stack, now);
        return result;
    }

    @Override
    protected void appendTooltip(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                 @NotNull TooltipFlag flag) {
        super.appendTooltip(stack, level, tooltip, flag);
        if (level == null || !isRefined(stack)) return;

        int ticks = bar.getTicksToNextPoint(stack, level.getGameTime());
        if (ticks <= 0) return;

        tooltip.add(Component.translatable(TOOLTIP_NEXT_POINT, (ticks + Ticks.SECOND - 1) / Ticks.SECOND)
                .withStyle(ChatFormatting.GRAY));
    }
    //endregion

    //region the memory -- the heartbeat's step, once per interval of game time
    public static void remember(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now % RewindClock.INTERVAL_TICKS >= Ticks.SECOND) return;

        if (isCarriedRefined(player, ManAsBeforeGuItem.class)) PlayerRewindService.record(player, now);
        if (isCarriedRefined(player, TerrainAsBeforeGuItem.class)) {
            ChunkRewindService.recordAround(player.serverLevel(), player.chunkPosition(), now);
        }
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
