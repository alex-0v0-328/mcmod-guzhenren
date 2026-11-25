package net.alex.guzhenren.gameplay.secretrealm;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.place.PlaceTitle;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 荡魂山[Soulsunder Mountain]：狐仙福地[Fox Immortal Blissful Land]中心唯一的半透明山，由 Gu World 结构集
 * {@code guworld:soulsunder_mountain} 定位；九转魂道秘境（{@link #rank}、{@link #path}）。
 * <ul>
 *   <li>范围为山心水平 {@value #RADIUS} 格圆柱。玩家每 10 秒（{@code tickCount} 为 {@value #SOUL_DRAIN_INTERVAL_TICKS}
 *       刻整倍的心跳）扣 {@value #SOUL_DRAIN} 点当前魂魄，扣到 0 由 {@code Heartbeat} 末步的致死检查按魂魄衰竭处死；扣
 *       之前发 {@link SoulsunderShakeEvent}，被取消就不扣。其它活着的生物每秒受 {@value #MOB_DAMAGE} 点
 *       {@link ModDamageTypes#SOULSUNDER}（穿甲、无击退），不刻更新的区块里的生物不扣。创造、旁观模式与
 *       {@link ModEntityTypeTags#SOULSUNDER_IMMUNE} 免疫。</li>
 *   <li>范围内生命上限不低于 {@value #MIN_MAX_HEALTH} 的生物死亡后，{@link #leaveGutsStone} 经 {@link #growGutsStone}
 *       在山心 {@value #GUTS_STONE_RADIUS} 格（不超过范围半径）内随机挑至多 {@value #GROW_ATTEMPTS} 列：须已加载、顶层
 *       运动阻挡方块为荡魂山石且其上可替换，才放一块胆石并记入 {@link GutsStones}；全数落空就不长。先有一半概率改为叠到
 *       随机一个已登记、未满 {@value GutsStones#MAX_STONES} 块的格子上（{@link #stackGutsStone}），格子未加载或已不是胆
 *       石就退回找新位置。同维度胆石满 {@value #GUTS_STONE_CAP} 块（按块数算）不再长。</li>
 *   <li>一转胆识蛊[Guts Gu]只在范围内能催动（未生成荡魂山时哪里都不能）。山已解出时，玩家每秒在范围外（含身处其它维度）
 *       就由 {@link #dissolveGutsGu} 清掉背包、光标、2×2 合成格与空窍存储（含本命槽）里的全部胆识蛊并提示一次（创造、
 *       旁观模式不清）；掉在范围外的胆识蛊由物品自身每秒检查 {@link #dissolvesAt} 后消失。箱子、漏斗等容器不管，收纳
 *       袋、潜影盒装不进。判定统一走 {@link #isGutsGu}。</li>
 *   <li>⚠ 性能：区域伤害每秒一次，只在 {@link SecretRealms} 判定附近有玩家后查实体；长胆石每次死亡至多
 *       {@value #GROW_ATTEMPTS} 次高度图查询，不扫描山体、不加载区块。</li>
 *   <li>{@link #damageMobs} 与 {@link #growGutsStone} 接受显式范围，GameTest 用小半径调用，避免波及并排运行的其它测试。
 *       </li>
 *   <li>⚠ 胆石方块以 {@code Block} 静态类型取用，不调 {@code GutsStoneBlock} 的方法：后者删坐标要回调
 *       {@link GutsStones}，反向调用会构成 {@code ArchitectureTest} 禁止的 block↔gameplay 包循环。</li>
 * </ul>
 */

public final class SoulsunderMountain implements SecretRealm {

    public static final SoulsunderMountain INSTANCE = new SoulsunderMountain();
    public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET,
            ResourceLocation.fromNamespaceAndPath("guworld", "soulsunder_mountain"));
    public static final int RADIUS = 96;
    public static final long SOUL_DRAIN = 1L;
    public static final int SOUL_DRAIN_INTERVAL_TICKS = 10 * Ticks.SECOND;
    public static final float MOB_DAMAGE = 2.0F;
    public static final float MIN_MAX_HEALTH = 20.0F;
    public static final int GUTS_STONE_RADIUS = 80;
    public static final int GUTS_STONE_CAP = 128;
    public static final int GROW_ATTEMPTS = 8;
    public static final int GUTS_GU_SOUL_BONUS = 10;
    public static final String MSG_GUTS_GU_DISSOLVED = "guzhenren.item.gu.dissolved_outside_soulsunder";
    public static final String NAME_KEY = "guzhenren.place.soulsunder_mountain";
    /** 荡魂山石贴图的主色。 */
    public static final int TITLE_COLOR = 0xF8ACC4;
    private static final PlaceTitle TITLE = new PlaceTitle(KIND_KEY, NAME_KEY, TITLE_COLOR);

    private SoulsunderMountain() {}

    @Override
    public @NotNull ResourceKey<Level> dimension() { return ModDimensions.FOX_IMMORTAL_BLISSFUL_LAND; }

    @Override
    public @NotNull ResourceKey<StructureSet> structureSet() { return STRUCTURE_SET; }

    @Override
    public int radius() { return RADIUS; }

    @Override
    public @NotNull Rank rank() { return Rank.NINE; }

    @Override
    public @NotNull GuPath path() { return GuPath.SOUL; }

    @Override
    public @NotNull PlaceTitle title() { return TITLE; }

    //region Hooks [钩子]
    @Override
    public void tickPlayer(@NotNull ServerPlayer player, @NotNull RealmSite site) {
        if (player.tickCount % SOUL_DRAIN_INTERVAL_TICKS == 0) drainSoul(player);
    }

    @Override
    public void tickPlayerAway(@NotNull ServerPlayer player, @NotNull RealmSite site) { dissolveGutsGu(player); }

    @Override
    public void tickArea(@NotNull ServerLevel level, @NotNull RealmSite site) { damageMobs(level, site); }

    @Override
    public void onDeath(@NotNull ServerLevel level, @NotNull RealmSite site, @NotNull LivingEntity entity) {
        if (!leavesGutsStone(entity)) return;
        leaveGutsStone(level, GutsStones.get(level), site.center(), Math.min(GUTS_STONE_RADIUS, site.radius()),
                entity.getRandom());
    }
    //endregion

    public static boolean leavesGutsStone(@NotNull LivingEntity entity) {
        return entity.getMaxHealth() >= MIN_MAX_HEALTH;
    }

    public static @Nullable BlockPos leaveGutsStone(@NotNull ServerLevel level, @NotNull GutsStones stones,
                                                    @NotNull BlockPos center, int radius,
                                                    @NotNull RandomSource random) {
        if (stones.total() >= GUTS_STONE_CAP) return null;
        if (random.nextBoolean()) {
            BlockPos stacked = stackGutsStone(level, stones, random);
            if (stacked != null) return stacked;
        }
        return growGutsStone(level, stones, center, radius, random);
    }

    //region Guts Gu [胆识蛊]
    public static boolean isInside(@NotNull ServerPlayer player) {
        return SecretRealms.siteAround(player.serverLevel(), INSTANCE, player.getX(), player.getZ()) != null;
    }

    public static boolean dissolvesAt(@NotNull ServerLevel level, @NotNull Entity entity) {
        RealmSite site = SecretRealms.siteOf(level.getServer(), INSTANCE);
        return site != null && (level.dimension() != SecretRealms.dimensionOf(INSTANCE) || !site.contains(entity));
    }

    public static int dissolveGutsGu(@NotNull ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return 0;

        int dissolved = player.getInventory().clearOrCountMatchingItems(SoulsunderMountain::isGutsGu, -1,
                player.inventoryMenu.getCraftSlots());
        if (dissolved > 0) {
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.slotsChanged(player.getInventory());
        }
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES; aperture++) {
            dissolved += dissolveStored(player, aperture);
            ItemStack vital = ApertureStorageService.getVital(player, aperture);
            if (isGutsGu(vital)) {
                ApertureStorageService.setVital(player, aperture, ItemStack.EMPTY);
                dissolved += vital.getCount();
            }
        }
        if (dissolved > 0) player.sendSystemMessage(Component.translatable(MSG_GUTS_GU_DISSOLVED));
        return dissolved;
    }

    private static int dissolveStored(ServerPlayer player, int aperture) {
        List<ItemStack> stored = ApertureStorageService.getView(player, aperture);
        int dissolved = 0;
        List<ItemStack> kept = new ArrayList<>(stored.size());
        for (ItemStack stack : stored) {
            if (isGutsGu(stack)) {
                dissolved += stack.getCount();
                kept.add(ItemStack.EMPTY);
            } else {
                kept.add(stack);
            }
        }
        if (dissolved > 0) ApertureStorageService.set(player, aperture, kept);
        return dissolved;
    }

    public static boolean isGutsGu(@NotNull ItemStack stack) { return stack.is(ModItems.GUTS_GU_1.get()); }
    //endregion

    //region Effects [效果]
    public static void drainSoul(@NotNull ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;
        SoulsunderShakeEvent shake = new SoulsunderShakeEvent(player);
        ModEvents.post(shake);
        if (shake.isCanceled()) return;
        SoulService.addCurrent(player, -SOUL_DRAIN);
    }

    public static void damageMobs(@NotNull ServerLevel level, @NotNull RealmSite site) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, site.box(level),
                entity -> isShaken(level, entity) && site.contains(entity))) {
            entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.SOULSUNDER), MOB_DAMAGE);
        }
    }

    private static boolean isShaken(ServerLevel level, LivingEntity entity) {
        return !(entity instanceof Player) && entity.isAlive() && level.isPositionEntityTicking(entity.blockPosition())
                && !entity.getType().is(ModEntityTypeTags.SOULSUNDER_IMMUNE);
    }

    public static @Nullable BlockPos growGutsStone(@NotNull ServerLevel level, @NotNull GutsStones stones,
                                                   @NotNull BlockPos center, int radius,
                                                   @NotNull RandomSource random) {
        if (stones.total() >= GUTS_STONE_CAP) return null;

        for (int attempt = 0; attempt < GROW_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            int x = center.getX() + Mth.floor(Math.cos(angle) * distance + 0.5D);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * distance + 0.5D);
            BlockPos pos = findGrowSpot(level, x, z);
            if (pos == null) continue;

            Block guts = ModBlocks.GUTS_STONE.get();
            level.setBlock(pos, guts.defaultBlockState(), Block.UPDATE_ALL);
            stones.add(pos);
            return pos;
        }
        return null;
    }

    public static @Nullable BlockPos stackGutsStone(@NotNull ServerLevel level, @NotNull GutsStones stones,
                                                    @NotNull RandomSource random) {
        if (stones.total() >= GUTS_STONE_CAP) return null;

        BlockPos pos = stones.pickStackable(random);
        if (pos == null || !level.isLoaded(pos)) return null;

        Block guts = ModBlocks.GUTS_STONE.get();
        BlockState state = level.getBlockState(pos);
        if (!state.is(guts)) {
            stones.remove(pos);
            return null;
        }
        int count = state.getValue(GutsStones.STONES) + 1;
        if (count > GutsStones.MAX_STONES) {
            stones.set(pos, GutsStones.MAX_STONES);
            return null;
        }
        level.setBlock(pos, state.setValue(GutsStones.STONES, count), Block.UPDATE_ALL);
        stones.set(pos, count);
        return pos;
    }

    public static @Nullable BlockPos findGrowSpot(@NotNull ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return null;

        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        if (pos.getY() >= level.getMaxBuildHeight()) return null;
        if (!level.getBlockState(pos.below()).is(ModBlocks.SOULSUNDER_STONE.get())) return null;
        return level.getBlockState(pos).canBeReplaced() ? pos : null;
    }
    //endregion
}
