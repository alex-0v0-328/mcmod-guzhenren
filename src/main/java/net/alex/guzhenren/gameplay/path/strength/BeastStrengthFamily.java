package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.alex.guzhenren.core.NamedEnum;

/**
 * 兽力[Beast Strength]所属的兽种分类枚举：按声明顺序决定 B 面板展示与网络同步序号（序号仅用于虚影同步，不落盘）。
 * <ul>
 *   <li>白豕与黑豕共用 {@link #BOAR}，青牛劳力蛊[Green Bull Labor Gu]
 *       的永久牛力与蛮力天牛蛊[Brute Force Longhorn Beetle Gu]的限时牛力共用 {@link #OX}；抽取与解放均按兽种归并计
 *       算。</li>
 *   <li>{@link #canManifest} 控制该兽种能否具象化为背后的兽力虚影或远程兽力冲击实体（当前全部开启；若为 {@code false}
 *       则照常参与抽取与解放数值结算，仅不生成模型实体）。</li>
 * </ul>
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
