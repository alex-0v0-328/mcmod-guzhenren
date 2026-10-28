package net.alex.guzhenren.compat;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.CrashGuEffect;
import net.alex.guzhenren.entity.gu.WildGuEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.particle.RingConeEmitter;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.player.ComboAttackEvent;
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

/**
 * The server-side Epic Fight bridge (the heavy-fist hook sits in {@code EpicFightServerEvents}, the dash
 * input in {@code DashInput}): aptitude cap, undead free skill use, attack refresh, target
 * exclusion for every {@link net.alex.guzhenren.entity.gu.WildGuEntity}, the dash animation, and
 * the dash-side shockwave cone trigger (ring spawning lives in {@code RingConeEmitter}).
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
        EpicFightEventHooks.Player.COMBO_ATTACK.registerEvent(EpicFightIntegration::onComboAttack, Guzhenren.MOD_ID);
        EpicFightEventHooks.Player.SET_TARGET.registerEvent(EpicFightIntegration::onSetTarget, Guzhenren.MOD_ID);
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

    private static void onComboAttack(ComboAttackEvent event) {
        ServerPlayer player = event.getPlayerPatch().getOriginal();
        if (player.hasEffect(ModEffects.HARDSHIP_STRENGTH_GU)) AttackDamageService.refresh(player);
    }

    private static void onSetTarget(SetTargetEvent event) {
        if (event.getTarget() instanceof WildGuEntity) event.cancel();
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
            return super.getCoordVector(entityPatch, animation).scale(CrashGuEffect.DASH_COORD_SCALE);
        }
    }
}
