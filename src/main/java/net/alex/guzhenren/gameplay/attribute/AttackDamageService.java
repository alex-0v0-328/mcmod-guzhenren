package net.alex.guzhenren.gameplay.attribute;

import java.util.function.Predicate;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The only thing in the mod that touches the {@code ATTACK_DAMAGE} attribute -- {@code getBonus()} is
 * the whole sum: the usable strength points [力量点] ({@link StrengthService#getUsablePoints}, beast and human
 * strength through one capacity) at {@link PathStrengthData#ATTACK_PER_POINT} each, and the zombie [僵] tier
 * bonus. The strength counts only for a fist [拳]: the main hand empty or holding what {@link #setFistItem} names a
 * fist weapon (Epic Fight's FIST category, set by {@code compat.EpicFightIntegration}); a weapon in hand gets
 * none of it. {@link AttributeEvents} refreshes it when a physique, a strength, a skin, an aperture or an effect
 * changes; the player tick refreshes it every tick, which is how a hand change takes it on or off at once, and
 * {@code TransientModifiers.swap} makes an unchanged refresh a no-op.
 *
 * <p>⚠ The modifier MUST stay transient ({@link TransientModifiers}). ⚠ No effect may declare its own
 * {@code addAttributeModifier}: every timed attack buff is strength points through the capacity, never a flat
 * bonus, so there is no second seam. ⚠ The zombie bonus rides {@code BodyData.zombieTier}, NOT a
 * MobEffect (permanent 僵 has none; command tier -1 gets NO attack).
 *
 * @author Alex
 * @version 1.0.0
 * @see MaxHealthService
 * @see StrengthService
 * @since 1.0.0
 */

public final class AttackDamageService {

    private AttackDamageService() {}

    private static final ResourceLocation MODIFIER_ID =
            Guzhenren.id("strength_attack_damage");
    public static final double ZOMBIE_ATTACK_BASE = 5.0D;
    private static Predicate<ItemStack> fistItem = ItemStack::isEmpty;

    /** Names the held items that still count as a fist; an empty hand always does. */
    public static void setFistItem(@NotNull Predicate<ItemStack> predicate) { fistItem = predicate; }

    public static boolean isFistHand(@NotNull Player player) {
        ItemStack held = player.getMainHandItem();
        return held.isEmpty() || fistItem.test(held);
    }

    public static double getBonus(@NotNull Player player) {
        return getStrengthBonus(player) + getZombieBonus(player);
    }

    public static double getStrengthBonus(@NotNull Player player) {
        return isFistHand(player) ? getFistStrengthBonus(player) : 0.0D;
    }

    /** What a fist would add right now, whatever the hand holds: the B panel's attack row. */
    public static double getFistBonus(@NotNull Player player) {
        return getFistStrengthBonus(player) + getZombieBonus(player);
    }

    private static double getFistStrengthBonus(@NotNull Player player) {
        return StrengthService.getUsablePoints(player) * PathStrengthData.ATTACK_PER_POINT;
    }

    public static double getZombieBonus(@NotNull Player player) {
        BodyData body = BodyService.get(player);
        if (!body.isZombieOrTempZombie() || body.zombieTier() < 0) return 0.0D;

        return ZOMBIE_ATTACK_BASE * (1 << body.zombieTier());
    }

    public static void refresh(@NotNull ServerPlayer player) {
        TransientModifiers.swap(player, Attributes.ATTACK_DAMAGE, MODIFIER_ID, getBonus(player));
    }
}
