package net.alex.guzhenren.entity.projectile;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 气波[Qi Wave]与月刃[Moon Blade]共用的命中规则（Alex 定）：{@link QiWaveEntity}、{@link BeastChargeEntity} 的爆开
 * （{@link QiBurst}）与 {@link MoonBladeEntity} 的飞行判定都只问 {@link #canHit}，不各写一份。
 * <ul>
 *   <li>{@link #canHit}：活着、可被拾取且不是旁观者即命中；队友、施放者自己的宠物、野生蛊与盔甲架都会被命中并受伤，施放
 *       者自己也在内。不再沿用范围伤害[Area Damage]的 {@code AreaDamage#canHit}（力道范围伤害、二段飞行冲撞仍用它）。
 *       盔甲架在原版 {@code ArmorStand#hurt} 里只吃带 {@code can_break_armor_stand} 标签的伤害类型，气波、蓄力一拳与月
 *       刃的类型均已加入该标签。</li>
 *   <li>⚠ 刚发射时不打自己靠原版 {@code Projectile#canHitEntity}（离手判定 {@code leftOwner}，射手同乘载具者亦然），两弹
 *       射物的 {@code canHitEntity} 先调 {@code super} 再调本类；服务器 {@code pvp=false} 与计分板队伍
 *       {@code friendlyFire=false} 由原版 {@code ServerPlayer#hurt} 的 {@code canHarmPlayer} 照常挡下，不绕过。</li>
 *   <li>⚠ 打到施放者自己时，最终伤害（护甲、附魔、效果减伤之后，吸收之前）减半（{@link #SELF_HIT_FACTOR}）：
 *       {@link #onDamage} 在 {@link LivingDamageEvent.Pre} 里判 {@link #isSelfHit}（直接来源是气波、兽力冲击或月刃，且伤
 *       害来源就是受害者本人）后改写伤害；无状态，不分玩家，单人与专用服务端一致。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ProjectileHits {

    private ProjectileHits() {}

    public static final float SELF_HIT_FACTOR = 0.5F;

    public static boolean canHit(@NotNull Entity target) {
        return target.isAlive() && !target.isSpectator() && target.isPickable();
    }

    public static boolean isSelfHit(@NotNull DamageSource source, @NotNull Entity victim) {
        Entity direct = source.getDirectEntity();
        boolean ours = direct instanceof QiWaveEntity || direct instanceof BeastChargeEntity
                || direct instanceof MoonBladeEntity;
        return ours && source.getEntity() == victim;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (isSelfHit(event.getSource(), event.getEntity())) {
            event.setNewDamage(event.getNewDamage() * SELF_HIT_FACTOR);
        }
    }
}
