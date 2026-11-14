package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.attribute.KnockbackResistanceService;
import net.alex.guzhenren.gameplay.attribute.MaxHealthService;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.mind.KillingMoveData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.mortal.behavior.StateEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 唯一的跨领域生命周期服务：出生、睡眠、死亡、克隆、重生、完全重置。
 * <ul>
 *   <li>克隆体继承 {@link PlayerData} 全部值（加载器复制），本类唯一决定死亡收回什么：清空型死亡全重置
 *       （{@link #resetAll}），keepInventory 死亡只重置返回点。</li>
 *   <li>加入、克隆、重置时重跑各领域服务；出生与重置在此直写整值、绕过领域服务，后者是唯一运行时写入方。</li>
 *   <li>{@link #dropHumanApertures}：清空型死亡于尸处按空窍[Aperture]各掉同转人窍[Human Aperture]。</li>
 *   <li>失去本命蛊[Vital Gu]由 {@link VitalLossService} 结算；它的 {@code vital_gu_lost} 与爆炸波及旁人的
 *       {@code ten_extreme_disaster} 是普通伤害致死，不留致死状态，无需撤销。</li>
 *   <li>⚠ {@code onClone}/{@code onBirth}/{@code resetAll} 用 {@code Player}，是读 {@code Player}、
 *       写 {@code ServerPlayer} 的唯一例外：{@code PlayerEvent.Clone} 中新实体是 {@code Player}；勿放宽领域服务。</li>
 *   <li>⚠ {@code BORN} 保持继承，否则下次登录重掷才情；新致死状态须在 {@code onRespawn} 加一行，撤销后取最低值（魂魄
 *       1，脑海 0），致死检查不触发。</li>
 * </ul>
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
        refreshBody(player);
        MaxHealthService.restoreCarried(player);
        refreshDamage(player);
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
            refreshBody(server);
            server.setHealth(wasDeath ? server.getMaxHealth() : from.getHealth());
            refreshDamage(server);
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
            refreshBody(server);
            refreshDamage(server);
        }
    }

    private static void refreshBody(ServerPlayer player) {
        MaxHealthService.refresh(player);
        KnockbackResistanceService.refresh(player);
    }

    /** 重算攻击伤害，再通知监听者派生属性已定。 */
    private static void refreshDamage(ServerPlayer player) {
        AttackDamageService.refresh(player);
        ModEvents.post(new PlayerRefreshedEvent(player));
    }
}
