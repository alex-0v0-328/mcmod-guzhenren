package net.alex.guzhenren.item.gu.mortal.path.metal;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TemperingBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 钢筋蛊[Steel Tendon Gu]炼体行为：将玩家筋络永久淬炼为对应转数的钢筋档位（{@link Tendon}），肉身承受力加成由目标档位决
 * 定。
 */

public final class TendonBehavior extends TemperingBehavior<Tendon> {

    public TendonBehavior(Tendon tendon) {
        super(tendon);
    }

    @Override
    protected @NotNull Tendon getWorn(Player player) { return BodyService.getTendon(player); }

    @Override
    protected void temper(MortalGuItem gu, ServerPlayer player) { BodyService.temperTendon(player, getGrade()); }
}
