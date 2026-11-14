package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.screen.InfoRowRenderer.RowBlock;
import net.alex.guzhenren.client.screen.InfoRowRenderer.ScreenRow;
import net.alex.guzhenren.display.InfoModel;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.Nullable;

/**
 * B 面板已绘制行的跨帧缓存，模型不变的帧不建文字。
 * <ul>
 *   <li>界面仍每帧向 {@link InfoModel} 取模型；与上次构建所用模型不相等时才由 {@link InfoRowRenderer} 重建。</li>
 *   <li>比较相等而非附件同一性：有些值不在任何附件里（效果攻击加成、效果剩余时间背后的游戏时间），变化当帧须刷新。</li>
 *   <li>普通行、空窍[Aperture]列两槽独立，各存重建时量好的 {@link RowBlock}，不变的帧不再量标签列和值列字宽。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @see InfoRowRenderer
 * @since 1.0.0
 */

final class InfoRowCache {

    private @Nullable List<InfoModel.Row> rowsModel;
    private RowBlock rows = RowBlock.EMPTY;
    private @Nullable List<InfoModel.Row> groupsModel;
    private List<RowBlock> groups = List.of();

    RowBlock getRows(Font font, List<InfoModel.Row> model) {
        if (!model.equals(rowsModel)) {
            rows = InfoRowRenderer.getBlock(font, InfoRowRenderer.getRows(model));
            rowsModel = model;
        }
        return rows;
    }

    List<RowBlock> getApertureGroups(Font font, List<InfoModel.Row> model) {
        if (!model.equals(groupsModel)) {
            List<RowBlock> blocks = new ArrayList<>();
            for (List<ScreenRow> group : InfoRowRenderer.getApertureGroups(model)) {
                blocks.add(InfoRowRenderer.getBlock(font, group));
            }
            groups = blocks;
            groupsModel = model;
        }
        return groups;
    }
}
