package net.alex.guzhenren.gameplay.impact;

import net.alex.guzhenren.core.ModEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 身体高速撞地炸出的撞击坑。
 *
 * <ul>
 *   <li>坠击落地、二段飞行[Second-Stage Flight]坠毁经 {@link #blast}：撞击点原版爆炸一次，只破方块；
 *       {@link NoEntityDamage} 拒绝一切实体伤害，生物、玩家、掉落物完好；保留原版击退，原版已排除玩家自己。</li>
 *   <li>先问 {@code ModEvents#canGrief}：拒绝则不破块，照样击退、出声；允许则按原版爆炸抗性破块，掉落同苦力怕
 *       （{@code mobExplosionDropDecay}）；坑内 TNT 按原版 {@code TntBlock#wasExploded} 点燃，连锁爆炸是保留特性。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ImpactCrater {

    private ImpactCrater() {}

    public static void blast(@NotNull ServerPlayer player, @NotNull Vec3 at, float power) {
        Level.ExplosionInteraction interaction = ModEvents.canGrief(player.level(), player)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        player.level().explode(player, null, new NoEntityDamage(), at.x, at.y, at.z, power, false, interaction);
    }

    private static final class NoEntityDamage extends ExplosionDamageCalculator {

        @Override
        public boolean shouldDamageEntity(@NotNull Explosion explosion, @NotNull Entity entity) { return false; }
    }
}
