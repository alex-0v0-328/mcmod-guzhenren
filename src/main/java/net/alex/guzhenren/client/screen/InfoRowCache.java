package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.screen.InfoRowRenderer.RowBlock;
import net.alex.guzhenren.client.screen.InfoRowRenderer.ScreenRow;
import net.alex.guzhenren.display.InfoModel;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.Nullable;

/**
 * B 面板已排版信息行的跨帧缓存：当数据模型未变化时直接复用上一帧布局，避免每帧重新构建文本组件。
 * <ul>
 *   <li>界面仍每帧向 {@link InfoModel} 请求快照，仅在与上次构建缓存所用模型不相等时才调用 {@link InfoRowRenderer} 重建
 *       缓存。</li>
 *   <li>刻意比较模型值相等而非附件对象同一性：部分展示值并不保存在任何附件中（例如效果提供的攻击加成与效果剩余时间背后
 *       的游戏时间流逝），必须确保数值变化的当帧即刻刷新。</li>
 *   <li>普通信息行与空窍分段分别持有独立缓存槽，各自存储重建时量算完毕的 {@link RowBlock}，静帧下无需重复测量标签列与
 *       值列字宽。</li>
 * </ul>
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
