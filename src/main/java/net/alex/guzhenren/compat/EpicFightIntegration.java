package net.alex.guzhenren.compat;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.CrashDash;
import net.alex.guzhenren.entity.gu.WildGuEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.attribute.TransientModifiers;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.CrashBreach;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
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
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AirSlashAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.ComboAttackAnimation;
import yesman.epicfight.api.animation.types.DashAttackAnimation;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.AttackPhaseEndEvent;
import yesman.epicfight.api.event.types.player.SetTargetEvent;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.gameasset.Animations;
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
 * <p>The dash plays one of this mod's own four clips, authored on the Epic Fight player rig and shipped under
 * {@code assets/guzhenren/animmodels/animations/biped/skill/}: {@code dash_forward} and {@code dash_right} ram with
 * the right shoulder, {@code dash_left} with the left, {@code dash_backward} with the back. {@link #getDashClip}
 * picks by the keyboard axes: any backward combination the back ram, any forward combination the forward ram, a
 * pure strafe its side (the payload's horizontal is 1 for left) -- pinned by {@code DashRingYawTest}. The clips
 * turn the body through the Root joint's rotation only; their Root translation is the travel, always along the
 * model's own forward axis (backward: its reverse), which Epic Fight rotates by the model yaw. So
 * {@code setModelYRot} stays: it is what aims the travel, the diagonals included.
 *
 * <p>{@code DashAnimation} scales Epic Fight's dodge coordinate vector by {@link #getDashScale}:
 * {@link CrashDash#getScale} from {@link StrengthService#isAllOut} and the held beasts' dash bonus (the galloping horse
 * [驰马骏力]). It is the one piece here that runs on the client: Epic Fight moves a player through an action animation
 * only on the owning client ({@code PlayerPatch.shouldMoveOnCurrentSide} is false on the server, true on the
 * {@code LocalPlayerPatch}) and posts the position to the server, so the scale reads All-Out and the beasts there,
 * from the player's own synced effect and strength data. The server reads the same scale for the two boars'
 * strength [两猪之力]: a body holding both clears the dash's path through {@link CrashBreach#clear} before the
 * animation is sent.
 *
 * <p>The Store Strength Gu's [蓄力蛊] charge [蓄力] plays three more of this mod's own clips from the same folder, a
 * bow draw. {@link #holdCharge} plays {@code charge_hold} as an action animation: the body turns side-on on
 * lightly bent knees, the right fist drawn back past the shoulder and the left arm pointing ahead; it locks moving
 * and turning, allows no combo attack or skill, and {@code Animations.ReusableSources.CHARGING} slows it into its
 * last frame, the fully charged pose, where it stays. {@link #strikeCharge} throws {@code charge_strike}, the
 * waist twisting into a heavy downward punch: an action animation too and so no attack, it has no collider, deals
 * nothing and closes no attack phase; its Root travel is the short step the strike has always taken.
 * {@link #cancelCharge} plays {@code charge_cancel} for every charge that ends without a strike -- the
 * client's cancel, a release the server refuses, a drop by the charge's own tick: a plain static animation, so it
 * locks nothing, lowers the arms in 0.2 s and hands the body back to the living motion, which may cut it short.
 *
 * <p>Second-stage flight [二段飞行] has two more of this mod's own clips. {@code biped/living/second_stage_flight_loop} is
 * the flight loop, 0.8 s: the body lies flat along the model's forward axis, head raised, both arms swept back along
 * the body, with a light flutter; a repeating static animation with a fixed head, since the client tilts the whole
 * model by the pitch ({@link EpicFightClientIntegration}, which maps it to {@link
 * EpicFightLivingMotions#SECOND_STAGE_FLIGHT_LOOP} through {@link #getSecondStageFlightLoop}). {@code
 * biped/skill/second_stage_flight_crash} is the crash, 0.4 s: the body crumples against the surface, is thrown back
 * and leans backward with the limbs flung ahead, then settles upright. {@link #crashFlight} plays it from the server's
 * crash ({@code EpicFightServerEvents}), synchronized to every client that sees the flier, the flier included: the
 * server's crash is the one that ends the stage and sends the ring and the crater, and the crash event on a client
 * reaches the flier's own client alone, so a client listener would show the bounce to nobody else. It is a main-frame
 * animation that only holds the living motion still for its length -- the stage ends in the same tick, and the motion
 * change that follows would otherwise replace the clip at once -- and locks no movement, turn, attack or skill; its
 * Root slides back 0.34 and returns, drawn as a pose and moving nothing.
 *
 * <p>The plunge [坠击] has the last two. {@code biped/living/plunge_fall} is the fall loop, 0.4 s, a stomp dive: the
 * body upright, both arms braced up against the airflow, the right leg stretched straight down and the left knee
 * drawn up; a repeating static animation the client maps to {@link EpicFightLivingMotions#PLUNGE_FALL} through
 * {@link #getPlungeFall}, so it runs for as long as the plunge does and ends with it, landing or not.
 * {@code biped/skill/plunge_land} is the landing, 0.8 s: the straight leg touches first, the body drops into a
 * three-point crouch in 0.1 s (left knee down, right fist on the ground, left arm thrown back), holds it and stands
 * up over the last 0.27 s. {@link #landPlunge} plays it from the server's landing ({@code EpicFightServerEvents}),
 * synchronized to every client that sees the player, and only for a landing hard enough to show, 3 blocks of fall or
 * more: a shorter plunge just ends the loop. It is a main-frame animation whose first
 * {@link #PLUNGE_LAND_LOCK} seconds, the crouch, lock movement, combo attacks and skills and hold the living motion
 * still; turning stays free. After that the living motion runs again, so walking off cuts the stand-up short and
 * standing still plays it out. Its Root keeps to the entity's position: the clip moves nothing.
 *
 * <p>Every shield [护盾] shares one clip, {@code biped/skill/shield_raise}, 0.6 s: the elbows flare as the chest rocks
 * back, both forearms snap into a cross in front of the chest in 0.08 s, the right one in front, while the chest
 * hunches forward behind them and the chin tucks; the guard sinks, settles and holds until 0.4 s, then the arms
 * swing down a little wide as the chest straightens. {@link #raiseShield}
 * plays it from {@code ShieldRaisedEvent} ({@code EpicFightServerEvents}), so every shield Gu, present or future, gets
 * it, and every call to {@code ShieldService#raise} replays it from the start, a refill of a shield still up
 * included. It is a plain static animation on a composite layer: the clip's client data file
 * ({@code biped/skill/data/shield_raise.json}, the way Epic Fight's own item-use clips declare it) puts it on the
 * {@code HIGH} composite layer under this mod's own joint mask
 * ({@code assets/guzhenren/animmodels/joint_mask/biped/chest_arms.json}: Epic Fight's {@code arms} plus the chest and
 * the head), so it drives the chest, the head, the shoulders, arms and hands alone, the legs, the Root and the torso
 * keep the base layer's living motion, a run's lean included, and it locks nothing; an animation of
 * a higher priority on the base layer (an attack, the dash, the landing crouch) hides it. It goes out through
 * {@code playAnimationInClientSide}, to every client that sees the player, the player's own included, and never
 * onto the server's animator: that one has a single track, and a clip played there would end the action it runs.
 *
 * <p>A throw [投掷] has three clips, 0.3 s each, first used by the Moon Blade [月刃] and meant for every later
 * throw-type skill: {@code biped/skill/throw_sweep}, the single shot, the hanging right arm dipping back and sweeping
 * from below upward, released at 0.12 s; {@code biped/skill/throw_punch_right} and its mirror
 * {@code throw_punch_left}, one shot of a run, the fist thrown straight ahead from the ribs by 0.08 s while the other
 * elbow is drawn back. {@link #throwProjectile} plays one for every {@code ProjectileThrownEvent}
 * ({@code EpicFightServerEvents}), so one for every blade, and {@link #getThrowClip} picks it from the gap since the
 * player's last throw: more than {@link #THROW_RUN_TICKS} ticks, or no throw yet, is the sweep; a throw inside the
 * window is a punch with the arm the last throw did not use, so a run goes sweep, left, right, left and a pause
 * starts over with the sweep. The gap is counted in game ticks and the last throw is kept per player in
 * {@code throwRuns}, a weak map only the server thread touches; it is never saved. The clips sit on the same
 * composite layer under the same {@code chest_arms} mask as the shield's and go out the same way
 * ({@code playAnimationInClientSide}), so they lock nothing, leave the legs to the living motion and reach every
 * client that sees the player. A blade may follow the last after 0.1 s, sooner than a clip ends: the new clip then
 * replaces the running one, blending in over 0.05 s from wherever the arms are, and only the last clip of a run
 * plays out to the arms' rest.
 *
 * <p>Meditation [打坐] has three clips, a cross-legged seat shared by nourishing an aperture [温养空窍], the awakening
 * ritual [开窍仪式] and pouring essence into a channeled Gu [灌注]: the thighs open on the ground, the shins fold across the
 * front, the back upright, the head a little bowed and both forearms over the lap so the hands stack before the lower
 * abdomen. {@code biped/skill/meditate_sit} sits down in 0.8 s, {@code biped/living/meditate_loop} is the seated loop,
 * one slow breath of 4 s, and {@code biped/skill/meditate_stand} stands up in 0.67 s. {@link #isMeditating} is the one
 * test both ends read: a nourishing run is going, the ritual still holds the player still ({@code
 * AwakeningRitualService#holdsStill}, its first 5 s), or the player holds the use of a refined channeled Gu ({@code
 * TendedGuItem#isChanneling}, read off the use item every client knows), so a channel sits for as long as the click is
 * held and stands up when it is let go or runs dry, and a ritual stands up when it lets the player move, not when its
 * plaque fades. The client maps the loop to {@link EpicFightLivingMotions#MEDITATE_LOOP} through {@link
 * #getMeditateLoop} for as long as the test holds, on every client that sees the player, a client that starts seeing
 * the player mid-seat included. {@link #tickMeditation} runs every server player tick ({@code EpicFightServerEvents}),
 * keeps the last answer per player in {@code meditating}, a weak map only the server thread touches and never saved,
 * and plays the sit-down when the test turns true and the stand-up when it turns false ({@link #getMeditationCue}),
 * synchronized to every client that sees the player: it watches the test instead of the services, so every way a run
 * or a ritual ends, the starved stop, a stage-up or a command included, stands the player up, and neither service
 * knows of it. A player first seen already seated (a login into a saved run) plays no sit-down and a dying one no
 * stand-up. Both one-shots are main-frame animations that lock no movement, turn, attack or skill and only hold the
 * living motion still, the sit-down for its whole length, after which the loop takes over, the stand-up for its first
 * {@link #MEDITATE_STAND_HOLD} seconds, after which walking off cuts it short. The head is fixed in all three; the
 * Root keeps to the entity's position, only its height drops, so the clips move nothing and the hitbox and the eye
 * height stay the standing ones.
 *
 * <p>A Gu use without an action of its own reaches forward: {@code biped/skill/use_reach}, 0.5 s, the right elbow
 * dips back, the chest turns a little left with the right shoulder leading and the right hand reaches straight
 * ahead no higher than the lower chest by 0.18 s, holds to 0.3 s and comes back down. {@link #useGu} plays it for every
 * {@code GuUsedEvent} ({@code EpicFightServerEvents}), the one event every landed use of a mortal Gu posts, so a new
 * Gu gets it with no code of its own; a refused use posts nothing and plays nothing. {@link #playsReach} leaves out
 * three kinds of use: one that already played a clip of its own in the same tick -- {@link #raiseShield} and
 * {@link #throwProjectile} stamp the game tick per player in {@code ownClips}, a weak map only the server thread
 * touches, and both run inside the Gu's effect, before the event -- so a shield Gu and a moon blade Gu keep theirs
 * alone, and a later Gu with its own clip only has to stamp too; one by a seated player ({@link #isMeditating}: the
 * Hope Gu [希望蛊], whose click starts the ritual that sits down); and an auto use ({@code AutoUse#isStarting}), which
 * the player never gestured for. A killing move's cast and an Amplify Effect Gu's drive are the player's own act
 * and do reach. The clip sits on the same {@code HIGH} composite layer under the same {@code chest_arms} mask as
 * the shield's and goes out the same way, so it locks nothing and leaves the legs to the living motion.
 *
 * <p>Refining a wild Gu [炼化] has a hold, {@code biped/living/refine_loop}, 2 s: the right hand raised before the chest
 * so the Gu it holds sits in view, the head bowed toward it, the left arm hanging, one slow breath a loop. It is a
 * repeating static animation the client maps to {@link EpicFightLivingMotions#REFINE_LOOP} as a composite living
 * motion through {@link #getRefineLoop} ({@link EpicFightClientIntegration}); its data file ({@code
 * biped/living/data/refine_loop.json}) puts it on the {@code MIDDLE} composite layer under the {@code chest_arms}
 * mask, so walking goes on under it. The server sends nothing for it.
 *
 * <p>The mutable statics, the four dash accessors, {@code chargeHold}, {@code chargeStrike}, {@code chargeCancel},
 * {@code secondStageFlightLoop}, {@code secondStageFlightCrash}, {@code plungeFall}, {@code plungeLand}, {@code
 * shieldRaise}, {@code throwSweep}, {@code throwPunchRight}, {@code throwPunchLeft}, {@code meditateSit}, {@code
 * meditateLoop}, {@code meditateStand}, {@code useReach} and {@code refineLoop}, are set once by Epic Fight's
 * animation registration and only read afterwards.
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
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashLeft;
    private static AnimationManager.AnimationAccessor<DodgeAnimation> dashRight;
    private static AnimationManager.AnimationAccessor<ActionAnimation> chargeHold;
    private static AnimationManager.AnimationAccessor<ActionAnimation> chargeStrike;
    private static AnimationManager.AnimationAccessor<StaticAnimation> chargeCancel;
    private static AnimationManager.AnimationAccessor<StaticAnimation> secondStageFlightLoop;
    private static AnimationManager.AnimationAccessor<MainFrameAnimation> secondStageFlightCrash;
    private static AnimationManager.AnimationAccessor<StaticAnimation> plungeFall;
    private static AnimationManager.AnimationAccessor<MainFrameAnimation> plungeLand;
    private static AnimationManager.AnimationAccessor<StaticAnimation> shieldRaise;
    private static AnimationManager.AnimationAccessor<StaticAnimation> throwSweep;
    private static AnimationManager.AnimationAccessor<StaticAnimation> throwPunchRight;
    private static AnimationManager.AnimationAccessor<StaticAnimation> throwPunchLeft;
    private static AnimationManager.AnimationAccessor<MainFrameAnimation> meditateSit;
    private static AnimationManager.AnimationAccessor<StaticAnimation> meditateLoop;
    private static AnimationManager.AnimationAccessor<MainFrameAnimation> meditateStand;
    private static AnimationManager.AnimationAccessor<StaticAnimation> useReach;
    private static AnimationManager.AnimationAccessor<StaticAnimation> refineLoop;
    static final long THROW_RUN_TICKS = 10L;
    private static final Map<ServerPlayer, ThrowRun> throwRuns = new WeakHashMap<>();
    private static final float PLUNGE_LAND_LOCK = 0.53F;
    private static final float MEDITATE_STAND_HOLD = 0.4F;
    private static final Map<ServerPlayer, Boolean> meditating = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> ownClips = new WeakHashMap<>();

    private EpicFightIntegration() {}

    public static void initialize() {
        LivingMotion.ENUM_MANAGER.registerEnumCls(Guzhenren.MOD_ID, EpicFightLivingMotions.class);
        EpicFightEventHooks.Player.CONSUME_SKILL.registerEvent(EpicFightIntegration::onSkillConsume, Guzhenren.MOD_ID);
        EpicFightEventHooks.Player.SET_TARGET.registerEvent(EpicFightIntegration::onSetTarget, Guzhenren.MOD_ID);
        EpicFightEventHooks.Animation.ATTACK_PHASE_END.registerEvent(EpicFightIntegration::onAttackPhaseEnd,
                Guzhenren.MOD_ID);
    }

    public static void onAnimationRegistry(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(Guzhenren.MOD_ID, builder -> {
            dashForward = builder.nextAccessor("biped/skill/dash_forward", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));
            dashBackward = builder.nextAccessor("biped/skill/dash_backward", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));
            dashLeft = builder.nextAccessor("biped/skill/dash_left", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));
            dashRight = builder.nextAccessor("biped/skill/dash_right", accessor ->
                    new DashAnimation(0.1F, accessor, 0.8F, 0.6F, Armatures.BIPED));
            chargeHold = builder.nextAccessor("biped/skill/charge_hold", accessor ->
                    new ActionAnimation(0.15F, accessor, Armatures.BIPED)
                            .addProperty(ActionAnimationProperty.STOP_MOVEMENT, true)
                            .addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER,
                                    Animations.ReusableSources.CHARGING)
                            .addProperty(StaticAnimationProperty.RESET_LIVING_MOTION, LivingMotions.IDLE));
            chargeStrike = builder.nextAccessor("biped/skill/charge_strike", accessor ->
                    new ActionAnimation(0.05F, accessor, Armatures.BIPED));
            chargeCancel = builder.nextAccessor("biped/skill/charge_cancel", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            secondStageFlightLoop = builder.nextAccessor("biped/living/second_stage_flight_loop", accessor ->
                    new StaticAnimation(true, accessor, Armatures.BIPED)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            secondStageFlightCrash = builder.nextAccessor("biped/skill/second_stage_flight_crash", accessor ->
                    new MainFrameAnimation(0.05F, accessor, Armatures.BIPED)
                            .newTimePair(0.0F, Float.MAX_VALUE)
                            .addState(EntityState.UPDATE_LIVING_MOTION, false)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            plungeFall = builder.nextAccessor("biped/living/plunge_fall", accessor ->
                    new StaticAnimation(true, accessor, Armatures.BIPED));
            plungeLand = builder.nextAccessor("biped/skill/plunge_land", accessor ->
                    new MainFrameAnimation(0.05F, accessor, Armatures.BIPED)
                            .newTimePair(0.0F, PLUNGE_LAND_LOCK)
                            .addState(EntityState.MOVEMENT_LOCKED, true)
                            .addState(EntityState.UPDATE_LIVING_MOTION, false)
                            .addState(EntityState.COMBO_ATTACKS_DOABLE, false)
                            .addState(EntityState.SKILL_EXECUTABLE, false)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            shieldRaise = builder.nextAccessor("biped/skill/shield_raise", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            throwSweep = builder.nextAccessor("biped/skill/throw_sweep", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            throwPunchRight = builder.nextAccessor("biped/skill/throw_punch_right", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            throwPunchLeft = builder.nextAccessor("biped/skill/throw_punch_left", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            meditateSit = builder.nextAccessor("biped/skill/meditate_sit", accessor ->
                    new MainFrameAnimation(0.1F, accessor, Armatures.BIPED)
                            .newTimePair(0.0F, Float.MAX_VALUE)
                            .addState(EntityState.UPDATE_LIVING_MOTION, false)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            meditateLoop = builder.nextAccessor("biped/living/meditate_loop", accessor ->
                    new StaticAnimation(true, accessor, Armatures.BIPED)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            meditateStand = builder.nextAccessor("biped/skill/meditate_stand", accessor ->
                    new MainFrameAnimation(0.05F, accessor, Armatures.BIPED)
                            .newTimePair(0.0F, MEDITATE_STAND_HOLD)
                            .addState(EntityState.UPDATE_LIVING_MOTION, false)
                            .addProperty(StaticAnimationProperty.FIXED_HEAD_ROTATION, true));
            useReach = builder.nextAccessor("biped/skill/use_reach", accessor ->
                    new StaticAnimation(0.05F, false, accessor, Armatures.BIPED));
            refineLoop = builder.nextAccessor("biped/living/refine_loop", accessor ->
                    new StaticAnimation(true, accessor, Armatures.BIPED));
        });
    }

    static AssetAccessor<? extends StaticAnimation> getSecondStageFlightLoop() { return secondStageFlightLoop; }

    public static void crashFlight(ServerPlayer player) {
        if (!player.isDeadOrDying()) play(player, secondStageFlightCrash);
    }

    static AssetAccessor<? extends StaticAnimation> getPlungeFall() { return plungeFall; }

    public static void landPlunge(ServerPlayer player) {
        if (!player.isDeadOrDying()) play(player, plungeLand);
    }

    public static void raiseShield(ServerPlayer player) {
        ownClips.put(player, player.serverLevel().getGameTime());
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || shieldRaise == null || player.isDeadOrDying()) return;
        patch.playAnimationInClientSide(shieldRaise, 0.0F);
    }

    public static void throwProjectile(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        ThrowRun last = throwRuns.get(player);
        ThrowClip clip = last == null ? ThrowClip.SWEEP : getThrowClip(last.clip(), now - last.tick());
        throwRuns.put(player, new ThrowRun(clip, now));
        ownClips.put(player, now);

        AnimationManager.AnimationAccessor<StaticAnimation> animation = switch (clip) {
            case SWEEP -> throwSweep;
            case PUNCH_RIGHT -> throwPunchRight;
            case PUNCH_LEFT -> throwPunchLeft;
        };
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || animation == null || player.isDeadOrDying()) return;
        patch.playAnimationInClientSide(animation, 0.0F);
    }

    static ThrowClip getThrowClip(ThrowClip last, long gap) {
        if (gap < 0L || gap > THROW_RUN_TICKS) return ThrowClip.SWEEP;
        return last == ThrowClip.PUNCH_LEFT ? ThrowClip.PUNCH_RIGHT : ThrowClip.PUNCH_LEFT;
    }

    public static void useGu(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null || useReach == null || player.isDeadOrDying()) return;
        if (!playsReach(AutoUse.isStarting(player), isMeditating(player), ownClips.get(player),
                player.serverLevel().getGameTime())) return;
        patch.playAnimationInClientSide(useReach, 0.0F);
    }

    static boolean playsReach(boolean autoUse, boolean seated, Long ownClipTick, long now) {
        return !autoUse && !seated && (ownClipTick == null || ownClipTick != now);
    }

    static AssetAccessor<? extends StaticAnimation> getRefineLoop() { return refineLoop; }

    static AssetAccessor<? extends StaticAnimation> getMeditateLoop() { return meditateLoop; }

    public static boolean isMeditating(Player player) {
        return ApertureNourishService.isCultivating(player) || AwakeningRitualService.holdsStill(player)
                || TendedGuItem.isChanneling(player);
    }

    public static void tickMeditation(ServerPlayer player) {
        boolean seated = isMeditating(player);
        MeditationCue cue = getMeditationCue(meditating.put(player, seated), seated);
        if (cue == MeditationCue.NONE || player.isDeadOrDying()) return;
        play(player, cue == MeditationCue.SIT ? meditateSit : meditateStand);
    }

    static MeditationCue getMeditationCue(Boolean last, boolean seated) {
        if (last == null || last == seated) return MeditationCue.NONE;
        return seated ? MeditationCue.SIT : MeditationCue.STAND;
    }

    public static void holdCharge(ServerPlayer player) { play(player, chargeHold); }

    public static void strikeCharge(ServerPlayer player) { play(player, chargeStrike); }

    public static void cancelCharge(ServerPlayer player) { play(player, chargeCancel); }

    private static void play(ServerPlayer player, AssetAccessor<? extends StaticAnimation> animation) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null && animation != null) patch.playAnimationSynchronized(animation, 0.0F);
    }

    public static void dash(ServerPlayer player, int vertical, int horizontal, float yRot) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        if (!EpicFightSkills.STEP.get().isExecutableState(patch)) return;

        AnimationManager.AnimationAccessor<DodgeAnimation> animation = switch (getDashClip(vertical, horizontal)) {
            case BACKWARD -> dashBackward;
            case LEFT -> dashLeft;
            case RIGHT -> dashRight;
            case FORWARD -> dashForward;
        };
        if (animation == null) return;
        Vec3 direction = dashDirection(vertical, yRot);
        if (PathStrengthService.get(player).hasTwoBoars()) {
            CrashBreach.clear(player, direction, CrashDash.getTravel(getDashScale(player)) + CrashBreach.MARGIN);
        }
        patch.playAnimationSynchronized(animation, 0.0F);
        patch.setModelYRot(yRot, true);
        RingConeEmitter.dashCone(player, direction);
    }

    public static double getDashScale(Player player) {
        return CrashDash.getScale(StrengthService.isAllOut(player),
                PathStrengthService.get(player).getBeastDashBonus());
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

    static DashClip getDashClip(int vertical, int horizontal) {
        if (vertical < 0) return DashClip.BACKWARD;
        if (vertical > 0 || horizontal == 0) return DashClip.FORWARD;
        return horizontal > 0 ? DashClip.LEFT : DashClip.RIGHT;
    }

    enum DashClip { FORWARD, BACKWARD, LEFT, RIGHT }

    enum ThrowClip { SWEEP, PUNCH_RIGHT, PUNCH_LEFT }

    enum MeditationCue { NONE, SIT, STAND }

    private record ThrowRun(ThrowClip clip, long tick) {}

    private static final class DashAnimation extends DodgeAnimation {

        private DashAnimation(float transitionTime, AnimationManager.AnimationAccessor<DodgeAnimation> accessor,
                              float width, float height, AssetAccessor<? extends Armature> armature) {
            super(transitionTime, accessor, width, height, armature);
        }

        @Override
        protected Vec3 getCoordVector(LivingEntityPatch<?> entityPatch,
                                      AssetAccessor<? extends DynamicAnimation> animation) {
            double scale = entityPatch.getOriginal() instanceof Player player ? getDashScale(player)
                    : CrashDash.COORD_SCALE;
            return super.getCoordVector(entityPatch, animation).scale(scale);
        }
    }
}
