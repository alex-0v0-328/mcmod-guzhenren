package net.alex.guzhenren.client.screen;

import java.util.List;
import net.alex.guzhenren.client.screen.InfoRowRenderer.ScreenRow;
import net.alex.guzhenren.display.InfoModel;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps the B panel's drawn rows from one frame to the next, so a frame whose model did not change builds no text.
 *
 * <p>The screen still asks {@link InfoModel} for the model every frame and hands it in; the rows are rebuilt by
 * {@link InfoRowRenderer} only when the model is not equal to the one they were built from. Equality, not the
 * identity of the attachments behind it, because some rows read what no attachment holds (an effect's attack
 * bonus, the game time behind a qi reading), and those must change the frame they change. The two slots, the
 * ordinary rows and the aperture columns, are independent.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @see InfoRowRenderer
 * @since 1.0.0
 */

final class InfoRowCache {

    private @Nullable List<InfoModel.Row> rowsModel;
    private List<ScreenRow> rows = List.of();
    private @Nullable List<InfoModel.Row> groupsModel;
    private List<List<ScreenRow>> groups = List.of();

    List<ScreenRow> getRows(List<InfoModel.Row> model) {
        if (!model.equals(rowsModel)) {
            rows = InfoRowRenderer.getRows(model);
            rowsModel = model;
        }
        return rows;
    }

    List<List<ScreenRow>> getApertureGroups(List<InfoModel.Row> model) {
        if (!model.equals(groupsModel)) {
            groups = InfoRowRenderer.getApertureGroups(model);
            groupsModel = model;
        }
        return groups;
    }
}
