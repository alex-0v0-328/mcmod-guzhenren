package net.alex.guzhenren.item.material;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.GuItem;

/**
 * 蛊材：具体类，无自身分类轴。无行为的蛊材直接注册为本类，需自有 {@code use()} 才开叶子类；蛊虫、蛊材都继承
 * {@link net.alex.guzhenren.item.GuItem}，故它居物品树根。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.GuItem
 * @since 1.0.0
 */

public class GuMaterialItem extends GuItem {

    private static final String KIND_KEY = "guzhenren.display.gu_material";

    public GuMaterialItem(Properties properties, Rank rank, GuPath path) {
        super(properties, rank, path);
    }

    @Override
    protected String getKindKey() { return KIND_KEY; }
}
