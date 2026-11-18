package net.alex.guzhenren.gameplay.path.strength;

/**
 * 持续期间等效于「一某之力」的限时兽力[Beast Strength]药水效果接口（如花豕蛊[Flower Boar Gu]对应豕、
 * 蛮力天牛蛊[Brute Force Longhorn Beetle Gu]对应牛）。
 * <ul>
 *   <li>B 面板据此将效果带倒计时归在力道「兽力」行下展示，不重复列入流派增益列表。</li>
 *   <li>力量点数取自对应流派效果上的力道道痕[Path Marks]点，经 {@link PathStrengthService#getBeastBuffs} 转为不带承受力
 *       加成的 {@link StrengthAmplifier}（超出当前承受力的部分全额发挥并触发 {@link BodyOverload} 肉体过载反噬，且不计
 *       入永久兽力分支准入）。</li>
 * </ul>
 */

public interface BeastStrengthBuff {

    BeastStrengthFamily getBeastFamily();
}
