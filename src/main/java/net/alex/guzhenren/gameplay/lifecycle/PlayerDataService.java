package net.alex.guzhenren.gameplay.lifecycle;

import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.attribute.MaxHealthService;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.path.qi.PathQiData;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The one cross-domain lifecycle service: birth, sleep, death, clone, respawn, and a full reset. It is
 * the single place that decides what a clone inherits; every domain service re-runs on join/clone/reset.
 * Birth, clone and reset write whole attachments here, past the domain services, which are the only
 * runtime writers.
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
        migratePhysique(player);
        ApertureService.syncTalentMarks(player);
        MaxHealthService.refresh(player);
        AttackDamageService.refresh(player);
        EpicFightIntegration.refresh(player);
    }

    private static void migratePhysique(@NotNull ServerPlayer player) {
        Aperture aperture = ApertureService.getAperture(player);
        ExtremePhysique legacy = aperture.legacyExtremePhysique();
        if (!BodyService.isExtreme(player) && legacy != null && legacy != ExtremePhysique.NONE) {
            BodyService.setExtremePhysique(player, legacy);
        } else if (!BodyService.isExtreme(player) && aperture.baseEssence() == Aperture.MAX_BASE) {
            ApertureService.set(player, ApertureData.PRIMARY, aperture.withBaseEssence(Aperture.MAX_BASE - 1)
                    .withPressure(0));
        }
        aperture = ApertureService.getAperture(player);
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
            MaxHealthService.refresh(server);
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

    private static void copy(@NotNull Player from, @NotNull Player player) {
        player.setData(ModAttachments.APERTURE, from.getData(ModAttachments.APERTURE));
        player.setData(ModAttachments.APERTURE_STORAGE, from.getData(ModAttachments.APERTURE_STORAGE).copy());
        player.setData(ModAttachments.BODY, from.getData(ModAttachments.BODY));
        player.setData(ModAttachments.SOUL, from.getData(ModAttachments.SOUL));
        player.setData(ModAttachments.PATH, from.getData(ModAttachments.PATH));
        player.setData(ModAttachments.PATH_QI, from.getData(ModAttachments.PATH_QI));
        player.setData(ModAttachments.PATH_STRENGTH, from.getData(ModAttachments.PATH_STRENGTH));
        player.setData(ModAttachments.MIND, from.getData(ModAttachments.MIND));
        player.setData(ModAttachments.APERTURE_NOURISH, from.getData(ModAttachments.APERTURE_NOURISH));
        player.setData(ModAttachments.DIMENSION_RETURN, from.getData(ModAttachments.DIMENSION_RETURN));
        player.setData(ModAttachments.BORN, from.getData(ModAttachments.BORN));
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
            MaxHealthService.refresh(server);
            AttackDamageService.refresh(server);
            EpicFightIntegration.refresh(server);
        }
    }
}
