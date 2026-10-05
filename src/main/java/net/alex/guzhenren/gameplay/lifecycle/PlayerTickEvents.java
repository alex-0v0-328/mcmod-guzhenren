package net.alex.guzhenren.gameplay.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.pool.DeathQiEffect;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageMenu;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageTick;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.SelfRelianceGuItem;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The one-second heartbeat: a straight run of ordered steps that most of the player's state depends
 * on. Every step runs inside {@code tickCount % Ticks.SECOND == 0}, and most read what an earlier one
 * just wrote — aging feeds the day-clock walks, {@code syncEffects} feeds {@code tickDeathQi} and
 * {@code regenStep}, {@code tickHalfZombie} feeds attack and regen. {@link
 * PlayerDataService} owns the lifecycle; this file owns the cadence.
 *
 * <p>⚠ The step ORDER is load-bearing and nothing in the code admits it: reorder nothing blind, and a
 * new step must declare which existing one it follows and why. {@code checkLethalState} runs last and
 * returns after the first hit, so the four deaths have a fixed precedence: 空窍压力 → 寿元 → 魂魄 → 脑海.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlayerTickEvents {

    private PlayerTickEvents() {}

    private static final int FULL_HUNGER = 20;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isRemoved() || player.isDeadOrDying()) return;

        if (player.tickCount % ApertureEssenceService.REGEN_INTERVAL_TICKS != 0) return;

        long days = BodyService.tickAging(player);
        TendedGuItem.tickCarried(player, days);
        ApertureStorageTick.tickStored(player, days);

        if (days > 0L && player.containerMenu instanceof ApertureStorageMenu menu) menu.reload();
        PlayerDataService.settleOfflineVitalLoss(player);

        closeDistilling(player);
        tickHalfZombie(player);
        pinUndeadHunger(player);
        PathQiService.syncEffects(player);
        tickDeathQi(player);
        AttackDamageService.refresh(player);
        BodyService.tickLifespan(player);
        ApertureEssenceService.regenStep(player);
        ApertureNourishService.tickNourish(player);
        MindService.regenStep(player);
        SelfRelianceGuItem.tryAutoUse(player);
        AperturePressureService.tick(player);
        checkLethalState(player);
    }

    private static void tickDeathQi(ServerPlayer player) {
        if (!player.hasEffect(ModEffects.DEATH_QI) || BodyService.isZombie(player)) return;

        if (player.tickCount % DeathQiEffect.YEAR_INTERVAL_TICKS == 0) {
            BodyService.drainByDeathQi(player, DeathQiEffect.YEARS_PER_INTERVAL);
        }
        if (player.getHealth() > DeathQiEffect.HEALTH_FLOOR) {
            player.setHealth(Math.max(DeathQiEffect.HEALTH_FLOOR,
                    player.getHealth() - DeathQiEffect.HEALTH_PER_HEARTBEAT));
        }
    }

    private static void pinUndeadHunger(ServerPlayer player) {
        if (!BodyService.isUndead(player)) return;

        FoodData food = player.getFoodData();
        food.setFoodLevel(FULL_HUNGER);
        food.setSaturation(FULL_HUNGER);
        food.setExhaustion(0.0F);
    }

    private static void tickHalfZombie(ServerPlayer player) {
        if (BodyService.isHalfZombie(player)) {
            if (PathQiService.getCurrent(player, QiKind.DEATH) > 0L) {
                BodyService.turnZombie(player, BodyService.get(player).zombieTier());
            } else if (BodyService.hasHalfZombieRunOut(player)) {
                BodyService.removePhysique(player, Physique.HALF_ZOMBIE);
            }
        }
        projectHalfZombie(player);
    }

    private static void projectHalfZombie(ServerPlayer player) {
        if (!BodyService.isHalfZombie(player)) {
            if (player.hasEffect(ModEffects.HALF_ZOMBIE)) player.removeEffect(ModEffects.HALF_ZOMBIE);
            return;
        }
        player.addEffect(ModEffects.instance(ModEffects.HALF_ZOMBIE,
                Math.max(1, (int) BodyService.getHalfZombieTicksLeft(player))));
    }

    private static void closeDistilling(ServerPlayer player) {
        if (ApertureEssenceService.totalDistilled(player) > 0L && !ApertureEssenceService.isDistilling(player)) {
            ApertureEssenceService.endDistilling(player);
        }
    }

    private static void checkLethalState(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;

        if (AperturePressureService.isFull(player)) {
            if (!ApertureNourishService.convertPetrifiedPressure(player)) AperturePressureService.detonate(player);
            return;
        }
        if (BodyService.get(player).isExhausted()) {
            player.hurt(ModDamageTypes.source(player, ModDamageTypes.LIFESPAN_EXHAUSTED), Float.MAX_VALUE);
            return;
        }
        if (SoulService.get(player).isCollapsed()) {
            player.hurt(ModDamageTypes.source(player, ModDamageTypes.SOUL_COLLAPSE), Float.MAX_VALUE);
            return;
        }
        if (MindService.get(player).isOverflowing()) {
            player.hurt(ModDamageTypes.source(player, ModDamageTypes.MIND_OCEAN_SHATTERED), Float.MAX_VALUE);
        }
    }
}
