package net.alex.guzhenren.gameplay.lifecycle;

import java.util.UUID;
import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.aperture.storage.PendingVitalPenalties;
import net.alex.guzhenren.gameplay.body.BodyAttackService;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyHealthService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.WisdomType;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.path.qi.PathQiData;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The one cross-domain lifecycle service: birth, sleep, death, clone, respawn, and a full reset. It is
 * the single place that decides what a clone inherits; every domain service re-runs on join/clone/reset.
 *
 * <p>⚠ The {@code Player} (not {@code ServerPlayer}) signature on {@code copy}/{@code onBirth}/{@code
 * resetAll} is the one carve-out from read-{@code Player}/write-{@code ServerPlayer}: during {@code
 * PlayerEvent.Clone} the fresh entity is typed {@code Player}; never widen a domain service. ⚠ {@code
 * copy} must carry {@code BORN} or the next login re-rolls brilliance. ⚠ A new death needs an {@code
 * onRespawn} line; its un-fire returns BARE values (soul 1, mind 0), on which the lethal check never fires.
 *
 * <p>{@link #dropHumanApertures}: a death that wipes the apertures shakes one Human Aperture [人窍]
 * loose per aperture, each at its own rank, at the corpse; keepInventory deaths keep the apertures
 * and drop nothing.
 *
 * <p>{@link #settleOfflineVitalLoss} waits out vanilla's 60-tick spawn invulnerability, which would
 * swallow the 80% hurt, and settles one lost Gu per heartbeat so the next hurt clears the 10-tick
 * hurt cooldown.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @see BodyService
 * @since 1.0.0
 */

public final class PlayerDataService {

    private static final String VITAL_LOST = "guzhenren.item.gu.vital_lost";
    public static final int OFFLINE_VITAL_SETTLE_AFTER_TICKS = 60;

    private PlayerDataService() {}

    public static void onJoin(@NotNull ServerPlayer player) {
        if (!player.getData(ModAttachments.BORN)) onBirth(player);
        migratePhysique(player);
        ApertureService.syncTalentMarks(player);
        BodyHealthService.refresh(player);
        BodyAttackService.refresh(player);
        EpicFightIntegration.refresh(player);
    }

    private static void migratePhysique(@NotNull ServerPlayer player) {
        Aperture aperture = ApertureService.aperture(player);
        ExtremePhysique legacy = aperture.legacyExtremePhysique();
        if (!BodyService.isExtreme(player) && legacy != null && legacy != ExtremePhysique.NONE) {
            BodyService.setExtremePhysique(player, legacy);
        } else if (!BodyService.isExtreme(player) && aperture.baseEssence() == Aperture.MAX_BASE) {
            ApertureService.set(player, ApertureData.PRIMARY, aperture.withBaseEssence(Aperture.MAX_BASE - 1)
                    .withPressure(0));
        }
        aperture = ApertureService.aperture(player);
        if (aperture.legacyExtremePhysique() != null) {
            ApertureService.set(player, ApertureData.PRIMARY, aperture.clearLegacyExtremePhysique());
        }
    }

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
        if (wasDeath) {
            if (!keepInventory) {
                dropHumanApertures(from);
                resetAll(to);
            } else {
                copy(from, to);
                to.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.DEFAULT);
            }
        } else {
            copy(from, to);
        }
        if (to instanceof ServerPlayer server) {
            BodyHealthService.refresh(server);
            BodyAttackService.refresh(server);
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

    public static void onVitalGuLost(@NotNull ServerPlayer owner, @NotNull ItemStack stack) {
        owner.sendSystemMessage(Component.translatable(VITAL_LOST, stack.getHoverName()));

        SoulService.setCurrent(owner, SoulService.get(owner).currentSoul() / 2L);
        for (WisdomType type : WisdomType.values()) {
            MindService.setCurrent(owner, type, MindService.current(owner, type) / 2L);
        }
        owner.hurt(ModDamageTypes.source(owner, ModDamageTypes.VITAL_GU_LOST), owner.getHealth() * 0.8F);

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

    private static void copy(@NotNull Player from, @NotNull Player to) {
        to.setData(ModAttachments.APERTURE, from.getData(ModAttachments.APERTURE));
        to.setData(ModAttachments.APERTURE_STORAGE, from.getData(ModAttachments.APERTURE_STORAGE).copy());
        to.setData(ModAttachments.BODY, from.getData(ModAttachments.BODY));
        to.setData(ModAttachments.SOUL, from.getData(ModAttachments.SOUL));
        to.setData(ModAttachments.PATH, from.getData(ModAttachments.PATH));
        to.setData(ModAttachments.PATH_QI, from.getData(ModAttachments.PATH_QI));
        to.setData(ModAttachments.PATH_STRENGTH, from.getData(ModAttachments.PATH_STRENGTH));
        to.setData(ModAttachments.MIND, from.getData(ModAttachments.MIND));
        to.setData(ModAttachments.APERTURE_NOURISH, from.getData(ModAttachments.APERTURE_NOURISH));
        to.setData(ModAttachments.DIMENSION_RETURN, from.getData(ModAttachments.DIMENSION_RETURN));
        to.setData(ModAttachments.BORN, from.getData(ModAttachments.BORN));
    }

    public static void resetAll(@NotNull Player player) {
        player.setData(ModAttachments.APERTURE, ApertureData.DEFAULT);
        player.setData(ModAttachments.APERTURE_STORAGE, ApertureStorage.DEFAULT);
        player.setData(ModAttachments.SOUL, SoulData.DEFAULT);
        player.setData(ModAttachments.PATH, PathData.DEFAULT);
        player.setData(ModAttachments.PATH_QI, PathQiData.DEFAULT);
        player.setData(ModAttachments.PATH_STRENGTH, PathStrengthData.DEFAULT);
        player.setData(ModAttachments.ESSENCE_CARRY, new float[ApertureData.MAX_APERTURES]);
        player.setData(ModAttachments.APERTURE_NOURISH, ApertureNourishData.DEFAULT);
        player.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.DEFAULT);
        onBirth(player);

        player.setData(ModAttachments.BODY,
                BodyData.DEFAULT.withLastDayIndex(BodyService.get(player).lastDayIndex()));

        if (player instanceof ServerPlayer server) {
            BodyHealthService.refresh(server);
            BodyAttackService.refresh(server);
            EpicFightIntegration.refresh(server);
        }
    }
}
