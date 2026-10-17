package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.attribute.MaxHealthService;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;

/**
 * The one cross-domain lifecycle service: birth, sleep, death, clone, respawn, and a full reset. A clone
 * inherits every attachment {@link ModAttachments} registers as inherited -- NeoForge copies them -- and this
 * is the single place that decides what a death takes back: a death that wipes resets them all
 * ({@link #resetAll}), a keepInventory death only the return point. Every domain service re-runs on
 * join/clone/reset. Birth and reset write whole attachments here, past the domain services, which are the
 * only runtime writers.
 *
 * <p>⚠ The {@code Player} (not {@code ServerPlayer}) signature on {@code onClone}/{@code onBirth}/{@code
 * resetAll} is the one carve-out from read-{@code Player}/write-{@code ServerPlayer}: during {@code
 * PlayerEvent.Clone} the fresh entity is typed {@code Player}; never widen a domain service. ⚠ {@code BORN}
 * stays inherited, or the next login re-rolls brilliance. ⚠ A new death needs an {@code
 * onRespawn} line; its un-fire returns BARE values (soul 1, mind 0), on which the lethal check never fires.
 *
 * <p>{@link #dropHumanApertures}: a death that wipes the apertures shakes one Human Aperture [人窍]
 * loose per aperture, each at its own rank, at the corpse; keepInventory deaths keep the apertures
 * and drop nothing.
 *
 * <p>Losing a Vital Gu [本命蛊] is charged by {@link VitalLossService}.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @see BodyService
 * @since 1.0.0
 */

public final class PlayerDataService {

    private PlayerDataService() {}

    public static void onJoin(@NotNull ServerPlayer player) {
        if (!player.getData(ModAttachments.BORN)) onBirth(player);
        BodyService.skipOfflineLifespan(player);
        ApertureService.syncTalentMarks(player);
        MaxHealthService.refresh(player);
        MaxHealthService.restoreCarried(player);
        AttackDamageService.refresh(player);
        EpicFightIntegration.refresh(player);
    }

    public static void onLogout(@NotNull ServerPlayer player) { MaxHealthService.carry(player); }

    public static void onBirth(@NotNull Player player) {
        player.setData(ModAttachments.MIND, MindData.newborn());
        player.setData(ModAttachments.BORN, true);
    }

    public static void onSleepComplete(@NotNull ServerPlayer player) {
        SoulService.refill(player);
        ApertureEssenceService.refill(player);
        MindService.onSleepComplete(player);
    }

    public static void onClone(@NotNull Player from, @NotNull Player to, boolean wasDeath, boolean keepInventory) {
        if (wasDeath && !keepInventory) {
            dropHumanApertures(from);
            resetAll(to);
        } else if (wasDeath) {
            to.removeData(ModAttachments.DIMENSION_RETURN);
        }
        if (to instanceof ServerPlayer server) {
            MaxHealthService.refresh(server);
            server.setHealth(wasDeath ? server.getMaxHealth() : from.getHealth());
            AttackDamageService.refresh(server);
            EpicFightIntegration.refresh(server);
        }
    }

    private static void dropHumanApertures(@NotNull Player from) {
        ApertureData data = from.getData(ModAttachments.APERTURE);
        for (int i = 0; i < data.count(); i++) {
            Item drop = ModItems.getHumanAperture(data.get(i).rank());
            if (drop == null) continue;
            from.level().addFreshEntity(new ItemEntity(from.level(), from.getX(), from.getY(), from.getZ(),
                    new ItemStack(drop)));
        }
    }

    public static void onRespawn(@NotNull ServerPlayer player) {
        BodyService.revive(player);
        if (AperturePressureService.isFull(player)) AperturePressureService.set(player, ApertureData.PRIMARY, 0);
        if (BodyService.get(player).isExhausted()) {
            BodyService.setLifespan(player, BodyData.DEFAULT_LIFESPAN);
        }
        if (SoulService.get(player).isCollapsed()) {
            SoulService.revive(player);
        }
        if (MindService.get(player).isOverflowing()) {
            MindService.empty(player);
        }
        BodyService.clearDeathQiDebt(player);
        PathQiService.set(player, QiKind.DEATH, 0L);
    }

    public static void resetAll(@NotNull Player player) {
        long lastDayIndex = BodyService.get(player).lastDayIndex();
        for (AttachmentType<?> type : ModAttachments.getInherited()) player.removeData(type);
        player.removeData(ModAttachments.ESSENCE_CARRY);
        player.setData(ModAttachments.BODY, BodyData.DEFAULT.withLastDayIndex(lastDayIndex));
        onBirth(player);

        if (player instanceof ServerPlayer server) {
            MaxHealthService.refresh(server);
            AttackDamageService.refresh(server);
            EpicFightIntegration.refresh(server);
        }
    }
}
