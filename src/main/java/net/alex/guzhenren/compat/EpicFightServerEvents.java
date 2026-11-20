package net.alex.guzhenren.compat;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.projectile.ProjectileThrownEvent;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.body.ShieldRaisedEvent;
import net.alex.guzhenren.gameplay.flight.SecondStageCrashEvent;
import net.alex.guzhenren.gameplay.path.strength.BeastPhantomEvents;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StoreStrengthChargeEvent;
import net.alex.guzhenren.gameplay.plunge.PlungeLandedEvent;
import net.alex.guzhenren.item.gu.GuUsedEvent;
import net.alex.guzhenren.lifecycle.PlayerRefreshedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPCreateTerrainFracture;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/**
 * Epic Fight 联动的服务端事件监听入口。
 * <ul>
 *   <li>服务端启动时调用 {@link EpicFightRuleSeed#seed} 初始化技能保留规则；空窍变更（{@link ApertureChangedEvent}）或
 *       玩家派生属性重建（{@link PlayerRefreshedEvent}）时调用 {@link EpicFightIntegration#refresh} 刷新耐力上限修饰
 *       符。</li>
 *   <li>{@link #onCharge} 在服务端主动丢弃蓄力蛊[Store Strength Gu]原地蓄力（非可移动蓄力）时播放收臂取消动画；
 *       {@link #onPlayerTick} 每刻驱动 {@link EpicFightIntegration#tickMeditation}，随温养[Nourish]、开窍[Awakening]仪
 *       式与真元灌注的起止广播入座和起身动画。</li>
 *   <li>{@link #onSecondStageCrash} 在服务端处理二段飞行[Second-Stage Flight]撞墙事件时向所有可见客户端广播反弹动画（忽
 *       略客户端侧触发的同名事件）；{@link #onShieldRaised}、{@link #onProjectileThrown} 与 {@link #onGuUsed} 分别触发
 *       护盾架起、投射物挥臂与凡蛊通用伸手催动动画。</li>
 *   <li>{@link #onPlungeLanded} 在重度坠击落地时向追踪该区块的客户端发送 {@link SPCreateTerrainFracture} 渲染环形地裂纹
 *       （仅发包渲染并保留碎屑粒子、关闭地裂音效且不调用会造成额外冲击波伤害的 {@code circleSlamFracture}，必须在弹坑挖
 *       掘前发送以免落点地面方块被挖空），随后播放落地蹲姿动画（{@link EpicFightIntegration#landPlunge}）。</li>
 *   <li>{@link #onStrikeDamage} 在最高优先级把 Epic Fight 出招伤害标为出拳（{@link ComboRound#markPunch}），供
 *       {@link BeastPhantomEvents} 的暴击与过载门槛识别：玩家出招的伤害来源由 {@code PlayerPatch#getDamageSource} 经
 *       {@code EpicFightDamageSources#playerAttack} 构建，类型是原版 {@code minecraft:player_attack}，与二段飞行冲撞同
 *       类型，只能靠 {@link EpicFightDamageSource} 上挂着的出招动画区分。</li>
 *   <li>{@link #onLivingDrops} 过滤非玩家生物死亡掉落中的 Epic Fight 技能书（箱子战利品已由全局战利品修改器
 *       {@code guzhenren:no_skill_books} 过滤，但生物掉落由 Epic Fight 自身的全局修改器按哈希顺序注入，需在掉落事件中兜
 *       底清除）。</li>
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onStrikeDamage(LivingIncomingDamageEvent event) {
        if (event.getSource() instanceof EpicFightDamageSource source && source.getAnimation() != null
                && source.getDirectEntity() instanceof ServerPlayer player) {
            ComboRound.markPunch(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) return;
        event.getDrops().removeIf(drop -> drop.getItem().is(EpicFightItems.SKILLBOOK));
    }

    @SubscribeEvent
    public static void onCharge(StoreStrengthChargeEvent event) {
        if (event.getChange() == StoreStrengthChargeEvent.Change.DROPPED
                && !StoreStrength.isMobile(event.getPlayer())) {
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
