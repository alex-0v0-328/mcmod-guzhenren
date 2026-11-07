package net.alex.guzhenren.compat;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.CrashDash;
import net.alex.guzhenren.entity.gu.WildGuEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.attribute.StrengthService;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.particle.RingConeEmitter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.AirSlashAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.ComboAttackAnimation;
import yesman.epicfight.api.animation.types.DashAttackAnimation;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.AttackPhaseEndEvent;
import yesman.epicfight.api.event.types.player.SetTargetEvent;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * The server-side Epic Fight bridge (the dash input sits in {@code DashInput}): aptitude cap, undead free skill
 * use, attack refresh, target exclusion for every {@link net.alex.guzhenren.entity.gu.WildGuEntity}, the dash
 * animation, the dash-side shockwave cone trigger (ring spawning lives in {@code RingConeEmitter}), and the area
 * impact trigger.
 *
 * <p>{@link #onAttackPhaseEnd}: when the hit window of a fist strike closes -- a combo, dash or air attack in Epic
 * Fight mode with a fist-category primary hand that is also a bare main hand ({@link AttackDamageService#isFistHand};
 * a glove [拳套] is FIST but no bare hand; a bare main hand beside an offhand weapon is Epic Fight's mirror mode, which
 * swings the offhand weapon, so no fist strike), never an innate
 * skill, which Epic Fight builds as a plain {@code AttackAnimation} -- the area impact rolls its chance once on the
 * player's random ({@link AreaImpact#rolls}); a landed roll lets {@link AreaImpact#release} hit the cone along the
 * look, sparing every target the fist already tried this phase. Every release, whiff or not, sends one area impact
 * ring down the cone ({@link RingConeEmitter#areaImpactRing}), a missed roll none, and each target it landed on gets
 * the strike's own hit sound, read the way Epic Fight reads it for the fist's hit; Epic Fight's hit particle stays
 * off the impact, the ring is its picture. The event fires once per phase on the server; a phase cut short by
 * another animation ({@code isAnimationTerminated}) rolls nothing.
 *
 * <p>⚠ Epic Fight owns stamina: the current value, regeneration, the HUD and all ordinary consumption. GZR only
 * scales the cap and waives the undead's skill cost; it keeps no stamina attachment and adds no sprint gate,
 * jump bill, hunger exhaustion surcharge or client mixin.
 *
 * <p>{@link #dashDirection} computes the dash motion direction as a unit vector: the payload's yRot is
 * the Epic Fight MODEL facing (a backward dodge still faces the enemy), so {@link #getRingYawOffset(int)}
 * recovers the actual motion yaw -- pinned by {@code DashRingYawTest}. The client already bakes the
 * strafe/diagonal angles (±45°/±90°) into that yaw, so the only correction left is the backward dodge
 * animation moving opposite to the model facing: every backward combination flips 180, everything else
 * rides the model yaw unchanged. Every Crash Gu dash moves in the yaw plane: the payload's
 * vertical/horizontal are the keyboard axes (W/S and A/D), never world-up.
 *
 * <p>{@code DashAnimation} scales Epic Fight's dodge coordinate vector by {@link CrashDash#getScale}, doubled
 * while {@link StrengthService#isAllOut}. It is the one piece here that runs on the client: Epic Fight moves a player
 * through an action animation only on the owning client ({@code PlayerPatch.shouldMoveOnCurrentSide} is false on the
 * server, true on the {@code LocalPlayerPatch}) and posts the position to the server, so the scale reads All-Out
 * there, from the player's own synced effect and strength data.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class EpicFightIntegration {

    private static final ResourceLocation STAMINA_MODIFIER = Guzhenren.id("epic_fight_stamina");
    private static final AttributeModifier.Operation STAMINA_OPERATION =
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashForward;
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashBackward;

    private EpicFightIntegration() {}

    public static void initialize() {
        EpicFightEventHooks.Player.CONSUME_SKILL.registerEvent(EpicFightIntegration::onSkillConsume, Guzhenren.MOD_ID);
        EpicFightEventHooks.Player.SET_TARGET.registerEvent(EpicFightIntegration::onSetTarget, Guzhenren.MOD_ID);
        EpicFightEventHooks.Animation.ATTACK_PHASE_END.registerEvent(EpicFightIntegration::onAttackPhaseEnd,
                Guzhenren.MOD_ID);
    }

    public static void onAnimationRegistry(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(Guzhenren.MOD_ID, builder -> {
            dashForward = builder.nextAccessor("biped/skill/dash_forward", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED)
                            .setResourceLocation("epicfight", "biped/skill/step_forward"));
            dashBackward = builder.nextAccessor("biped/skill/dash_backward", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED)
                            .setResourceLocation("epicfight", "biped/skill/step_backward"));
        });
    }

    public static void dash(ServerPlayer player, int vertical, float yRot) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        if (!EpicFightSkills.STEP.get().isExecutableState(patch)) return;

        AnimationManager.AnimationAccessor<DodgeAnimation> animation = vertical < 0
                ? dashBackward : dashForward;
        if (animation == null) return;
        patch.playAnimationSynchronized(animation, 0.0F);
        patch.setModelYRot(yRot, true);
        RingConeEmitter.dashCone(player, dashDirection(vertical, yRot));
    }

    public static void refresh(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(EpicFightAttributes.MAX_STAMINA);
        if (instance == null) return;

        double bonus = getStaminaMaxPercent(player) / 100.0D;
        if (TransientModifiers.hasModifier(instance, STAMINA_MODIFIER, bonus, STAMINA_OPERATION)) return;

        TransientModifiers.swap(instance, STAMINA_MODIFIER, bonus, STAMINA_OPERATION);
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) patch.clampMaxAttributes();
    }

    private static int getStaminaMaxPercent(ServerPlayer player) {
        ExtremePhysique physique = BodyService.getExtremePhysique(player);
        return physique == ExtremePhysique.NONE
                ? ApertureService.getTalent(player).getStaminaMaxPercent()
                : physique.getStaminaMaxPercent();
    }

    private static void onSkillConsume(SkillConsumeEvent event) {
        if (event.getResourceType() != Skill.Resource.STAMINA) return;
        if (event.getEntityPatch().getOriginal() instanceof Player player
                && BodyService.isUndead(player)) {
            event.setAmount(0.0F);
        }
    }

    private static void onSetTarget(SetTargetEvent event) {
        if (event.getTarget() instanceof WildGuEntity) event.cancel();
    }

    private static void onAttackPhaseEnd(AttackPhaseEndEvent event) {
        if (event.isAnimationTerminated()) return;
        if (!(event.getEntityPatch() instanceof ServerPlayerPatch patch) || !patch.isEpicFightMode()) return;
        if (!isFistStrike(event.getAnimation().get())) return;
        if (patch.getPrimaryItemCapability().getWeaponCategory() != CapabilityItem.WeaponCategories.FIST) return;

        ServerPlayer player = patch.getOriginal();
        if (!AttackDamageService.isFistHand(player)) return;
        if (!AreaImpact.rolls(player, player.getRandom())) return;

        Vec3 look = player.getLookAngle();
        RingConeEmitter.areaImpactRing(player, look, AreaImpact.RANGE);
        List<LivingEntity> landed = AreaImpact.release(player, look, patch.getCurrentlyAttackTriedEntities());
        AttackAnimation.Phase phase = event.getPhase();
        SoundEvent sound = phase.getProperty(AttackPhaseProperty.HIT_SOUND)
                .orElseGet(() -> patch.getWeaponHitSound(phase.effectiveHand(patch)));
        if (sound == null) return;

        ServerLevel level = player.serverLevel();
        for (LivingEntity target : landed) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), sound, target.getSoundSource(),
                    1.0F, 1.0F);
        }
    }

    private static boolean isFistStrike(AttackAnimation animation) {
        return animation instanceof ComboAttackAnimation || animation instanceof DashAttackAnimation
                || animation instanceof AirSlashAnimation;
    }

    private static Vec3 dashDirection(int vertical, float yRot) {
        float radians = (yRot + getRingYawOffset(vertical)) * ((float) Math.PI / 180.0F);
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
    }

    static float getRingYawOffset(int vertical) {
        return vertical < 0 ? 180.0F : 0.0F;
    }

    private static final class DashAnimation extends DodgeAnimation {

        private DashAnimation(float transitionTime, AnimationManager.AnimationAccessor<DodgeAnimation> accessor,
                              float width, float height, AssetAccessor<? extends Armature> armature) {
            super(transitionTime, accessor, width, height, armature);
        }

        @Override
        protected Vec3 getCoordVector(LivingEntityPatch<?> entityPatch,
                                      AssetAccessor<? extends DynamicAnimation> animation) {
            boolean allOut = entityPatch.getOriginal() instanceof Player player && StrengthService.isAllOut(player);
            return super.getCoordVector(entityPatch, animation).scale(CrashDash.getScale(allOut));
        }
    }
}
