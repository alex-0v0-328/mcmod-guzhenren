package net.alex.guzhenren.item.material;

import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Primeval stone [元石]: a right click pours its essence [真元] into the holder's aperture [空窍].
 *
 * <p>Extends {@link net.alex.guzhenren.item.material.GuMaterialItem}. The essence value comes from registration.
 * The gate refuses an unawakened player (the service write is a silent no-op there) and a full pool.
 * Every automatic draw on carried stones -- the top-up line, paying a cost, the refinement supply slot --
 * lives in {@code PrimevalStoneSupply}; this class is the stone itself and what one is worth.
 *
 * <p>⚠ It refuses the unawakened instead of quietly doing nothing: the stone would otherwise be eaten for free.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.material.GuMaterialItem
 * @since 1.0.0
 */

public class PrimevalStoneItem extends GuMaterialItem {

    private static final String FAILED_UNAWAKENED = "guzhenren.item.failed.unawakened";
    private static final String FAILED_FULL = "guzhenren.item.failed.essence_full";
    private final long essence;

    public PrimevalStoneItem(Properties properties, long essence) {
        super(properties, Rank.ONE, GuPath.HEAVEN);
        this.essence = essence;
    }

    public long essence() { return essence; }

    @Override
    protected boolean hasUse() { return true; }

    @Override
    protected @Nullable Refusal gate(Player player, ItemStack stack) {
        if (!ApertureService.hasAperture(player)) return new Refusal(FAILED_UNAWAKENED);
        return ApertureEssenceService.getCurrentEssence(player) >= ApertureEssenceService.getMaxEssence(player)
                ? new Refusal(FAILED_FULL) : null;
    }

    @Override
    protected int apply(ServerPlayer player, ItemStack stack) {
        int used = used(player, stack);
        ApertureEssenceService.add(player, essence * used);
        return used;
    }

    public int used(Player player, ItemStack stack) {
        long deficit = ApertureEssenceService.getMaxEssence(player) - ApertureEssenceService.getCurrentEssence(player);
        return (int) Math.min(stack.getCount(), (deficit + essence - 1) / essence);
    }
}
