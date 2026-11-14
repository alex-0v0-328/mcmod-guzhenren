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
 * 失去本命蛊[Vital Gu]对主人的代价。
 * <ul>
 *   <li>魂魄、各脑海池减半，扣当前生命的 {@link #LOST_HEALTH_FRACTION}，清所绑空窍[Aperture]的主修流派。</li>
 *   <li>{@link #onVitalGuLost} 收 {@link VitalGuLostEvent}：在线即扣，离线存入 {@link PendingVitalPenalties}。</li>
 *   <li>{@link #settleOfflineVitalLoss} 先等过原版 60 tick 出生无敌（否则吞掉 80% 伤害），每次心跳结算一个，下次伤害才
 *       越过 10 tick 受伤冷却。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PendingVitalPenalties
 * @see Heartbeat
 * @since 1.0.0
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
