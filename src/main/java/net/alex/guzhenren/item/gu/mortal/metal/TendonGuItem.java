package net.alex.guzhenren.item.gu.mortal.metal;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.mortal.TemperingGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The Steel Tendon Gu [钢筋蛊]: makes the holder's tendons [筋] steel for good, in the rank registration names;
 * the capacity it adds is the grade's. Extends {@link TemperingGuItem}; the write is
 * {@link BodyService#temperTendon}.
 *
 * @author Alex
 * @version 1.0.0
 * @see Tendon
 * @since 1.0.0
 */

public final class TendonGuItem extends TemperingGuItem<Tendon> {

    private static final String FAILED_TENDON_TAKEN = "guzhenren.item.failed.tendon_taken";

    public TendonGuItem(Properties properties, Tendon tendon, GuSpec spec) {
        super(properties, tendon, FAILED_TENDON_TAKEN, spec);
    }

    @Override
    protected @NotNull Tendon getWorn(Player player) { return BodyService.getTendon(player); }

    @Override
    protected void temper(ServerPlayer player) { BodyService.temperTendon(player, getGrade()); }
}
