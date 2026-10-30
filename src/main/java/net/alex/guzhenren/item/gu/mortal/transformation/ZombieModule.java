package net.alex.guzhenren.item.gu.mortal.transformation;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The zombie Gu's [僵尸蛊] module; the form's length comes from registration, the rung from the Gu's rank.
 *
 * <p>Nine Gu register with it (二转至五转). The gate refuses only "already 僵"; the payout delegates to
 * {@link BodyService} to enter temp zombie or, on a relapse, turn permanently 僵. {@code StateEffects} finds the Gu
 * of a rung by this module and {@link #getRung}.
 *
 * <p>⚠ Using one again too soon is not refused -- it is the penalty, and it makes the form permanent.
 * That branch leaves early, so anything both outcomes need has to sit ahead of it.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record ZombieModule(int tempZombieTicks) implements GuModule {

    private static final String FAILED_ALREADY_ZOMBIE = "guzhenren.item.failed.zombie_already";

    public static int getRung(MortalGuItem gu) { return gu.getRank().ordinal() - Rank.TWO.ordinal(); }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return BodyService.isZombie(player) ? new Refusal(FAILED_ALREADY_ZOMBIE) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        if (BodyService.isWithinRelapseWindow(player)) {
            BodyService.turnZombie(player, getRung(gu));
            return;
        }
        BodyService.enterTempZombie(player, getRung(gu), tempZombieTicks);
    }
}
