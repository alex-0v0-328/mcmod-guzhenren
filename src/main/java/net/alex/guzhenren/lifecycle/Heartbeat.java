package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.MugwortShieldService;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.gameplay.body.UndeadService;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.GuUpkeep;
import net.alex.guzhenren.item.gu.mortal.strength.SelfRelianceGuItem;
import net.alex.guzhenren.item.gu.mortal.time.AsBeforeGuItem;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.alex.guzhenren.vault.ApertureStorageTick;
import net.minecraft.server.level.ServerPlayer;

/**
 * The one-second heartbeat: a straight run of ordered steps that most of the player's state depends
 * on. Every tick of a live server player calls {@link #beat}, which gates itself: every step runs inside
 * {@code tickCount % HEARTBEAT_TICKS == 0}, and most read what an earlier one just wrote. The beat reads the
 * clock once, into a {@link Beat}, and every time-scaled step runs on that one reading. {@link PlayerDataService}
 * owns the lifecycle; this class owns the cadence, and every step but
 * {@code checkLethalState} lives in its own domain. {@code checkLethalState} stays here because the
 * order of the four deaths is a rule across domains.
 *
 * <p>The beat, in order, each step naming the step it must follow and why:
 *
 * <ol>
 * <li>{@code BodyService.tickAging} and {@code TimeFlow.getRate}, read once into the {@link Beat}: every
 * day-clock walk spends its {@code days}, and lifespan is not billed here. A rate change during the beat
 * lands on the next one.</li>
 * <li>{@code GuUpkeep.tickCarried} -- a day-clock walk on (1).</li>
 * <li>{@code ApertureStorageTick.tickStored} -- a day-clock walk on (1).</li>
 * <li>{@code ApertureStorageMenu.refresh}, for an open store -- after (3), or the menu saves its stale view
 * back over the walk.</li>
 * <li>{@code VitalLossService.settleOfflineVitalLoss} -- right after (2) and (3), which charge an online
 * Vital Gu death on the spot; its soul and mind halving lands before (17) and (21).</li>
 * <li>{@code PathEffectService.tick} -- after (5), before every step that reads a body form or a pool: drops the
 * path effects [流派效果] whose time ran out and re-projects the icons of the rest, so this beat reads this second's
 * effects.</li>
 * <li>{@code ApertureEssenceService.closeDistilling} -- before (15): the 1:2 return lands before regen picks
 * a pool.</li>
 * <li>{@code UndeadService.tickHalfZombie} -- may turn a half-zombie into a zombie or end the form; (10) and
 * (12) to (17) read the form.</li>
 * <li>{@code SkinService.tickSkin}, then {@code MugwortShieldService.tickShield} -- shed a jade skin [玉皮] or a
 * Heavenly Mugwort shield [天蓬护盾] whose time ran out and project what is still worn as its effect, a body-form
 * projection like (8); nothing later reads either, both are read per hit instead.</li>
 * <li>{@code UndeadService.pinHunger} -- after (8), or a form that ended this beat still pins hunger.</li>
 * <li>{@code PathQiService.syncEffects} -- rebuilds the qi effects from the pool; (12), (13) and (15) read
 * them.</li>
 * <li>{@code UndeadService.tickDeathQi} -- after (11) for the effect, after (8) for the zombie skip.</li>
 * <li>{@code AttackDamageService.refresh} -- after (8) and (11): the form and the strength-qi effect are both
 * {@code getBonus} terms.</li>
 * <li>{@code BodyService.tickLifespan} -- after (8), since an undead body is never billed; before (21),
 * which reads exhaustion.</li>
 * <li>{@code ApertureEssenceService.regenStep} -- after (7), (8) and (11).</li>
 * <li>{@code ApertureNourishService.tickNourish} -- after (15): this second's regen before this second's
 * bill.</li>
 * <li>{@code MindService.regenStep} -- after (8): a zombie thinks every fifth second.</li>
 * <li>{@code SelfRelianceGuItem.tryAutoUse} -- after (15), whose essence the auto-use pays.</li>
 * <li>{@code AsBeforeGuItem.remember} -- after (8), (14) and (17): the body it records is this beat's settled one,
 * the half-zombie form, lifespan exhaustion and thoughts included; before (21), so a body about to die is still
 * remembered as it was.</li>
 * <li>{@code AperturePressureService.tick} -- the service gates its own minute step and countdown; a full
 * gauge goes to (21).</li>
 * <li>{@code checkLethalState} -- last.</li>
 * </ol>
 *
 * <p>⚠ The step ORDER is load-bearing and nothing in the code admits it: reorder nothing blind, and a
 * new step adds a line above naming which one it follows and why. {@code checkLethalState} runs last and
 * returns after the first hit, so the four deaths have a fixed precedence: 空窍压力 → 寿元 → 魂魄 → 脑海.
 *
 * <p>⚠ A step slower than a second gates on {@code tickCount % N} inside the beat, N a multiple of
 * {@code HEARTBEAT_TICKS}; any other N fires only every lcm(20, N) ticks, silently slower. Death Qi's year
 * ({@code UndeadService.DEATH_QI_YEAR_INTERVAL_TICKS}, 120) and a zombie's thought
 * ({@code MindService.ZOMBIE_THOUGHT_INTERVAL_TICKS}, 100) both follow it.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see PlayerTickEvents
 * @since 1.0.0
 */

public final class Heartbeat {

    private Heartbeat() {}

    public static final int HEARTBEAT_TICKS = Ticks.SECOND;

    public static void beat(ServerPlayer player) {
        if (player.isRemoved() || player.isDeadOrDying()) return;
        if (player.tickCount % HEARTBEAT_TICKS != 0) return;

        Beat beat = new Beat(BodyService.tickAging(player), TimeFlow.getRate(player), player.getRandom());
        GuUpkeep.tickCarried(player, beat.days());
        ApertureStorageTick.tickStored(player, beat.days());

        if (player.containerMenu instanceof ApertureStorageMenu menu) menu.refresh();
        VitalLossService.settleOfflineVitalLoss(player);

        PathEffectService.tick(player);
        ApertureEssenceService.closeDistilling(player);
        UndeadService.tickHalfZombie(player);
        SkinService.tickSkin(player);
        MugwortShieldService.tickShield(player);
        UndeadService.pinHunger(player);
        PathQiService.syncEffects(player);
        UndeadService.tickDeathQi(player);
        AttackDamageService.refresh(player);
        BodyService.tickLifespan(player, beat);
        ApertureEssenceService.regenStep(player, beat);
        ApertureNourishService.tickNourish(player, beat);
        MindService.regenStep(player, beat);
        SelfRelianceGuItem.tryAutoUse(player);
        AsBeforeGuItem.remember(player);
        AperturePressureService.tick(player);
        checkLethalState(player);
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
