package net.alex.guzhenren.gameplay.plunge;

import net.alex.guzhenren.core.ModEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * The game-version half of a plunge [坠击] landing: the fall damage vanilla works out for a height, and the crater.
 *
 * <p>{@link #getFallDamage} feeds the player's safe fall distance and fall damage multiplier attributes to
 * {@link PlungeImpact#getDamage}, the same numbers 1.21's {@code LivingEntity#calculateFallDamage} reads.
 * {@link #blast} is a vanilla explosion at the landing that breaks the ground and hurts nothing: its {@link Crater}
 * calculator refuses every entity's damage, so mobs, players and dropped items come through whole, and keeps
 * vanilla's knockback (vanilla already leaves the plunger, the explosion's source, out). It asks
 * {@code ModEvents#canGrief} first: refused, the blast keeps every block and still knocks back and booms; allowed,
 * blocks break by vanilla blast resistance and drop like a creeper's ({@code mobExplosionDropDecay}).
 *
 * <p>⚠ A version seam: the two fall attributes are 1.20.5's, where 1.20.1 hard-codes a safe distance of 3 and folds
 * in jump boost, and the calculator's entity hooks ({@code shouldDamageEntity}, {@code getKnockbackMultiplier})
 * only exist from 1.20.5, so the Forge 1.20.1 port works both out its own way.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlungeService
 * @since 1.0.0
 */

final class PlungeLanding {

    private PlungeLanding() {}

    static int getFallDamage(@NotNull ServerPlayer player, double height) {
        return PlungeImpact.getDamage(height, player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),
                player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
    }

    static void blast(@NotNull ServerPlayer player, float power) {
        Level.ExplosionInteraction interaction = ModEvents.canGrief(player.level(), player)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        player.level().explode(player, null, new Crater(),
                player.getX(), player.getY(), player.getZ(), power, false, interaction);
    }

    private static final class Crater extends ExplosionDamageCalculator {

        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) { return false; }
    }
}
