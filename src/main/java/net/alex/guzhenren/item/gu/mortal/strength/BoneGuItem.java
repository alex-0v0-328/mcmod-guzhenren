package net.alex.guzhenren.item.gu.mortal.strength;

import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The Iron Bone Gu [铁骨蛊] and the Essence Iron Bone Gu [精铁骨蛊]: make the holder's bones [骨] iron for good;
 * the max health and knockback resistance they add are the grade's. Extends {@link TemperingGuItem}; the
 * write is {@link BodyService#temperBone}.
 *
 * @author Alex
 * @version 1.0.0
 * @see Bone
 * @since 1.0.0
 */

public final class BoneGuItem extends TemperingGuItem<Bone> {

    private static final String FAILED_BONE_TAKEN = "guzhenren.item.failed.bone_taken";

    public BoneGuItem(Properties properties, Bone bone, GuSpec spec) {
        super(properties, bone, FAILED_BONE_TAKEN, spec);
    }

    @Override
    protected @NotNull Bone getWorn(Player player) { return BodyService.getBone(player); }

    @Override
    protected void temper(ServerPlayer player) { BodyService.temperBone(player, getGrade()); }
}
