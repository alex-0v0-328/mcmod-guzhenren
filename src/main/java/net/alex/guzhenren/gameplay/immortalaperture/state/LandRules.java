package net.alex.guzhenren.gameplay.immortalaperture.state;

import net.alex.guzhenren.registry.block.ModBlockTags;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 福地各功能共用的世界判定：什么算地面、整地上下各动多少格、哪些生物归地灵支使。地图扫描、开垦、圈地、养殖、挪生物都按
 * 这一份判，客户端选址预判才与服务端对得上。
 * <ul>
 *   <li>{@link #isPassable}：找真实地面时跳过的方块（空气、树叶、原木、可替换方块、植物、栅栏与栅栏门，以及
 *       {@link #isThin} 的薄件）；先碰到的其余方块即地面。</li>
 *   <li>{@link #isThin}：没有碰撞箱的（火把、铁轨、告示牌、旗帜、按钮、拉杆、压力板、红石线、蜘蛛网等，细雪除外：它对
 *       空碰撞上下文也返回空形状）与 {@link ModBlockTags#NOT_GROUND} 的薄件；地图上压平、取下面地面的颜色（雪层画雪色）。
 *       台阶、楼梯、活板门、床、箱子等有实在碰撞箱的仍算地面。</li>
 *   <li>{@link #isPlant}：原版 {@link BushBlock}（花、草丛、树苗、作物、蘑菇、浆果丛、睡莲等）与甘蔗；不算地面，圈地时
 *       被栅栏替换。仙人掌、竹子有碰撞箱，仍算地面。</li>
 *   <li>{@link #FILL_DEPTH}、{@link #CLEAR_HEIGHT}：开垦整平时地面下补土、地面上清空的格数；地图片只认这个范围内的方块
 *       实体（地下矿井里的箱子不挡开田）。</li>
 *   <li>{@link #isManaged}：活着的非玩家 {@link Mob}，排除地灵、已驯服（有主人 UUID）、已命名、拴着绳和
 *       {@link ModEntityTypeTags#SOULSUNDER_IMMUNE} 的。</li>
 * </ul>
 */

public final class LandRules {

    private LandRules() {}

    public static final int FILL_DEPTH = 8;
    public static final int CLEAR_HEIGHT = 32;

    public static boolean isPassable(@NotNull BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.canBeReplaced()
                || isPlant(state) || state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES) || isThin(state);
    }

    public static boolean isThin(@NotNull BlockState state) {
        if (state.is(ModBlockTags.NOT_GROUND)) return true;
        return !state.is(Blocks.POWDER_SNOW) && state.getFluidState().isEmpty()
                && state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
    }

    public static boolean isPlant(@NotNull BlockState state) {
        return state.getBlock() instanceof BushBlock || state.is(Blocks.SUGAR_CANE);
    }

    public static boolean isManaged(@NotNull Entity entity) {
        if (!(entity instanceof Mob mob) || !mob.isAlive() || entity instanceof EarthSpiritEntity) return false;
        if (mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) return false;
        if (mob.hasCustomName() || mob instanceof Leashable leashable && leashable.isLeashed()) return false;
        return !mob.getType().is(ModEntityTypeTags.SOULSUNDER_IMMUNE);
    }
}
