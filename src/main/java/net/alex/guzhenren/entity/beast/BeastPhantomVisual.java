package net.alex.guzhenren.entity.beast;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * What the Beast Phantom renderer [兽力虚影] reads of anything drawn as a phantom beast: the family whose model it
 * borrows and how far it has faded in or out. The phantom beside its owner and the Beast Charge [兽力冲击] both are one.
 *
 * <p>The animation files do not share a naming: the wild boar's are bare ({@code idle}, {@code attack_toss}), the
 * bear's and the tiger's carry an {@code animation.} prefix and attack with a swipe. {@link #getAttack} and
 * {@link #getLoop} hide that; the ox never shows and falls back to the boar.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomEntity
 * @see net.alex.guzhenren.entity.projectile.BeastChargeEntity
 * @since 1.0.0
 */

public interface BeastPhantomVisual {

    BeastStrengthFamily getFamily();

    float getFade(float partialTick);

    static Map<BeastStrengthFamily, RawAnimation> byFamily(Function<BeastStrengthFamily, RawAnimation> animation) {
        Map<BeastStrengthFamily, RawAnimation> animations = new EnumMap<>(BeastStrengthFamily.class);
        for (BeastStrengthFamily family : BeastStrengthFamily.values()) animations.put(family, animation.apply(family));
        return animations;
    }

    static RawAnimation getAttack(BeastStrengthFamily family, String loop) {
        return RawAnimation.begin().thenPlay(isBareNamed(family) ? "attack_toss" : "animation.attack_swipe")
                .thenLoop(getName(family, loop));
    }

    static RawAnimation getLoop(BeastStrengthFamily family, String loop) {
        return RawAnimation.begin().thenLoop(getName(family, loop));
    }

    private static String getName(BeastStrengthFamily family, String name) {
        return isBareNamed(family) ? name : "animation." + name;
    }

    private static boolean isBareNamed(BeastStrengthFamily family) {
        return family == BeastStrengthFamily.BOAR || family == BeastStrengthFamily.OX;
    }
}
