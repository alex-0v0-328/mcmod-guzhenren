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
 * 宝黄天[Treasure Yellow Heaven]交易魂实体：悬停原地的交互式交易 NPC。
 * <ul>
 *   <li>右键交互为存活玩家打开 {@link SoulTradeMenu}（报价取自 {@link SoulTrader}，外观由客户端渲染器选配）；实
 *       体看向 {@link #LOOK_RANGE} 内玩家，完全固定原地（{@link #travel} 为空、无重力、不可推/骑/拴、免疫爆
 *       炸、不自然消失且无掉落），仅受绕过无敌的伤害（如 {@code /kill} 与虚空）。</li>
 *   <li>GeckoLib 动画模型需提供两个控制器：{@code movement} 平时循环 {@code animation.idle}、初生时先播 20
 *       tick {@code animation.appear}、濒死保持 24 tick {@code animation.disappear} 至 {@link #tickDeath} 移
 *       除；{@code gesture} 仅叠播单次触发动画（仅驱动身体、头与双臂），在玩家新走入 {@link #BECKON_RANGE}
 *       时播 {@code beckon}，成交播 {@code cross_arms_nod}，被拒播 {@code hands_hips_shake}。</li>
 *   <li>手势锁定时长取动画长度加淡入时间 {@code GESTURE_TRANSITION}（5 tick）：交易反应可打断招手但不会打断进
 *       行中的另一次反应（避免连续点击每 tick 重启点头）；尚在出现阶段时不扫描玩家到达，待成形后再对等候玩家
 *       触发招手。</li>
 *   <li>⚠ {@code DATA_APPEARING} 默认值为 {@code true} 且随生成包下发，使实体首帧即以出现动画起手而不会先闪现全
 *       尺寸模型；读档时若存在 {@code Appeared} 标记则置为 {@code false}（无该键的 {@code /summon} 仍播出现），
 *       客户端每个实例仅在首帧锁存一次 {@code opensWithAppear}。</li>
 *   <li>直接继承 {@link Mob} 而不接入野兽状态机或捕获链；NBT 仅持久化 {@code Appeared}，手势计时与已迎客玩家集
 *       合均为运行期内存状态。</li>
 * </ul>
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
