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
 * 宝黄天[Treasure Yellow Heaven]的服务端权威规则。
 * <ul>
 *   <li>只许移动、飞行、聊天、退出命令，其余交互一律拒；虚空伤害也拒，交 {@link #tick} 拉回出生点。</li>
 *   <li>破坏、放置、使用、攻击两端同判：客户端知道所在维度，拒绝即不预测（无幽灵方块、挥手、使用动画），仍发包，服务端
 *       为准；丢弃与伤害只在服务端判，丢弃是唯一提示玩家的检查，只跑一次。</li>
 *   <li>{@link #tick}：任何途径离开（退出、原版 {@code /tp}、他模组传送、死后重生）都撤飞行授权，除非站在另一个给飞行的
 *       锚定维度，并把 {@link TossStashData} 掉在脚下。</li>
 *   <li>唯一可右键的实体是 {@link SoulTraderEntity}，此天本为交易；攻击它照拒；{@link #isUseRefused} 点方块或手持物品时
 *       传 {@code null}。</li>
 *   <li>⚠ 不发生丢弃：{@link #stowOrStash} 经 {@link TossedItems#putBack}（从不掉落）放回，放不下的暂存到离开并提示背包
 *       已满；物品实体从不进入，无悬浮或消失的掉落物。</li>
 *   <li>{@link #stowOrStash}、{@link #dropStash} 不查维度：GameTest 世界没有真宝黄天。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TreasureYellowHeavenGuardEvents
 * @see net.alex.guzhenren.gameplay.dimension.DimensionTravelService
 * @since 1.0.0
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
