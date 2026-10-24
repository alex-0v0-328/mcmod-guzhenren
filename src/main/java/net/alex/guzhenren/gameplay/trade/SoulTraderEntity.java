package net.alex.guzhenren.gameplay.trade;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.alex.guzhenren.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A soul that has come to the Treasure Yellow Heaven [宝黄天] to trade: it hovers where it was summoned, turns
 * its head toward players within {@link #LOOK_RANGE}, and opens {@link SoulTradeMenu} on a right click. Which
 * offers it carries is its {@link SoulTrader}; the look is the renderer's.
 *
 * <p>It never moves on its own and cannot be moved: {@link #travel} is empty, and it is not pushable, not
 * rideable, not leashable and ignores explosions. It takes damage only from sources that bypass invulnerability
 * ({@code /kill}, the void), never despawns, and drops nothing.
 *
 * <p>The model's contract runs on two controllers. {@code movement} loops {@code animation.idle}; a trader that
 * has just come into the world first plays {@code animation.appear} (20 ticks), and a dying one holds
 * {@code animation.disappear} (24 ticks) until {@link #tickDeath} removes it. {@code gesture} only plays
 * triggered one-shots over it -- they key body, head and arms, never the root or legs: {@code beckon} when a
 * player comes within {@link #BECKON_RANGE}, {@code cross_arms_nod} after a trade and
 * {@code hands_hips_shake} after a refused one. A trade reaction may cut a beckon short but never another
 * reaction, so a burst of clicks does not restart the nod every tick. A gesture counts as running for its length
 * plus the controller's {@code GESTURE_TRANSITION} blend-in, exactly as long as the client shows it: a trigger of the
 * same animation while it still runs would not restart it. The arrival scan does not look while the
 * soul is still appearing, so a player who was already waiting counts as arriving once it has formed.
 *
 * <p>⚠ {@code DATA_APPEARING} is born true, so the spawn packet already carries it and the first client frame
 * is the first appear frame, not a full-size soul that then shrinks. A trader read back from a save that
 * says {@code Appeared} starts false instead; a {@code /summon} with NBT has no such key and still appears.
 * The client decides once per entity instance whether its movement animation opens with the appear animation.
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulTrader
 * @see SoulTradeMenu
 * @since 1.0.0
 */

public class SoulTraderEntity extends Mob implements GeoEntity {

    public static final double LOOK_RANGE = 8.0D;
    public static final double BECKON_RANGE = 6.0D;
    public static final double TRADE_RANGE = 8.0D;
    public static final int APPEAR_TICKS = 20;
    public static final int DISAPPEAR_TICKS = 24;
    public static final int BECKON_TICKS = 44;
    public static final int REACTION_TICKS = 52;
    private static final int SCAN_INTERVAL = 10;
    private static final int GESTURE_TRANSITION = 5;
    private static final double CULL_MARGIN = 0.5D;
    private static final String MOVEMENT = "movement";
    private static final String GESTURE = "gesture";
    private static final String BECKON = "beckon";
    private static final String NOD = "nod";
    private static final String SHAKE = "shake";
    private static final EntityDataAccessor<Boolean> DATA_APPEARING = SynchedEntityData.defineId(
            SoulTraderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation APPEAR_ANIM =
            RawAnimation.begin().thenPlay("animation.appear").thenLoop("animation.idle");
    private static final RawAnimation DISAPPEAR_ANIM =
            RawAnimation.begin().thenPlayAndHold("animation.disappear");
    private static final RawAnimation BECKON_ANIM =
            RawAnimation.begin().thenPlay("animation.beckon");
    private static final RawAnimation NOD_ANIM =
            RawAnimation.begin().thenPlay("animation.cross_arms_nod");
    private static final RawAnimation SHAKE_ANIM =
            RawAnimation.begin().thenPlay("animation.hands_hips_shake");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final SoulTrader trader;
    private Set<UUID> nearby = new HashSet<>();
    private int appearTicks;
    private int gestureTicks;
    private boolean reacting;
    private @Nullable Boolean opensWithAppear;

    public SoulTraderEntity(EntityType<? extends SoulTraderEntity> type, Level level, SoulTrader trader) {
        super(type, level);
        this.trader = trader;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    public SoulTrader getTrader() { return this.trader; }

    public boolean isAppearing() { return this.entityData.get(DATA_APPEARING); }

    public boolean isGesturing() { return this.gestureTicks > 0; }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_APPEARING, true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, (float) LOOK_RANGE, 1.0F));
    }

    //region trading -- a right click opens the offers; the menu settles, the soul only reacts
    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!this.isAlive()) return InteractionResult.PASS;

        if (player instanceof ServerPlayer server) {
            ModNetwork.openMenu(server, new SimpleMenuProvider(
                    (id, inventory, who) -> new SoulTradeMenu(id, inventory, this), this.getDisplayName()),
                    buffer -> SoulTradeMenu.writeOffers(buffer, this.trader.getOffers()));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    public void reactToTrade(boolean traded) {
        if (!this.isAlive() || this.isGesturing() && this.reacting) return;

        this.gesture(traded ? NOD : SHAKE, REACTION_TICKS, true);
    }
    //endregion

    //region the soul's own clock -- appear countdown, gesture timer, and the beckon scan
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !this.isAlive()) return;

        if (this.isAppearing() && ++this.appearTicks >= APPEAR_TICKS) this.entityData.set(DATA_APPEARING, false);
        if (this.gestureTicks > 0) this.gestureTicks--;
        if (this.tickCount % SCAN_INTERVAL == 0) this.scanForArrivals();
    }

    private void scanForArrivals() {
        if (this.isAppearing()) return;

        Set<UUID> present = new HashSet<>();
        for (Player player : this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(BECKON_RANGE), this::canBeckon)) {
            present.add(player.getUUID());
        }
        boolean arrived = !this.nearby.containsAll(present);
        this.nearby = present;
        if (arrived && !this.isGesturing()) this.gesture(BECKON, BECKON_TICKS, false);
    }

    private boolean canBeckon(Player player) {
        return player.isAlive() && !player.isSpectator() && this.distanceToSqr(player) <= BECKON_RANGE * BECKON_RANGE;
    }

    private void gesture(String name, int ticks, boolean reaction) {
        this.triggerAnim(GESTURE, name);
        this.gestureTicks = ticks + GESTURE_TRANSITION;
        this.reacting = reaction;
    }
    //endregion

    //region staying put -- no travel, no push, no ride, no leash, no knockback
    @Override
    public void travel(@NotNull Vec3 travelVector) {}

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected boolean canRide(@NotNull Entity vehicle) { return false; }

    @Override
    public boolean canBeLeashed(@NotNull Player player) { return false; }

    @Override
    public boolean ignoreExplosion() { return true; }

    @Override
    public boolean isNoGravity() { return true; }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

    @Override
    public boolean requiresCustomPersistence() { return true; }
    //endregion

    //region harm -- only what bypasses invulnerability lands, and the soul fades instead of falling over
    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        super.die(damageSource);
        if (!this.level().isClientSide()) this.stopTriggeredAnimation(GESTURE, null);
        this.gestureTicks = 0;
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= DISAPPEAR_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(@NotNull DamageSource damageSource) { return null; }

    @Override
    protected @Nullable SoundEvent getDeathSound() { return null; }

    @Override
    protected boolean shouldDropLoot() { return false; }
    //endregion

    @Override
    public @NotNull AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(CULL_MARGIN); }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Appeared", true);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Appeared")) this.entityData.set(DATA_APPEARING, false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MOVEMENT, 0, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(DISAPPEAR_ANIM);
            if (this.opensWithAppear == null) this.opensWithAppear = this.isAppearing();
            return state.setAndContinue(this.opensWithAppear ? APPEAR_ANIM : IDLE_ANIM);
        }));
        controllers.add(new AnimationController<>(this, GESTURE, GESTURE_TRANSITION, state -> PlayState.STOP)
                .triggerableAnim(BECKON, BECKON_ANIM)
                .triggerableAnim(NOD, NOD_ANIM)
                .triggerableAnim(SHAKE, SHAKE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}
