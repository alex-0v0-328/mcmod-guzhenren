package net.alex.guzhenren.item.gu.mortal.path.metal;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TemperingBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The Iron Bone Gu's [铁骨蛊] behavior: makes the holder's bones [骨] iron for good, in the rank registration names;
 * the max health and knockback resistance it adds are the grade's. Extends {@link TemperingBehavior}; the write is
 * {@link BodyService#temperBone}.
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
