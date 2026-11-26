package net.alex.guzhenren.gameplay.npc;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
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

/**
 * 原地待命的 NPC 基类（地灵[Earth Spirit]、宝黄天[Treasure Yellow Heaven]交易魂共用）。
 * <ul>
 *   <li>完全固定原地：{@link #travel} 为空、无重力、不可推/骑/拴、免疫爆炸、不自然消失、无掉落、无受伤与死亡音效；只
 *       看向 {@link #LOOK_RANGE} 格内的玩家。</li>
 *   <li>只受绕过无敌的伤害（{@code /kill}、虚空）；死亡后由 {@link #tickDeath} 保持构造时给的消散刻数再移除，供子类的
 *       消散动画播完。</li>
 *   <li>继承 {@link Mob} 而不接入野兽状态机或捕获链；子类在 {@link #registerGoals} 之外另加目标时先调 {@code super}。</li>
 * </ul>
 */

public abstract class StationaryNpc extends Mob {

    public static final double LOOK_RANGE = 8.0D;
    private final int disappearTicks;

    protected StationaryNpc(EntityType<? extends StationaryNpc> type, Level level, int disappearTicks) {
        super(type, level);
        this.disappearTicks = disappearTicks;
    }

    public static AttributeSupplier.Builder createAttributes() { return Mob.createMobAttributes(); }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, (float) LOOK_RANGE, 1.0F));
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
        if (this.deathTime >= this.disappearTicks && !this.level().isClientSide() && !this.isRemoved()) {
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
}
