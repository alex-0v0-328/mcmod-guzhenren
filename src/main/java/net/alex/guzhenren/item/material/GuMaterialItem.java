package net.alex.guzhenren.item.material;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.GuItem;

/**
 * 蛊材[Gu Material]基类：具体物品类，自身不划分二级子轴；无额外行为的蛊材直接注册为本类，需要独立 {@code use()}
 * 逻辑时再派生叶子类。蛊虫与蛊材共同继承 {@link GuItem}，由其统领物品树根节点。
 */

public class GuMaterialItem extends GuItem {

    private static final String KIND_KEY = "guzhenren.display.gu_material";

    public GuMaterialItem(Properties properties, Rank rank, GuPath path) {
        super(properties, rank, path);
    }

    @Override
    protected String getKindKey() { return KIND_KEY; }
}
