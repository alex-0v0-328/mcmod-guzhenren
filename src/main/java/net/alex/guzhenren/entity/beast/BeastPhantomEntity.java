package net.alex.guzhenren.entity.beast;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
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
 * 兽力虚影：攻击轮次放出的一个兽种，立于主人身旁、朝向随主人。
 * <ul>
 *   <li>显示 {@link #LIFETIME_TICKS} 后再逗留 {@link #LINGER_TICKS}，供持续出拳的下一轮续接，故末轮后
 *       {@link #getShownTicks} 才消失；淡入起播一次兽种出场动画，后待机。</li>
 *   <li>{@link Slot} 正面看分两层（Alex 布局）：地面层在主人前 {@link #FORWARD_OFFSET}，首只右、次只左，各距
 *       {@link #SIDE_OFFSET}；后层在主人后，似从脚跟长出，为 {@link #FAN_ROWS} 排成对槽位、自顶点向下张开的扇形，
 *       每排更外更低，底排离地 {@link #FAN_BASE_HEIGHT}；第三人称见整把扇形，无扇形槽正对地面槽后方。</li>
 *   <li>{@link #show} 按一轮抽中兽种更新：在场者原槽续接（寿命重计，不重生），新者按声明序占首个空槽（先地面两槽，
 *       再扇形底排至顶点），余者消失；不能出形（{@code BeastStrengthFamily.canManifest}）者略过、不占槽；续接递增同步计
 *       数器，客户端据此重置年龄。</li>
 *   <li>⚠ {@link #LINGER_TICKS} 与扇形 {@link #FAN_BACK_OFFSET}、{@link #FAN_COLUMN}、{@link #FAN_ROW}、
 *       {@link #FAN_BASE_HEIGHT} 为占位值，由 Alex 在客户端调。</li>
 *   <li>出现与消失只改绘制（{@link #getDrift}、{@link #getScale}、{@link #getFade}、{@link #getGlow}），
 *       实体始终在槽位：出现历时 {@link #APPEAR_TICKS}，每槽按声明序晚 1 tick（出场动画随之推迟），自槽后
 *       {@link #APPEAR_BACK} 格滑入淡入，大小 {@link #APPEAR_SCALE} → {@link #APPEAR_OVERSHOOT} → 1；末
 *       {@link #VANISH_TICKS} 收向主人脚上 {@link #BODY_HEIGHT} 处，缩至 {@link #VANISH_SCALE} 并淡出，发光升至
 *       {@link #GLOW_PEAK} 倍；续接不重播出现，内收中续接用 {@link #VANISH_RETURN_TICKS} 回槽；被一轮剔除者即刻消
 *       失。</li>
 *   <li>⚠ 以上出现与消失数值均为占位。</li>
 *   <li>纯显示：无碰撞，攻击与投射物打不中，无伤害，不存档；命令召唤者无主人，首 tick 即消失；两端每 tick 按主人摆放，
 *       客户端忽略服务端位置包；时间耗尽或主人离维度、死亡、下线即消失。</li>
 *   <li>有意超 300 行：槽位摆放与出现、消失曲线读同一组同步字段和 tick 状态，另有约 30 行纯显示的原版覆写；拆开要公开这
 *       些状态。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.StrengthRound
 * @since 1.0.0
 */

public final class BeastPhantomEntity extends Entity implements GeoEntity, BeastPhantomVisual {

    public static final int LIFETIME_TICKS = 20;
    public static final int LINGER_TICKS = 5;
    public static final double SIDE_OFFSET = 1.2D;
    public static final double FORWARD_OFFSET = 0.5D;
    public static final double FAN_BACK_OFFSET = 0.5D;
    public static final int FAN_ROWS = 4;
    public static final double FAN_COLUMN = 1.0D;
    public static final double FAN_ROW = 0.6D;
    public static final double FAN_BASE_HEIGHT = 0.5D;
    private static final double SEARCH_RANGE = 4.0D;
    private static final double CULL_MARGIN = 1.5D;
    private static final int NO_OWNER = -1;
    private static final EntityDataAccessor<Integer> OWNER =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FAMILY =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RENEWALS =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    public static final int APPEAR_TICKS = 6;
    public static final double APPEAR_BACK = 1.0D;
    public static final float APPEAR_SCALE = 0.7F;
    public static final float APPEAR_OVERSHOOT = 1.08F;
    public static final int VANISH_TICKS = 8;
    public static final int VANISH_RETURN_TICKS = 4;
    public static final float VANISH_SCALE = 0.2F;
    public static final double BODY_HEIGHT = 0.9D;
    public static final float GLOW_PEAK = 2.4F;
    private static final float OVERSHOOT_AT = 0.7F;
    private static final Map<BeastStrengthFamily, RawAnimation> APPEARANCES =
            BeastPhantomVisual.byFamily(family -> BeastPhantomVisual.getAppear(family, "idle"));
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int bornTick;
    private float vanish;
    private float vanishO;

    public enum Slot {

        RIGHT(0, true),
        LEFT(0, false),
        FAN_RIGHT_4(4, true),
        FAN_LEFT_4(4, false),
        FAN_RIGHT_3(3, true),
        FAN_LEFT_3(3, false),
        FAN_RIGHT_2(2, true),
        FAN_LEFT_2(2, false),
        FAN_RIGHT_1(1, true),
        FAN_LEFT_1(1, false);

        private final int row;
        private final boolean right;

        Slot(int row, boolean right) {
            this.row = row;
            this.right = right;
        }

        /** 地面层为 0，否则为从扇形顶点数起的排号。 */
        public int getRow() { return this.row; }

        private boolean isRight() { return this.right; }
    }

    public BeastPhantomEntity(EntityType<? extends BeastPhantomEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void show(ServerPlayer owner, List<BeastStrengthFamily> drawn) {
        List<BeastStrengthFamily> families = BeastStrengthFamily.getManifesting(drawn);
        List<BeastPhantomEntity> owned = findOwned(owner);
        EnumSet<Slot> taken = EnumSet.noneOf(Slot.class);
        for (BeastPhantomEntity phantom : owned) {
            if (!families.contains(phantom.getFamily())) {
                phantom.discard();
                continue;
            }
            phantom.renew();
            taken.add(phantom.getSlot());
        }
        for (BeastStrengthFamily family : families) {
            if (owned.stream().anyMatch(phantom -> phantom.isAlive() && phantom.getFamily() == family)) continue;

            Slot slot = pickSlot(taken);
            if (slot == null) continue;

            taken.add(slot);
            summon(owner, family, slot);
        }
    }

    public static @Nullable Slot pickSlot(@NotNull Set<Slot> taken) {
        for (Slot slot : Slot.values()) if (!taken.contains(slot)) return slot;
        return null;
    }

    private static BeastPhantomEntity summon(ServerPlayer owner, BeastStrengthFamily family, Slot slot) {
        BeastPhantomEntity phantom = new BeastPhantomEntity(ModEntityTypes.BEAST_PHANTOM.get(), owner.level());
        phantom.entityData.set(OWNER, owner.getId());
        phantom.entityData.set(FAMILY, family.ordinal());
        phantom.entityData.set(SLOT, slot.ordinal());
        phantom.follow(owner);
        phantom.xo = phantom.getX();
        phantom.yo = phantom.getY();
        phantom.zo = phantom.getZ();
        phantom.yRotO = phantom.getYRot();
        owner.level().addFreshEntity(phantom);
        return phantom;
    }

    public static List<BeastPhantomEntity> findOwned(ServerPlayer owner) {
        AABB area = owner.getBoundingBox().inflate(SEARCH_RANGE);
        return owner.level().getEntitiesOfClass(BeastPhantomEntity.class, area,
                phantom -> phantom.isAlive() && phantom.getOwnerId() == owner.getId());
    }

    public static @Nullable BeastPhantomEntity findOwned(ServerPlayer owner, BeastStrengthFamily family) {
        return findOwned(owner).stream().filter(phantom -> phantom.getFamily() == family).findFirst().orElse(null);
    }

    private void renew() {
        this.bornTick = this.tickCount;
        this.entityData.set(RENEWALS, this.entityData.get(RENEWALS) + 1);
    }

    public static int getShownTicks() { return LIFETIME_TICKS + LINGER_TICKS; }

    public float getAge(float partialTick) { return this.tickCount - this.bornTick + partialTick; }

    //region Coming and going [出现与消失]
    private float getAppear(float partialTick) {
        return Mth.clamp((this.tickCount + partialTick - getSlot().ordinal()) / APPEAR_TICKS, 0.0F, 1.0F);
    }

    private float getVanish(float partialTick) { return Mth.lerp(partialTick, this.vanishO, this.vanish); }

    private static float easeOut(float progress) {
        float rest = 1.0F - progress;
        return 1.0F - rest * rest * rest;
    }

    private void tickVanish() {
        boolean gathering = this.tickCount - this.bornTick > getShownTicks() - VANISH_TICKS;
        this.vanishO = this.vanish;
        this.vanish = Mth.clamp(this.vanish + (gathering ? 1.0F / VANISH_TICKS : -1.0F / VANISH_RETURN_TICKS),
                0.0F, 1.0F);
    }

    @Override
    public float getFade(float partialTick) {
        float vanish = getVanish(partialTick);
        return getAppear(partialTick) * (1.0F - vanish * vanish);
    }

    @Override
    public float getGlow(float partialTick) {
        float vanish = getVanish(partialTick);
        float late = vanish * vanish * vanish;
        return getAppear(partialTick) * Mth.lerp(vanish, 1.0F, GLOW_PEAK) * (1.0F - late * late);
    }

    @Override
    public float getScale(float partialTick) {
        float appear = getAppear(partialTick);
        float vanish = getVanish(partialTick);
        float grown = appear < OVERSHOOT_AT
                ? Mth.lerp(easeOut(appear / OVERSHOOT_AT), APPEAR_SCALE, APPEAR_OVERSHOOT)
                : Mth.lerp((appear - OVERSHOOT_AT) / (1.0F - OVERSHOOT_AT), APPEAR_OVERSHOOT, 1.0F);
        return grown * Mth.lerp(vanish * vanish, 1.0F, VANISH_SCALE);
    }

    @Override
    public Vec3 getDrift(float partialTick) {
        float vanish = getVanish(partialTick);
        double gathered = vanish * vanish;
        double back = APPEAR_BACK * (1.0F - easeOut(getAppear(partialTick)));
        Vec3 offset = getOffset(getSlot());
        return new Vec3(-offset.x * gathered, (BODY_HEIGHT - offset.y) * gathered, -offset.z * gathered - back);
    }
    //endregion

    private int getOwnerId() { return this.entityData.get(OWNER); }

    @Override
    public BeastStrengthFamily getFamily() {
        BeastStrengthFamily[] families = BeastStrengthFamily.values();
        return families[Mth.clamp(this.entityData.get(FAMILY), 0, families.length - 1)];
    }

    public Slot getSlot() {
        Slot[] slots = Slot.values();
        return slots[Mth.clamp(this.entityData.get(SLOT), 0, slots.length - 1)];
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(OWNER, NO_OWNER);
        builder.define(FAMILY, BeastStrengthFamily.BOAR.ordinal());
        builder.define(SLOT, Slot.RIGHT.ordinal());
        builder.define(RENEWALS, 0);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (RENEWALS.equals(key) && this.level().isClientSide()) this.bornTick = this.tickCount;
    }

    @Override
    public void tick() {
        super.tick();
        tickVanish();
        Entity owner = this.level().getEntity(getOwnerId());
        if (!this.level().isClientSide()
                && (!(owner instanceof Player player) || !player.isAlive()
                || this.tickCount - this.bornTick > getShownTicks())) {
            this.discard();
            return;
        }
        if (owner != null) follow(owner);
    }

    public static Vec3 getOffset(Slot slot) {
        int row = slot.getRow();
        double side = row == 0 ? SIDE_OFFSET : FAN_COLUMN * (row - 0.5D);
        double up = row == 0 ? 0.0D : FAN_BASE_HEIGHT + FAN_ROW * (FAN_ROWS - row);
        double ahead = row == 0 ? FORWARD_OFFSET : -FAN_BACK_OFFSET;
        return new Vec3(slot.isRight() ? side : -side, up, ahead);
    }

    private void follow(Entity owner) {
        float yaw = owner.getYRot();
        double radians = Math.toRadians(yaw);
        Vec3 offset = getOffset(getSlot());
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        Vec3 right = new Vec3(-Math.cos(radians), 0.0D, -Math.sin(radians));
        Vec3 position = owner.position().add(forward.scale(offset.z)).add(right.scale(offset.x)).add(0.0D, offset.y,
                0.0D);
        this.setPos(position);
        this.setYRot(yaw);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {}

    @Override
    public @NotNull AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(CULL_MARGIN); }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isAttackable() { return false; }

    @Override
    public boolean canBeHitByProjectile() { return false; }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}

    //region Animation [动画]
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "appear", 0, state -> this.tickCount < getSlot().ordinal()
                ? PlayState.STOP : state.setAndContinue(APPEARANCES.get(getFamily()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
    //endregion
}
