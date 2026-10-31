package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.attribute.KnockbackResistanceService;
import net.alex.guzhenren.gameplay.attribute.MaxHealthService;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.mind.KillingMoveData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.mortal.StateEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The one cross-domain lifecycle service: birth, sleep, death, clone, respawn, and a full reset. A clone
 * inherits every value {@link PlayerData} keeps -- the loader copies them -- and this
 * is the single place that decides what a death takes back: a death that wipes resets them all
 * ({@link #resetAll}), a keepInventory death only the return point. Every domain service re-runs on
 * join/clone/reset. Birth and reset write whole values here, past the domain services, which are the
 * only runtime writers.
 *
 * <p>⚠ The {@code Player} (not {@code ServerPlayer}) signature on {@code onClone}/{@code onBirth}/{@code
 * resetAll} is the one carve-out from read-{@code Player}/write-{@code ServerPlayer}: during {@code
 * PlayerEvent.Clone} the fresh entity is typed {@code Player}; never widen a domain service. ⚠ {@code BORN}
 * stays inherited, or the next login re-rolls brilliance. ⚠ A new lethal state needs an {@code
 * onRespawn} line; its un-fire returns BARE values (soul 1, mind 0), on which the lethal check never fires. A
 * death dealt as plain damage leaves no lethal state to undo and needs none: {@code vital_gu_lost}
 * ({@link VitalLossService}) and the {@code ten_extreme_disaster} a detonation deals bystanders.
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
        if (!PlayerData.get(player).isBorn()) onBirth(player);
        BodyService.skipOfflineLifespan(player);
        ApertureService.syncTalentEffect(player);
        BodyService.syncRaceTalent(player);
        StateEffects.sync(player);
        MaxHealthService.refresh(player);
        KnockbackResistanceService.refresh(player);
        MaxHealthService.restoreCarried(player);
        AttackDamageService.refresh(player);
        ModEvents.post(new PlayerRefreshedEvent(player));
    }

    public static void onLogout(@NotNull ServerPlayer player) { MaxHealthService.carry(player); }

    public static void onBirth(@NotNull Player player) {
        PlayerData data = PlayerData.get(player);
        data.setMind(MindData.createNewborn());
        data.setBorn(true);
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
            PlayerData data = PlayerData.get(to);
            data.setDimensionReturn(DimensionReturnData.DEFAULT);
            data.setKillingMoves(data.getKillingMoves().withActive(KillingMoveData.NONE));
        }
        if (to instanceof ServerPlayer server) {
            MaxHealthService.refresh(server);
            KnockbackResistanceService.refresh(server);
            server.setHealth(wasDeath ? server.getMaxHealth() : from.getHealth());
            AttackDamageService.refresh(server);
            ModEvents.post(new PlayerRefreshedEvent(server));
        }
    }

    private static void dropHumanApertures(@NotNull Player from) {
        ApertureData data = PlayerData.get(from).getAperture();
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
        PlayerData data = PlayerData.get(player);
        data.resetCarriedOnDeath();
        data.resetEssenceCarry();
        data.setAwakeningRitual(null);
        data.setBody(BodyData.DEFAULT.withLastDayIndex(lastDayIndex));
        onBirth(player);

        if (player instanceof ServerPlayer server) {
            MaxHealthService.refresh(server);
            KnockbackResistanceService.refresh(server);
            AttackDamageService.refresh(server);
            ModEvents.post(new PlayerRefreshedEvent(server));
        }
    }
}
