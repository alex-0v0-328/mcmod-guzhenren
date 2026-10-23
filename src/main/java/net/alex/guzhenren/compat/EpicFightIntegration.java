package net.alex.guzhenren.compat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.CrashGuEffect;
import net.alex.guzhenren.entity.WildGuEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import yesman.epicfight.world.entity.eventlistener.BasicAttackEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.entity.eventlistener.SkillConsumeEvent;

/**
 * The one required Epic Fight bridge: aptitude cap, undead free skill use, attack refresh, target
 * exclusion for every {@link net.alex.guzhenren.entity.WildGuEntity}, the dash animation, and
 * the dash-side shockwave cone trigger (ring spawning lives in {@code RingConeEmitter}; the
 * heavy-fist hook is in {@code EpicFightServerEvents}).
 *
 * <p>The old GZR stamina attachment, sprint gate, jump bill, hunger exhaustion surcharge and client mixin are
 * deliberately absent. Epic Fight owns current stamina, regeneration, HUD and all ordinary consumption.
 *
 * <p>⚠ A version seam: Epic Fight 21.x takes global event hooks, 20.14 takes listeners on each player's own
 * patch. {@link #initialize()} therefore listens for a player entering a level and (re)attaches the three
 * listeners under one id, which replaces instead of stacking.
 *
 * <p>{@link #dashDirection} computes the dash motion direction as a unit vector: the payload's yRot is
 * the Epic Fight MODEL facing (a backward dodge still faces the enemy), so {@link #ringYawOffset(int)}
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
    private static final AttributeModifier.Operation STAMINA_OPERATION = AttributeModifier.Operation.MULTIPLY_BASE;
    private static final UUID LISTENER_ID = idOf(Guzhenren.id("epic_fight_listener"));
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashForward;
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashBackward;

    private EpicFightIntegration() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(EpicFightIntegration::onPlayerJoin);
    }

    private static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;

        PlayerEventListener listener = patch.getEventListener();
        listener.addEventListener(PlayerEventListener.EventType.SKILL_CONSUME_EVENT, LISTENER_ID,
                EpicFightIntegration::onSkillConsume);
        listener.addEventListener(PlayerEventListener.EventType.BASIC_ATTACK_EVENT, LISTENER_ID,
                EpicFightIntegration::onComboAttack);
    }

    private static UUID idOf(ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
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
        if (!EpicFightSkills.STEP.isExecutableState(patch)) return;

        AnimationManager.AnimationAccessor<DodgeAnimation> animation = vertical < 0
                ? dashBackward : dashForward;
        if (animation == null) return;
        patch.playAnimationSynchronized(animation, 0.0F);
        patch.setModelYRot(yRot, true);
        RingConeEmitter.dashCone(player, dashDirection(vertical, yRot));
    }

    public static void refresh(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(EpicFightAttributes.MAX_STAMINA.get());
        if (instance == null) return;

        double bonus = staminaMaxPercent(player) / 100.0D;
        if (TransientModifiers.holds(instance, STAMINA_MODIFIER, bonus, STAMINA_OPERATION)) return;

        TransientModifiers.swap(instance, STAMINA_MODIFIER, bonus, STAMINA_OPERATION);
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) patch.clampMaxAttributes();
    }

    private static int staminaMaxPercent(ServerPlayer player) {
        ExtremePhysique physique = BodyService.getExtremePhysique(player);
        return physique == ExtremePhysique.NONE
                ? ApertureService.getTalent(player).getStaminaMaxPercent()
                : physique.getStaminaMaxPercent();
    }

    private static void onSkillConsume(SkillConsumeEvent event) {
        if (event.getResourceType() != Skill.Resource.STAMINA) return;
        Player player = event.getPlayerPatch().getOriginal();
        if (BodyService.isUndead(player)) event.setAmount(0.0F);
    }

    private static void onComboAttack(BasicAttackEvent event) {
        ServerPlayer player = event.getPlayerPatch().getOriginal();
        if (ModEffects.has(player, ModEffects.HARDSHIP_STRENGTH_GU)) AttackDamageService.refresh(player);
    }

    /**
     * Whether Epic Fight may not lock a player's attack target onto {@code target}: a wild Gu is never one.
     *
     * <p>⚠ Epic Fight 20.14 builds its set-target event as non-cancelable and assigns the target regardless, so
     * {@code mixin.ServerPlayerPatchMixin} asks this before the assignment instead of a listener canceling it.
     */
    public static boolean refusesTarget(@Nullable LivingEntity target) { return target instanceof WildGuEntity; }

    private static Vec3 dashDirection(int vertical, float yRot) {
        float radians = (yRot + ringYawOffset(vertical)) * ((float) Math.PI / 180.0F);
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
    }

    static float ringYawOffset(int vertical) {
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
