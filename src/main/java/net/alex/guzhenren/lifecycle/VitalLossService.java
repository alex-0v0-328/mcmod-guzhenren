package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.VitalGuLostEvent;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 本命蛊损毁时对绑定主人施加反噬惩罚与离线补扣的服务（由 {@link Heartbeat} 定时结算离线队列）。
 * <ul>
 *   <li>损毁惩罚固定将当前魂魄与各脑海池减半，造成当前生命值 {@link #LOST_HEALTH_FRACTION} 的伤害，并清空该本命蛊所绑定
 *       空窍的主修流派。</li>
 *   <li>{@link #onVitalGuLost} 响应 {@link VitalGuLostEvent}：若主人在线则立即扣除惩罚，离线则写入
 *       {@link PendingVitalPenalties} 存档队列。</li>
 *   <li>{@link #settleOfflineVitalLoss} 在玩家上线并度过原版 {@link #OFFLINE_VITAL_SETTLE_AFTER_TICKS} 刻出生无敌后，每
 *       次心跳至多结算一条待处理惩罚，使下一次 80% 伤害能跨过 10 刻受伤无敌帧。</li>
 * </ul>
 */

public final class VitalLossService {

    private VitalLossService() {}

    private static final String VITAL_LOST = "guzhenren.item.gu.vital_lost";
    public static final int OFFLINE_VITAL_SETTLE_AFTER_TICKS = 60;
    public static final float LOST_HEALTH_FRACTION = 0.8F;

    public static void onVitalGuLost(@NotNull VitalGuLostEvent event) {
        MinecraftServer server = event.getServer();
        ServerPlayer owner = server.getPlayerList().getPlayer(event.getOwner());
        if (owner != null) charge(owner, event.getStack());
        else PendingVitalPenalties.get(server).recordPenalty(event.getOwner(), event.getStack());
    }

    private static void charge(ServerPlayer owner, ItemStack stack) {
        if (!VitalGuState.isCurrentVital(stack, owner)) return;

        owner.sendSystemMessage(Component.translatable(VITAL_LOST, stack.getHoverName()));

        SoulService.setCurrent(owner, SoulService.get(owner).currentSoul() / 2L);
        for (MindPoolType type : MindPoolType.values()) {
            MindService.setCurrent(owner, type, MindService.getCurrent(owner, type) / 2L);
        }
        owner.hurt(ModDamageTypes.source(owner, ModDamageTypes.VITAL_GU_LOST),
                owner.getHealth() * LOST_HEALTH_FRACTION);

        int bound = VitalGuState.getBoundIndex(stack, owner);
        if (bound >= 0) ApertureService.setPrimaryPath(owner, bound, null);
    }

    public static void settleOfflineVitalLoss(@NotNull ServerPlayer player) {
        if (player.tickCount <= OFFLINE_VITAL_SETTLE_AFTER_TICKS) return;

        ItemStack lost = PendingVitalPenalties.get(player.server).poll(player.getUUID());
        if (lost != null) charge(player, lost);
    }
}
