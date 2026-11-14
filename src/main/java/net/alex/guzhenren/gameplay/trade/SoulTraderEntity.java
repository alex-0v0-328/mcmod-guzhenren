package net.alex.guzhenren.gameplay.trade;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 来宝黄天[Treasure Yellow Heaven] 交易的魂。
 * <ul>
 *   <li>悬停原地，看向 {@link #LOOK_RANGE} 内玩家，右键开 {@link SoulTradeMenu}；报价归 {@link SoulTrader}，外观归渲染
 *       器；不自行移动也移不动（{@link #travel} 为空）：不可推、骑、拴，无视爆炸；只受绕过无敌的伤害（{@code /kill}、虚
 *       空）；不消失，无掉落。</li>
 *   <li>模型契约用两个控制器：{@code movement} 循环 {@code animation.idle}，新入世先播 {@code animation.appear}（20
 *       tick），濒死保持 {@code animation.disappear}（24 tick）至 {@link #tickDeath} 移除。</li>
 *   <li>{@code gesture} 只叠播触发式单次动画，只动身体、头、臂，不动根骨和腿：玩家进入 {@link #BECKON_RANGE} 播
 *       {@code beckon}，成交播 {@code cross_arms_nod}，被拒播 {@code hands_hips_shake}。</li>
 *   <li>交易反应可打断招手、不打断另一反应，连点不每 tick 重启点头；手势计时 = 动画长 + 控制器
 *       {@code GESTURE_TRANSITION} 淡入，同客户端显示，运行中再触发同一动画不重启；出现中不扫到达，等候者成形后算到
 *       达。</li>
 *   <li>⚠ {@code DATA_APPEARING} 生来为 true：生成包即带，首帧即出现首帧，不会全尺寸再缩小；读档带 {@code Appeared} 的
 *       以 false 起步，带 NBT 的 {@code /summon} 无此键仍播出现；客户端每实例只判一次是否以出现开场。</li>
 *   <li>直接继承 {@code Mob}，不进野兽动作状态机或捕获链；存档只写 {@code Appeared}，手势计时与迎客的玩家集合
 *       不存。</li>
 * </ul>
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
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_APPEARING, true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, (float) LOOK_RANGE, 1.0F));
    }

    //region trading [交易]
    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!this.isAlive()) return InteractionResult.PASS;

        if (player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider(
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

    //region the soul's own clock [魂的时钟]
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

    //region staying put [原地不动]
    @Override
    public void travel(@NotNull Vec3 travelVector) {}

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected boolean canRide(@NotNull Entity vehicle) { return false; }

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean ignoreExplosion(@NotNull Explosion explosion) { return true; }

    @Override
    public boolean isNoGravity() { return true; }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

    @Override
    public boolean requiresCustomPersistence() { return true; }
    //endregion

    //region harm [受伤]
    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        super.die(damageSource);
        if (!this.level().isClientSide()) this.stopTriggeredAnim(GESTURE, null);
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
