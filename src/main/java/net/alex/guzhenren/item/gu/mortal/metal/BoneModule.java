package net.alex.guzhenren.item.gu.mortal.metal;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.TemperingModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The Iron Bone Gu's [铁骨蛊] module: makes the holder's bones [骨] iron for good, in the rank registration names;
 * the max health and knockback resistance it adds are the grade's. Extends {@link TemperingModule}; the write is
 * {@link BodyService#temperBone}.
 *
 * @author Alex
 * @version 1.0.0
 * @see Bone
 * @since 1.0.0
 */

public final class BoneModule extends TemperingModule<Bone> {

    private static final String FAILED_BONE_TAKEN = "guzhenren.item.failed.bone_taken";

    public BoneModule(Bone bone) {
        super(bone, FAILED_BONE_TAKEN);
    }

    @Override
    protected @NotNull Bone getWorn(Player player) { return BodyService.getBone(player); }

    @Override
    protected void temper(MortalGuItem gu, ServerPlayer player) { BodyService.temperBone(player, getGrade()); }
}
