package net.alex.guzhenren.item.gu.mortal.metal;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.TemperingModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The Steel Tendon Gu's [钢筋蛊] module: makes the holder's tendons [筋] steel for good, in the rank registration
 * names; the capacity it adds is the grade's. Extends {@link TemperingModule}; the write is
 * {@link BodyService#temperTendon}.
 *
 * @author Alex
 * @version 1.0.0
 * @see Tendon
 * @since 1.0.0
 */

public final class TendonModule extends TemperingModule<Tendon> {

    private static final String FAILED_TENDON_TAKEN = "guzhenren.item.failed.tendon_taken";

    public TendonModule(Tendon tendon) {
        super(tendon, FAILED_TENDON_TAKEN);
    }

    @Override
    protected @NotNull Tendon getWorn(Player player) { return BodyService.getTendon(player); }

    @Override
    protected void temper(MortalGuItem gu, ServerPlayer player) { BodyService.temperTendon(player, getGrade()); }
}
