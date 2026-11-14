package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.alex.guzhenren.core.NamedEnum;

/**
 * 兽力[Beast Strength]所属兽种。两猪同档，熊独档；牛档兼容青牛劳力蛊[Green Bull Labor Gu]的永久兽力与蛮力天牛蛊
 * [Brute Force Longhorn Beetle Gu]的限时兽力；读数按兽种分组；声明顺序即 B 面板顺序；序号同步给虚影，不存档；
 * {@link #canManifest} 定能否出形为兽力虚影或兽力冲击，现全能（牛借水牛模型），不能者照常抽取、解放，只不显示。
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrength
 * @see PathStrengthData
 * @since 1.0.0
 */

public enum BeastStrengthFamily implements NamedEnum {

    BOAR(true),
    OX(true),
    BEAR(true),
    TIGER(true),
    TURTLE(true),
    HORSE(true);

    private final boolean manifests;

    BeastStrengthFamily(boolean manifests) { this.manifests = manifests; }

    public boolean canManifest() { return manifests; }

    public static List<BeastStrengthFamily> getManifesting(Collection<BeastStrengthFamily> families) {
        List<BeastStrengthFamily> manifesting = new ArrayList<>();
        for (BeastStrengthFamily family : families) if (family.canManifest()) manifesting.add(family);
        return manifesting;
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.beast_family."; }
}
