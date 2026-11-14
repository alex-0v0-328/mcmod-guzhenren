package net.alex.guzhenren.gameplay.path.strength;

/**
 * 运行期间等于一某之力的限时兽力[Beast Strength]增益。花豕蛊[Flower Boar Gu]一猪，
 * 蛮力天牛蛊[Brute Force Longhorn Beetle Gu]一牛，由原版效果实现；B 面板据此在力道造诣下显示带倒计时的兽力，
 * 不列入流派增益；点数取流派效果的力道丝，经 {@link PathStrengthService#getBeastBuffs} 成无承受力的
 * {@link StrengthAmplifier}，超出即肉体过载（{@link BodyOverload}）；不计入永久兽力准入。
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrengthFamily
 * @since 1.0.0
 */

public interface BeastStrengthBuff {

    BeastStrengthFamily getBeastFamily();
}
