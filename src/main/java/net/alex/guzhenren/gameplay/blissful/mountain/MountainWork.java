package net.alex.guzhenren.gameplay.blissful.mountain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.blissful.herd.PastureWork;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.land.EarthSpiritEntity;
import net.alex.guzhenren.gameplay.secretrealm.GutsStones;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵面板「荡魂山」页的两个操作，以及养殖、挪生物共用的「地灵可支使的生物」判定。
 * <ul>
 *   <li>{@link #isManaged}：活着的非玩家 {@link Mob}，排除地灵、已驯服（有主人 UUID）、已命名、拴着绳和
 *       {@link ModEntityTypeTags#SOULSUNDER_IMMUNE} 的；只看已加载区块里的实体（{@code ServerLevel#getAllEntities}）。
 *       </li>
 *   <li>{@link #moveMobsUp}：把山范围外、各牧场栏外（{@link PastureWork#penner}）的可支使生物至多
 *       {@value #MOVE_LIMIT} 只传到山心 {@code GUTS_STONE_RADIUS} 格内、顶层为荡魂山石的列上（选点同长胆石，每只至多试
 *       {@value #MOVE_ATTEMPTS} 次，须已加载），之后冷却 {@value #MOVE_COOLDOWN_TICKS} 刻；调用方先查
 *       {@link #moveCooldown}。野生蛊不挪：生命只有 1，上山即被震死，也不长胆石。</li>
 *   <li>{@link #driveGutsGu}：主人须在山范围内；敲碎山范围内、已加载区块里登记的全部胆石，每块按胆石方块自己的战利品表
 *       各掷一次（同手敲的 50%），掷出的胆识蛊[Guts Gu]不成物品，直接给主人魂魄上限各加
 *       {@value SoulsunderMountain#GUTS_GU_SOUL_BONUS}。登记了但已不是胆石的格子顺手删去。</li>
 *   <li>催动有意不走胆识蛊物品的 {@code OneShotGuItem#checkUse}（真元门槛 {@code checkEssenceGate}）：认主即可一键催
 *       动，没开窍的主人也能拿到魂魄上限（Alex 定）。</li>
 *   <li>两个操作都接受显式范围，挪生物另可传入候选实体；GameTest 用小范围与测试区内的实体调用，不碰别的测试。</li>
 * </ul>
 */

public final class MountainWork {

    private MountainWork() {}

    public static final int MOVE_LIMIT = 64;
    public static final int MOVE_ATTEMPTS = 16;
    public static final int MOVE_COOLDOWN_TICKS = 60 * Ticks.SECOND;

    public record Drive(int stones, int gu) {}

    public static boolean isManaged(@NotNull Entity entity) {
        if (!(entity instanceof Mob mob) || !mob.isAlive() || entity instanceof EarthSpiritEntity) return false;
        if (mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) return false;
        if (mob.hasCustomName() || mob instanceof Leashable leashable && leashable.isLeashed()) return false;
        return !mob.getType().is(ModEntityTypeTags.SOULSUNDER_IMMUNE);
    }

    //region Move [挪生物上山]
    public static long moveCooldown(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        return Math.max(0L, data.mountain().moveReadyAt() - level.getGameTime());
    }

    public static int moveMobsUp(@NotNull ServerLevel level, @NotNull RealmSite site, @NotNull BlissfulLandData data,
                                 @NotNull RandomSource random) {
        return moveMobsUp(level, site, data, random, level.getAllEntities());
    }

    public static int moveMobsUp(@NotNull ServerLevel level, @NotNull RealmSite site, @NotNull BlissfulLandData data,
                                 @NotNull RandomSource random, @NotNull Iterable<? extends Entity> candidates) {
        List<Mob> movers = new ArrayList<>();
        Predicate<Entity> penned = PastureWork.penner(data);
        for (Entity entity : candidates) {
            if (movers.size() >= MOVE_LIMIT) break;
            if (entity.getType().is(ModEntityTypeTags.WILD_GU) || !isManaged(entity) || site.contains(entity)) continue;
            if (!penned.test(entity)) movers.add((Mob) entity);
        }
        int radius = Math.min(SoulsunderMountain.GUTS_STONE_RADIUS, site.radius());
        int moved = 0;
        for (Mob mob : movers) {
            BlockPos spot = findSpot(level, site.center(), radius, random);
            if (spot == null) continue;
            mob.teleportTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
            mob.setDeltaMovement(Vec3.ZERO);
            moved++;
        }
        data.mountain().setMoveReadyAt(level.getGameTime() + MOVE_COOLDOWN_TICKS);
        return moved;
    }

    private static @Nullable BlockPos findSpot(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        for (int attempt = 0; attempt < MOVE_ATTEMPTS; attempt++) {
            BlockPos column = SoulsunderMountain.pickColumn(center, radius, random);
            if (column == null) continue;
            BlockPos spot = SoulsunderMountain.findGrowSpot(level, column.getX(), column.getZ());
            if (spot != null && level.getBlockState(spot.above()).canBeReplaced()) return spot;
        }
        return null;
    }
    //endregion

    //region Drive [胆识蛊催动]
    public static @Nullable Drive driveGutsGu(@NotNull ServerLevel level, @NotNull RealmSite site,
                                              @NotNull ServerPlayer owner) {
        if (owner.level() != level || !site.contains(owner)) return null;

        GutsStones stones = GutsStones.get(level);
        Block guts = ModBlocks.GUTS_STONE.get();
        int broken = 0;
        int gu = 0;
        for (long packed : stones.positions()) {
            BlockPos pos = BlockPos.of(packed);
            if (!site.contains(pos.getX() + 0.5D, pos.getZ() + 0.5D) || !level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.is(guts)) {
                stones.remove(pos);
                continue;
            }
            int count = state.getValue(GutsStones.STONES);
            BlockState single = state.setValue(GutsStones.STONES, 1);
            for (int i = 0; i < count; i++) gu += rollGu(level, pos, single);
            broken += count;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (gu > 0) SoulService.addMax(owner, (long) gu * SoulsunderMountain.GUTS_GU_SOUL_BONUS);
        return new Drive(broken, gu);
    }

    private static int rollGu(ServerLevel level, BlockPos pos, BlockState state) {
        LootParams.Builder params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
        int gu = 0;
        for (ItemStack drop : state.getDrops(params)) {
            if (SoulsunderMountain.isGutsGu(drop)) gu += drop.getCount();
        }
        return gu;
    }
    //endregion
}
