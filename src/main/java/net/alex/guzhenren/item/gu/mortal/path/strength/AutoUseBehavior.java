package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The auto behavior [自动生效]: its Gu works on its own while the holder is hurt, never by hand. The Self Reliance
 * Gu [自力更生蛊] and the Hardship Strength Gu [苦力蛊] carry one each; {@link AutoUse} starts and stops them.
 *
 * <p>Each names the path effect it runs ({@link #getStateType}), its own start condition ({@link #canStart}) and
 * the length of the window one start opens ({@link #getDurationSeconds}); its {@code applyEffect} puts that effect
 * on until {@link AutoUse#getEndTick}, the window plus the held beasts' duration bonus. A hand use is refused here with
 * {@link #FAILED_AUTO_ONLY} and pays nothing; only {@link AutoUse}'s start gets past the gate, and no Amplify Effect
 * Gu [功倍蛊] multiplies it.
 *
 * @author Alex
 * @version 1.0.0
 * @see AutoUse
 * @since 1.0.0
 */

public interface AutoUseBehavior extends GuBehavior {

    String FAILED_AUTO_ONLY = "guzhenren.item.failed.auto_only";

    @NotNull PathEffectType<?> getStateType();

    boolean canStart(@NotNull ServerPlayer player);

    int getDurationSeconds();

    @Override
    default @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return AutoUse.isStarting(player) ? null : new Refusal(FAILED_AUTO_ONLY);
    }
}
