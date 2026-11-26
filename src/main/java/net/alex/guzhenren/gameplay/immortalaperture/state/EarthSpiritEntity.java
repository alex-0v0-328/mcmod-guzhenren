package net.alex.guzhenren.gameplay.immortalaperture.state;

import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.npc.StationaryNpc;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 地灵[Earth Spirit]：福地[Blissful Land]的执念所化，粉色人魂，常驻荡魂行宫[Soulsunder Palace]待命，右键打开经营面板。
 * <ul>
 *   <li>每个福地一个实体类型（ID = {@code earth_spirit_} + 福地 ID），构造时绑定所属 {@link BlissfulLand}；右键只发
 *       {@link EarthSpiritUseEvent}，开不开面板、给谁看什么由面板层定。</li>
 *   <li>原地不动、只受绕过无敌的伤害，见 {@link StationaryNpc}；不做手势。荡魂山震荡靠实体标签
 *       {@code guzhenren:soulsunder_immune} 豁免。丢失由 {@code BlissfulLands} 在行宫区块加载时补生成，本类不自己复活。
 *       </li>
 *   <li>GeckoLib 共用 {@code human_soul} 模型与动画：平时循环 {@code animation.idle}，死亡保持
 *       {@code animation.disappear} {@value #DISAPPEAR_TICKS} 刻后移除。</li>
 * </ul>
 */

public class EarthSpiritEntity extends StationaryNpc implements GeoEntity {

    public static final double USE_RANGE = 8.0D;
    public static final int DISAPPEAR_TICKS = 24;
    private static final String MOVEMENT = "movement";
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation DISAPPEAR_ANIM = RawAnimation.begin().thenPlayAndHold("animation.disappear");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final BlissfulLand land;

    public EarthSpiritEntity(EntityType<? extends EarthSpiritEntity> type, Level level, BlissfulLand land) {
        super(type, level, DISAPPEAR_TICKS);
        this.land = land;
    }

    public @NotNull BlissfulLand getLand() { return this.land; }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (!this.isAlive()) return InteractionResult.PASS;

        if (player instanceof ServerPlayer server) ModEvents.post(new EarthSpiritUseEvent(server, this));
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MOVEMENT, 0, state -> state.setAndContinue(
                this.isDeadOrDying() ? DISAPPEAR_ANIM : IDLE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}
