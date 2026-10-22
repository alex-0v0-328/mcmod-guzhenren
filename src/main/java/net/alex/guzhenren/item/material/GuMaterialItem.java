package net.alex.guzhenren.item.material;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.GuItem;

/**
 * Gu material [蛊材]: concrete and with no axes of its own.
 *
 * <p>Extends {@link net.alex.guzhenren.item.GuItem}. A material with no behavior registers directly
 * as one of these; only a material that needs its own {@code use()} earns a leaf class. Both 蛊虫 and
 * 蛊材 extend {@link net.alex.guzhenren.item.GuItem}, which is why it stays at the root of the
 * item tree.
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
    protected String kindKey() { return KIND_KEY; }
}
