package net.alex.guzhenren.item.gu.mortal.transformation;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The shared class behind every zombie Gu [僵尸蛊]; the form's length comes from registration.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.TendedGuItem}. Nine Gu register against this one
 * class (二转至五转). The gate refuses only "already 僵"; the payout delegates to
 * {@link BodyService} to enter temp zombie or, on a
 * relapse, turn permanently 僵.
 *
 * <p>⚠ Using one again too soon is not refused -- it is the penalty, and it makes the form permanent.
 * That branch leaves early, so anything both outcomes need has to sit ahead of it.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public class ZombieGuItem extends TendedGuItem {

    private static final String FAILED_ALREADY_ZOMBIE = "guzhenren.item.failed.zombie_already";
    private final int tempZombieTicks;

    public ZombieGuItem(Properties properties, int tempZombieTicks, GuSpec spec) {
        super(properties, spec);
        this.tempZombieTicks = tempZombieTicks;
    }

    public int getRung() { return getRank().ordinal() - Rank.TWO.ordinal(); }

    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        return BodyService.isZombie(player) ? new Refusal(FAILED_ALREADY_ZOMBIE) : null;
    }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {
        if (BodyService.isWithinRelapseWindow(player)) {
            BodyService.turnZombie(player, getRung());
            return;
        }
        BodyService.enterTempZombie(player, getRung(), tempZombieTicks);
    }
}
