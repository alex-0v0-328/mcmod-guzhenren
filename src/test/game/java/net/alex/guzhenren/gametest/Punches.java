package net.alex.guzhenren.gametest;

import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 力道 GameTest 的出拳入口：不经 Epic Fight 出招或原版挥拳，直接对目标造成 {@code minecraft:player_attack} 伤害。
 * <ul>
 *   <li>⚠ 暴击掷骰与过载反噬只认带出拳标记的伤害（{@link ComboRound#markPunch}），所以先打标记再造成伤害；测非出拳伤害
 *       （二段飞行冲撞、荆棘反伤）时不要走这里。</li>
 * </ul>
 */

final class Punches {

    private Punches() {}

    static boolean land(ServerPlayer player, LivingEntity target, float amount) {
        ComboRound.markPunch(player);
        return target.hurt(player.damageSources().playerAttack(player), amount);
    }
}
