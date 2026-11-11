package net.alex.guzhenren.entity.beast;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * What the Beast Phantom renderer [兽力虚影] reads of anything drawn as a phantom beast: the family whose model it
 * borrows, how far it has faded in or out, how far its nose is pitched down (MC's sign: down is positive) and whether
 * it charges. The phantom beside its owner and the Beast Charge [兽力冲击] both are one; only the charge pitches and
 * charges. Only the phantom beside its owner comes and goes animated: {@link #getGlow} is the glow layer's strength
 * as a multiple of its resting one, {@link #getScale} the model's size and {@link #getDrift} how far the model is
 * drawn from the entity, in blocks to its right, up and ahead by its own yaw.
 *
 * <p>The animation files do not share a naming: the wild boar's are bare ({@code idle}, {@code appear}), the
 * bear's, the tiger's and the buffalo's carry an {@code animation.} prefix. {@link #getAppear} and {@link #getLoop}
 * hide that. The tortoise's carry the prefix but no appearance, so the turtle's phantom only idles. The horse has no
 * animation file: it is drawn on vanilla's horse model, which poses itself ({@code HorsePhantomModel}).
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

    default float getGlow(float partialTick) { return getFade(partialTick); }

    default float getScale(float partialTick) { return 1.0F; }

    default Vec3 getDrift(float partialTick) { return Vec3.ZERO; }

    default float getPitch(float partialTick) { return 0.0F; }

    default boolean isCharging() { return false; }

    static Map<BeastStrengthFamily, RawAnimation> byFamily(Function<BeastStrengthFamily, RawAnimation> animation) {
        Map<BeastStrengthFamily, RawAnimation> animations = new EnumMap<>(BeastStrengthFamily.class);
        for (BeastStrengthFamily family : BeastStrengthFamily.values()) animations.put(family, animation.apply(family));
        return animations;
    }

    static RawAnimation getAppear(BeastStrengthFamily family, String loop) {
        if (family == BeastStrengthFamily.TURTLE) return getLoop(family, loop);
        return RawAnimation.begin().thenPlay(getName(family, "appear")).thenLoop(getName(family, loop));
    }

    static RawAnimation getLoop(BeastStrengthFamily family, String loop) {
        return RawAnimation.begin().thenLoop(getName(family, loop));
    }

    private static String getName(BeastStrengthFamily family, String name) {
        return isBareNamed(family) ? name : "animation." + name;
    }

    private static boolean isBareNamed(BeastStrengthFamily family) {
        return family == BeastStrengthFamily.BOAR;
    }
}
