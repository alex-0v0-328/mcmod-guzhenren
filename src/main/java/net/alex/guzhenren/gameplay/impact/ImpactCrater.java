package net.alex.guzhenren.gameplay.impact;

import net.alex.guzhenren.core.ModEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The crater a body leaves when it hits the world hard: the plunge's [坠击] landing and the second-stage flight's
 * [二段飞行] crash both blast one through {@link #blast}.
 *
 * <p>A vanilla explosion at the impact that breaks blocks and hurts nothing: its {@link NoEntityDamage} calculator
 * refuses every entity's damage, so mobs, players and dropped items come through whole, and keeps vanilla's
 * knockback, which already leaves the explosion's source, the player, out. It asks {@code ModEvents#canGrief}
 * first: refused, the blast keeps every block and still knocks back and booms; allowed, blocks break by vanilla blast
 * resistance and drop like a creeper's ({@code mobExplosionDropDecay}), and TNT in the crater primes as vanilla's
 * {@code TntBlock#wasExploded} does, so chain reactions are a kept feature.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ImpactCrater {

    private ImpactCrater() {}

    public static void blast(@NotNull ServerPlayer player, @NotNull Vec3 at, float power) {
        Level.ExplosionInteraction interaction = ModEvents.canGrief(player.level(), player)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        player.level().explode(player, null, new NoEntityDamage(), at.x, at.y, at.z, power, false, interaction);
    }

    private static final class NoEntityDamage extends ExplosionDamageCalculator {

        @Override
        public boolean shouldDamageEntity(@NotNull Explosion explosion, @NotNull Entity entity) { return false; }
    }
}
