package net.alex.guzhenren.gameplay.blissful;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 地灵[Earth Spirit]：福地[Blissful Land]的执念所化，粉色人魂，常驻荡魂行宫[Soulsunder Palace]待命，右键打开经营面板。
 * <ul>
 *   <li>每个福地一个实体类型（ID = {@code earth_spirit_} + 福地 ID），构造时绑定所属 {@link BlissfulLand}；右键交给
 *       {@link EarthSpiritPanel#open}：主人打开面板，别人只看到认主条件或主人名字。</li>
 *   <li>原地不动：{@link #travel} 为空、无重力、不可推/骑/拴、免疫爆炸、不自然消失、无掉落；只看向
 *       {@value #LOOK_RANGE} 格内的玩家，不做手势。</li>
 *   <li>只受绕过无敌的伤害（{@code /kill}、虚空）；荡魂山震荡靠实体标签 {@code guzhenren:soulsunder_immune} 豁免。丢失
 *       由{@link BlissfulLands} 在行宫区块加载时补生成，本类不自己复活。</li>
 *   <li>GeckoLib 共用 {@code human_soul} 模型与动画：平时循环 {@code animation.idle}，死亡保持
 *       {@code animation.disappear} {@value #DISAPPEAR_TICKS} 刻后移除。</li>
 * </ul>
 */

public class EarthSpiritEntity extends Mob implements GeoEntity {

    public static final double LOOK_RANGE = 8.0D;
    public static final double USE_RANGE = 8.0D;
    public static final int DISAPPEAR_TICKS = 24;
    private static final String MOVEMENT = "movement";
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation DISAPPEAR_ANIM = RawAnimation.begin().thenPlayAndHold("animation.disappear");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final BlissfulLand land;

    public EarthSpiritEntity(EntityType<? extends EarthSpiritEntity> type, Level level, BlissfulLand land) {
        super(type, level);
        this.land = land;
    }

    public static AttributeSupplier.Builder createAttributes() { return Mob.createMobAttributes(); }

    public @NotNull BlissfulLand getLand() { return this.land; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, (float) LOOK_RANGE, 1.0F));
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!this.isAlive()) return InteractionResult.PASS;

        if (player instanceof ServerPlayer server) EarthSpiritPanel.open(server, this);
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    //region Staying put [原地待命]
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

    //region Harm [受伤]
    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MOVEMENT, 0, state -> state.setAndContinue(
                this.isDeadOrDying() ? DISAPPEAR_ANIM : IDLE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}
