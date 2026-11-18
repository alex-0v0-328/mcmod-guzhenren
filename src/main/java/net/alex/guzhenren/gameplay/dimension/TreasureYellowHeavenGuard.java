package net.alex.guzhenren.gameplay.dimension;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.TossedItems;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 宝黄天[Treasure Yellow Heaven]维度的交互限制与安全守卫服务。
 * <ul>
 *   <li>维度内仅允许移动、飞行、聊天、与 {@link SoulTraderEntity} 右键交易以及执行退出指令；其余方块破坏、放置、物品使
 *       用、攻击（含攻击交易商）以及玩家掉入虚空伤害一律拒绝，跌出最低高度时由 {@link #tick} 拉回出生点。</li>
 *   <li>破坏、放置、右键使用与攻击在客户端和服务端双端同时拦截：客户端直接取消本地预测（避免出现幽灵方块、挥手或使用动
 *       画），服务端作最终权威裁定；丢弃物品与伤害拦截仅在服务端执行，其中丢弃拦截是唯一向动作栏发送提示的检查。</li>
 *   <li>{@link #tick}：当玩家以任何途径离开宝黄天（退出指令、{@code /tp}、跨模组传送或死亡重生）且未进入另一个授予飞行
 *       的锚定维度时，立即撤销维度飞行授权并将 {@link TossStashData} 中暂存的物品吐在脚下。</li>
 *   <li>⚠ 宝黄天内绝不生成掉落物实体：{@link #stowOrStash} 通过 {@link TossedItems#putBack} 将丢弃的物品直接塞回背包，
 *       塞不下的部分存入 {@link TossStashData} 并提示背包已满，防止出现悬浮或消失的掉落物。</li>
 *   <li>{@link #stowOrStash} 与 {@link #dropStash} 本身不校验当前维度，以便在无宝黄天维度的 GameTest 环境中直接测
 *       试。</li>
 * </ul>
 */

public final class TreasureYellowHeavenGuard {

    private static final String INVENTORY_FULL = "guzhenren.dimension.treasure_yellow_heaven.inventory_full";

    private TreasureYellowHeavenGuard() {}

    public static boolean isInside(Entity entity) {
        return entity.level().dimension().equals(ModDimensions.TREASURE_YELLOW_HEAVEN);
    }

    public static boolean isSoulTrader(Entity target) {
        return target instanceof SoulTraderEntity;
    }

    public static boolean isBreakRefused(Player player) { return isInside(player); }

    public static boolean isPlaceRefused(@Nullable Entity placer) {
        return placer instanceof Player player && isInside(player);
    }

    public static boolean isUseRefused(Player player, @Nullable Entity target) {
        return isInside(player) && !isSoulTrader(target);
    }

    public static boolean isAttackRefused(Player player) { return isInside(player); }

    public static boolean tryStowToss(Player player, ItemEntity tossed) {
        if (player.level().isClientSide() || !isInside(player)) return false;
        stowOrStash(player, tossed);
        return true;
    }

    public static void stowOrStash(Player player, ItemEntity tossed) {
        ItemStack rest = TossedItems.putBack(player, tossed);
        if (rest.isEmpty()) return;

        PlayerData data = PlayerData.get(player);
        data.setTossStash(data.getTossStash().with(rest));
        ActionBar.refuse(player, INVENTORY_FULL);
    }

    public static void dropStash(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        TossStashData stash = data.getTossStash();
        if (stash.isEmpty()) return;

        data.setTossStash(TossStashData.DEFAULT);
        for (ItemStack stack : stash.stacks()) player.spawnAtLocation(stack.copy());
    }

    public static boolean isDamageRefused(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide()) return false;
        if (source.getEntity() instanceof Player attacker && isInside(attacker)) return true;
        return victim instanceof Player && isInside(victim) && source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    public static void tick(ServerPlayer player) {
        if (!isInside(player)) {
            if (!ModDimensions.isFlightGranted(player.level().dimension())) {
                DimensionTravelService.revokeFlight(player);
            }
            dropStash(player);
            return;
        }
        DimensionTravelService.ensureFlight(player);
        DimensionTravelService.rescueIfBelowVoid(player, ModDimensions.TREASURE_YELLOW_HEAVEN_SPAWN);
    }
}
