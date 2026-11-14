package net.alex.guzhenren.item.gu.mortal.path.metal;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TemperingBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 铁骨蛊[Iron Bone Gu]的行为：骨永久变为同转铁骨，最大生命与击退抗性加成取该档。
 *
 * @author Alex
 * @version 1.0.0
 * @see Bone
 * @since 1.0.0
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
