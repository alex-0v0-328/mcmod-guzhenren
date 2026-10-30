package net.alex.guzhenren.item.gu.killermove;

import java.util.List;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The closed table of killer moves [杀招]: each row names the main Gu that may lead it from the main hand, the one
 * partner Gu that backs it from the hotbar, and the factor it puts on the main Gu's blade damage. A new killer move
 * is a new row; the rows carry no display name until Alex names them.
 *
 * <p>Both Gu are tended and count only once refined: {@link #asRefinedMain} and {@link #find} are the one
 * eligibility test, read by the client's radial menu and by {@link KillerMoveService} alike; a main and a partner
 * find the first row naming both, so two rows never share that pair.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillerMoveService
 * @since 1.0.0
 */

public enum KillerMove {

    /** A Moonlight Gu [月光蛊] or Moonglow Gu [月芒蛊] backed by a Little Light Gu [小光蛊]: the blade hits twice as hard. */
    LIGHT_BOOSTED_MOON_BLADE(List.of(ModItems.MOONLIGHT_GU, ModItems.MOONGLOW_GU), ModItems.LITTLE_LIGHT_GU, 2.0F);

    private final List<ItemLike> mains;
    private final ItemLike partner;
    private final float damageFactor;

    KillerMove(List<ItemLike> mains, ItemLike partner, float damageFactor) {
        this.mains = mains;
        this.partner = partner;
        this.damageFactor = damageFactor;
    }

    public float getDamageFactor() { return damageFactor; }

    public boolean isLedBy(Item item) { return mains.stream().anyMatch(main -> main.asItem() == item); }

    public static @Nullable TendedGuItem asRefinedMain(@NotNull ItemStack stack) {
        if (!(stack.getItem() instanceof TendedGuItem main) || !main.isRefined(stack)) return null;

        for (KillerMove move : values()) {
            if (move.isLedBy(main)) return main;
        }
        return null;
    }

    public static @Nullable KillerMove find(@NotNull Item main, @NotNull ItemStack partner) {
        if (!(partner.getItem() instanceof TendedGuItem backer) || !backer.isRefined(partner)) return null;

        for (KillerMove move : values()) {
            if (move.isLedBy(main) && move.partner.asItem() == backer) return move;
        }
        return null;
    }
}
