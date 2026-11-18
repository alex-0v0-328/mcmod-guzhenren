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
 * 肉身高速撞地或撞墙产生的物理撞击坑工具（供坠击落地与二段飞行[Second-Stage Flight]撞毁共用）。
 * <ul>
 *   <li>{@link #blast} 在撞击点触发一次原版爆炸：通过 {@code NoEntityDamage} 屏蔽所有实体伤害（生物、玩家与掉落物均完好
 *       无损），仅保留原版对周围生物的冲击波击退（发起玩家自身已被原版排除）。</li>
 *   <li>先经 {@code ModEvents.canGrief} 检查破坏权限：若禁止破坏则切换为 {@code NONE}（仅保留音效与击退、不破方块）；若
 *       允许破坏则切换为 {@code MOB}（按原版爆炸抗性破块并沿用苦力怕掉落衰减，坑内 TNT 会被原版机制正常引爆形成连
 *       锁）。</li>
 * </ul>
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
