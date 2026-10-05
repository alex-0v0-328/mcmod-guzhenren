package net.alex.guzhenren.gameplay.lifecycle;

import java.util.UUID;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * What losing a Vital Gu [本命蛊] costs its owner: half the soul [魂魄], half of every mind pool [脑海],
 * {@link #LOST_HEALTH_FRACTION} of the current health, and the bound aperture's primary path [主修].
 * {@link #onVitalGuLost} charges an online owner at once; {@link #recordOfflineVitalLoss} parks the
 * stack in {@link PendingVitalPenalties} for an offline one.
 *
 * <p>{@link #settleOfflineVitalLoss} waits out vanilla's 60-tick spawn invulnerability, which would
 * swallow the 80% hurt, and settles one lost Gu per heartbeat so the next hurt clears the 10-tick
 * hurt cooldown.
 *
 * @author Alex
 * @version 1.0.0
 * @see PendingVitalPenalties
 * @see PlayerTickEvents
 * @since 1.0.0
 */

public final class VitalLossService {

    private VitalLossService() {}

    private static final String VITAL_LOST = "guzhenren.item.gu.vital_lost";
    public static final int OFFLINE_VITAL_SETTLE_AFTER_TICKS = 60;
    public static final float LOST_HEALTH_FRACTION = 0.8F;

    public static void onVitalGuLost(@NotNull ServerPlayer owner, @NotNull ItemStack stack) {
        owner.sendSystemMessage(Component.translatable(VITAL_LOST, stack.getHoverName()));

        SoulService.setCurrent(owner, SoulService.get(owner).currentSoul() / 2L);
        for (MindPoolType type : MindPoolType.values()) {
            MindService.setCurrent(owner, type, MindService.getCurrent(owner, type) / 2L);
        }
        owner.hurt(ModDamageTypes.source(owner, ModDamageTypes.VITAL_GU_LOST),
                owner.getHealth() * LOST_HEALTH_FRACTION);

        int bound = stack.getOrDefault(ModDataComponents.VITAL_APERTURE.get(), ApertureData.PRIMARY);
        ApertureService.setPrimaryPath(owner, bound, null);
    }

    public static void recordOfflineVitalLoss(@NotNull MinecraftServer server, @NotNull UUID owner,
            @NotNull ItemStack stack) {
        PendingVitalPenalties.get(server).record(owner, stack);
    }

    public static void settleOfflineVitalLoss(@NotNull ServerPlayer player) {
        if (player.tickCount <= OFFLINE_VITAL_SETTLE_AFTER_TICKS) return;

        ItemStack lost = PendingVitalPenalties.get(player.server).poll(player.getUUID());
        if (lost != null) onVitalGuLost(player, lost);
    }
}
