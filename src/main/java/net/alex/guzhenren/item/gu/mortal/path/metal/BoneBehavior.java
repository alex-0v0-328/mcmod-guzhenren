package net.alex.guzhenren.item.gu.mortal.path.metal;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TemperingBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 铁骨蛊[Iron Bone Gu]炼体行为：将玩家骨骼永久淬炼为对应转数的铁骨档位（{@link Bone}），最大生命值与击退抗性加成由目标
 * 档位决定。
 */

public final class BoneBehavior extends TemperingBehavior<Bone> {

    public BoneBehavior(Bone bone) {
        super(bone);
    }

    @Override
    protected @NotNull Bone getWorn(Player player) { return BodyService.getBone(player); }

    @Override
    protected void temper(MortalGuItem gu, ServerPlayer player) { BodyService.temperBone(player, getGrade()); }
}
