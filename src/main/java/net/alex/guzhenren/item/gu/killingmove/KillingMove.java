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
 * the one partner Gu that backs it, the factor it puts on the main Gu's blade damage, the hunger the partner pays once
 * to start the move [启动] and the essence it pays on every blade fired while the move is on. A new killing move is a
 * new row, with a book [杀招书] of the same id that teaches it ({@link KillingMoveBookItem}).
 *
 * <p>Both Gu are tended and count only once refined: {@link #isLedBy} and {@link #isBackedBy} are the one
 * eligibility test, read by the client's radial menu and by {@link KillingMoveService} alike. Whether the player has
 * learned the row is {@link KillingMoveService#knows}, and where its partner may sit is the service's.
 *
 * <p>⚠ The id is a save key and the payload's name for the move: the learned list stores it
 * ({@code KillingMoveData}), and it doubles as the book's item id and the name's translation key, so renaming one
 * forgets the move for everyone who learned it.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveService
 * @since 1.0.0
 */

public enum KillingMove {

    /**
     * Little Light Amplification [小光蛊增幅]: a Moonlight [月光蛊] or Moonglow Gu [月芒蛊] blade, twice as hard; the
     * Little Light Gu pays 3 hunger to start and 8 essence a blade, half its own use.
     */
    LITTLE_LIGHT_AMPLIFY("km_mortal_little_light_amplify", List.of(ModItems.MOONLIGHT_GU, ModItems.MOONGLOW_GU),
            ModItems.LITTLE_LIGHT_GU, 2.0F, 3, 8);

    private final String id;
    private final List<ItemLike> mains;
    private final ItemLike partner;
    private final float damageFactor;
    private final int startHunger;
    private final int partnerEssence;

    KillingMove(String id, List<ItemLike> mains, ItemLike partner, float damageFactor, int startHunger,
                int partnerEssence) {
        this.id = id;
        this.mains = mains;
        this.partner = partner;
        this.damageFactor = damageFactor;
        this.startHunger = startHunger;
        this.partnerEssence = partnerEssence;
    }

    public String getId() { return id; }

    public String getTranslationKey() { return "guzhenren.killing_move." + id; }

    public List<ItemLike> getMains() { return mains; }

    public ItemLike getPartner() { return partner; }

    public float getDamageFactor() { return damageFactor; }

    /** The hunger the partner pays once when the move starts. */
    public int getStartHunger() { return startHunger; }

    /** The essence the partner pays on every blade fired while the move is on. */
    public int getPartnerEssence() { return partnerEssence; }

    public boolean isLedBy(Item item) { return mains.stream().anyMatch(main -> main.asItem() == item); }

    public boolean isBackedBy(@NotNull ItemStack stack) {
        return stack.getItem() == partner.asItem() && stack.getItem() instanceof TendedGuItem backer
                && backer.isRefined(stack);
    }

    public static @Nullable KillingMove byId(@NotNull String id) {
        for (KillingMove move : values()) {
            if (move.id.equals(id)) return move;
        }
        return null;
    }
}
