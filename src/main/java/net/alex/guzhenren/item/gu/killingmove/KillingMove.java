package net.alex.guzhenren.item.gu.killingmove;

import java.util.List;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The closed table of killing moves [杀招]: each row names its id, the main Gu that may lead it from the main hand,
 * the one partner Gu that backs it from the hotbar, and the factor it puts on the main Gu's blade damage. A new
 * killing move is a new row, with a book [杀招书] of the same id that teaches it ({@link KillingMoveBookItem}).
 *
 * <p>Both Gu are tended and count only once refined: {@link #asRefinedMain} and {@link #find} are the one
 * eligibility test, read by the client's radial menu and by {@link KillingMoveService} alike; a main and a partner
 * find the first row naming both, so two rows never share that pair. Whether the player has learned the row is
 * {@link KillingMoveService#knows}, asked on top.
 *
 * <p>⚠ The id is a save key: the learned list stores it ({@code KillingMoveData}), and it doubles as the book's
 * item id and the name's translation key, so renaming one forgets the move for everyone who learned it.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveService
 * @since 1.0.0
 */

public enum KillingMove {

    /** Little Light Amplification [小光蛊增幅]: a Moonlight [月光蛊] or Moonglow Gu [月芒蛊] blade, twice as hard. */
    LITTLE_LIGHT_AMPLIFY("km_mortal_little_light_amplify", List.of(ModItems.MOONLIGHT_GU, ModItems.MOONGLOW_GU),
            ModItems.LITTLE_LIGHT_GU, 2.0F);

    private final String id;
    private final List<ItemLike> mains;
    private final ItemLike partner;
    private final float damageFactor;

    KillingMove(String id, List<ItemLike> mains, ItemLike partner, float damageFactor) {
        this.id = id;
        this.mains = mains;
        this.partner = partner;
        this.damageFactor = damageFactor;
    }

    public String getId() { return id; }

    public String getTranslationKey() { return "guzhenren.killing_move." + id; }

    public List<ItemLike> getMains() { return mains; }

    public ItemLike getPartner() { return partner; }

    public float getDamageFactor() { return damageFactor; }

    public boolean isLedBy(Item item) { return mains.stream().anyMatch(main -> main.asItem() == item); }

    public static @Nullable TendedGuItem asRefinedMain(@NotNull ItemStack stack) {
        if (!(stack.getItem() instanceof TendedGuItem main) || !main.isRefined(stack)) return null;

        for (KillingMove move : values()) {
            if (move.isLedBy(main)) return main;
        }
        return null;
    }

    public static @Nullable KillingMove find(@NotNull Item main, @NotNull ItemStack partner) {
        if (!(partner.getItem() instanceof TendedGuItem backer) || !backer.isRefined(partner)) return null;

        for (KillingMove move : values()) {
            if (move.isLedBy(main) && move.partner.asItem() == backer) return move;
        }
        return null;
    }
}
