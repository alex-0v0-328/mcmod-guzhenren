package net.alex.guzhenren.lifecycle;

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
import net.alex.guzhenren.gameplay.immortalaperture.BlissfulLands;
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
 * 唯一的跨领域玩家生命周期服务，统筹出生初始化、睡眠回复、死亡克隆、重生恢复与完全重置。
 * <ul>
 *   <li>NeoForge 克隆体默认继承 {@link PlayerData} 全部附件，因此由本类唯一裁定死亡代价：非 {@code keepInventory} 死亡
 *       时在尸体处掉落各空窍对应转数的人窍[Human Aperture]（{@link #dropHumanApertures}）并执行 {@link #resetAll} 全量
 *       重置，{@code keepInventory} 死亡仅清空维度返回点与当前激活杀招[Killing Move]。</li>
 *   <li>上线、克隆与重置时统一刷新 {@link ApertureService}、{@link BodyService} 等领域同步与派生属性；仅出生与全量重置
 *       允许在此直写整份状态记录，运行时增量更新一律归各自领域服务。</li>
 *   <li>本命蛊损毁由 {@link VitalLossService} 单独结算；{@code vital_gu_lost} 与爆炸波及他人的
 *       {@code ten_extreme_disaster} 属于一次性普通伤害致死，不留致死状态，重生时无需额外撤销。</li>
 *   <li>⚠ {@code onClone}、{@code onBirth} 与 {@code resetAll} 是全模组唯一接收 {@code Player} 而非
 *       {@code ServerPlayer} 写入数据的例外（因 {@code PlayerEvent.Clone} 新实体尚未完全转为服务端类型），领域服务不得
 *       放宽此参数约束。</li>
 *   <li>⚠ {@code BORN} 标记必须随死亡克隆继承，否则玩家重生或重登后会重新随机生成资质[Aptitude]；新增任何致死状态时必
 *       须在 {@link #onRespawn} 中补充重置分支，且重置后应落至不触发心跳致死判定的安全值（如魂魄为 1、脑海为 0）。</li>
 * </ul>
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
            BlissfulLands.onReset(server);
        }
    }

    private static void refreshBody(ServerPlayer player) {
        MaxHealthService.refresh(player);
        KnockbackResistanceService.refresh(player);
    }

    private static void refreshDamage(ServerPlayer player) {
        AttackDamageService.refresh(player);
    }
}
