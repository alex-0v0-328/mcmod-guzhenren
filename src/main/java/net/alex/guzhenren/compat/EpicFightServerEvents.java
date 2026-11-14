package net.alex.guzhenren.compat;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.projectile.ProjectileThrownEvent;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.body.ShieldRaisedEvent;
import net.alex.guzhenren.gameplay.flight.SecondStageCrashEvent;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent;
import net.alex.guzhenren.gameplay.plunge.PlungeLandedEvent;
import net.alex.guzhenren.item.gu.GuUsedEvent;
import net.alex.guzhenren.lifecycle.PlayerRefreshedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPCreateTerrainFracture;
import yesman.epicfight.registry.entries.EpicFightItems;

/**
 * Epic Fight 兼容的服务端事件订阅。
 * <ul>
 *   <li>每个世界写入一次技能保留规则（{@link EpicFightRuleSeed}）；空窍[Aperture]变化（{@code ApertureChangedEvent}）或
 *       生命周期重建派生属性（{@code PlayerRefreshedEvent}）时重读耐力修饰符。</li>
 *   <li>{@link #onCharge}：服务端自行丢弃蓄力蛊[Store Strength Gu]蓄力时放下姿势；{@link #onPlayerTick}：每 tick 交
 *       {@link EpicFightIntegration#tickMeditation}，随温养[Nourish]、开窍[Awakening]仪式起止坐下起身。</li>
 *   <li>{@link #onSecondStageCrash}：服务端撞击为所有可见客户端播反弹；二段飞行[Second-Stage Flight]者本人客户端也发此
 *       事件（带 {@code LocalPlayer}），留给服务端那次。</li>
 *   <li>护盾、投掷、催动动画只在此接一次：{@link #onShieldRaised} 双臂交叉（{@code ShieldService#raise} 对每种护盾、每
 *       次补充都发）；{@link #onProjectileThrown} 挥臂（每片月刃一个事件）；{@link #onGuUsed} 右手前伸
 *       （{@code MortalGuItem} 在凡蛊催动共同收尾发），自带动画的由 {@link EpicFightIntegration#useGu} 排除。</li>
 *   <li>{@link #onPlungeLanded}：落点放 Epic Fight 地裂（陨石砸地裂纹圈），半径取冲击力
 *       （{@code PlungeImpact#getImpactPower}，2 到 6 格），发给追踪该区块的客户端，再播落地动画
 *       （{@link EpicFightIntegration#landPlunge}）；事件只为够重的落地发出。</li>
 *   <li>只发包：{@code LevelUtil#circleSlamFracture} 还会造成冲击波伤害，而坠击不伤任何东西；地裂只在客户端绘制、不改方
 *       块，须先于弹坑发出，因其画在弹坑会挖掉的落点下方地面；爆炸即落地音效，地裂音效关、碎屑粒子留。</li>
 *   <li>{@link #onLivingDrops}：从生物掉落删技能书；{@code guzhenren:no_skill_books} 只清箱子战利品，Epic Fight 经自己
 *       的全局修改器加生物掉落，NeoForge 按哈希序而非数据包序应用，我们的可能先跑；玩家死亡掉落是背包，保留。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class EpicFightServerEvents {

    private EpicFightServerEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) { EpicFightRuleSeed.seed(event.getServer()); }

    @SubscribeEvent
    public static void onApertureChanged(ApertureChangedEvent event) {
        EpicFightIntegration.refresh(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerRefreshed(PlayerRefreshedEvent event) {
        EpicFightIntegration.refresh(event.getPlayer());
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) return;
        event.getDrops().removeIf(drop -> drop.getItem().is(EpicFightItems.SKILLBOOK));
    }

    @SubscribeEvent
    public static void onCharge(StoreStrengthChargeEvent event) {
        if (event.getChange() == StoreStrengthChargeEvent.Change.DROPPED) {
            EpicFightIntegration.cancelCharge(event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onSecondStageCrash(SecondStageCrashEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) EpicFightIntegration.crashFlight(player);
    }

    @SubscribeEvent
    public static void onShieldRaised(ShieldRaisedEvent event) {
        EpicFightIntegration.raiseShield(event.getPlayer());
    }

    @SubscribeEvent
    public static void onProjectileThrown(ProjectileThrownEvent event) {
        EpicFightIntegration.throwProjectile(event.getPlayer());
    }

    @SubscribeEvent
    public static void onGuUsed(GuUsedEvent event) { EpicFightIntegration.useGu(event.getPlayer()); }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) EpicFightIntegration.tickMeditation(player);
    }

    @SubscribeEvent
    public static void onPlungeLanded(PlungeLandedEvent event) {
        ServerPlayer player = event.getPlayer();
        EpicFightNetworkManager.sendToAllPlayerTrackingThisChunkWithSelf(
                new SPCreateTerrainFracture(event.getPosition(), event.getImpactPower(), true, false),
                player.serverLevel(), new ChunkPos(BlockPos.containing(event.getPosition())));
        EpicFightIntegration.landPlunge(player);
    }
}
