package net.alex.guzhenren.item.gu.mortal.human;

import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Hope Gu [希望蛊]: the one Gu that awakens [开窍] an aperture, through the awakening ritual [开窍仪式] a single
 * click starts.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.MortalGuItem} directly, not {@code OneShotGuItem}: it pays no refining
 * and runs no modules. The click rolls the aptitude [资质], takes the Gu and hands the rest to
 * {@link AwakeningRitualService}, which holds the player still and awakens them when the white peaks. A stack that
 * rolled under the old held ritual still carries that roll in {@link ModDataComponents#AWAKEN_BASE}, and the ritual
 * takes it; creative, which keeps the stack, clears it so a reset can re-roll.
 *
 * <p>⚠ {@code stacksTo(1)}: a data component is shared by the whole stack, so stacking would give 64 copies of one
 * rolled aptitude.
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningRitualService
 * @see net.alex.guzhenren.item.gu.MortalGuItem
 * @since 1.0.0
 */

public class HopeGuItem extends MortalGuItem {

    private static final String FAILED_AWAKENED = "guzhenren.item.failed.awakened";
    private static final String FAILED_AWAKENING = "guzhenren.item.failed.awakening";
    private static final String FAILED_NOURISHING = "guzhenren.item.failed.nourishing";

    public HopeGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    @Override
    protected @Nullable Refusal checkOwnUse(Player player, ItemStack stack) {
        if (ApertureService.isAwakened(player)) return new Refusal(FAILED_AWAKENED);
        if (AwakeningRitualService.isInRitual(player)) return new Refusal(FAILED_AWAKENING);
        return ApertureNourishService.isCultivating(player) ? new Refusal(FAILED_NOURISHING) : null;
    }

    @Override
    protected final @Nullable Refusal checkUse(Player player, ItemStack stack) { return checkOwnUse(player, stack); }

    @Override
    protected final int useDurationTicks(Player player, ItemStack stack) { return 0; }

    @Override
    protected int getCooldownTicks(ItemStack stack) { return REFINE_DONE_COOLDOWN_TICKS; }

    @Override
    protected final int performUse(ServerPlayer player, ItemStack stack) {
        int rolled = stack.getOrDefault(ModDataComponents.AWAKEN_BASE, 0);
        stack.remove(ModDataComponents.AWAKEN_BASE);
        AwakeningRitualService.start(player, rolled > 0 ? rolled : Talent.randomPercent(Talent.randomTalent()));
        return 1;
    }
}
