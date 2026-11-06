package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.screen.InfoRowRenderer.RowBlock;
import net.alex.guzhenren.client.screen.InfoRowRenderer.ScreenRow;
import net.alex.guzhenren.display.InfoModel;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps the B panel's drawn rows from one frame to the next, so a frame whose model did not change builds no text.
 *
 * <p>The screen still asks {@link InfoModel} for the model every frame and hands it in; the rows are rebuilt by
 * {@link InfoRowRenderer} only when the model is not equal to the one they were built from. Equality, not the
 * identity of the attachments behind it, because some rows read what no attachment holds (an effect's attack
 * bonus, the game time behind an effect's time left), and those must change the frame they change. The two slots, the
 * ordinary rows and the aperture columns, are independent. Each slot is kept as {@link RowBlock}s measured at the
 * rebuild, so the label and value columns cost no text measuring on an unchanged frame.
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
