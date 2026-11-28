package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldKind;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldPlot;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapPart;

/**
 * 选中各田的合计，田坞（{@link MapActionBar}）用：田数、规划田数、已种 / 耕地格、熟 / 未熟、按已种株加权的生长进度、普通田与蛊植田数、
 * 共同作物（不一时 {@link CropKeys#MIXED}），以及哪些作物能选（{@link #canPick}）、能种（{@link #canSow}）。
 * <ul>
 *   <li>田里有株就只能选、种它现有的作物（未选的田按株数最多的算），换别的要先清除作物；空田什么都能选。</li>
 *   <li>改蛊植田要的腐泥冻土 = 所选普通田数 × {@link #cost}；催熟要的骨粉 = 未熟株数（每株 1 份，Alex 定）。</li>
 * </ul>
 */

record FieldTotals(int fields, int planned, int planted, int cells, int mature, int growing, int growth, int plain,
                   int guPlant, int crop, boolean bare, int growingCrops, int openCrops) {

    static final FieldTotals NONE = new FieldTotals(0, 0, 0, 0, 0, 0, 0, 0, 0, CropKeys.MIXED, false, 0, 0);

    /** 一块整区块田的耕地格数（= 改蛊植田扣的腐泥冻土数）。 */
    static int cost() { return FieldPlot.of(0, 0, 0).farmland(); }

    static FieldTotals of(List<MapPlots.Plot> chosen, MapTools.Land land) {
        int fields = 0;
        int planned = 0;
        int mature = 0;
        int growing = 0;
        long weighted = 0L;
        int plain = 0;
        int crop = CropKeys.NONE;
        boolean bare = false;
        int growingCrops = 0;
        int openCrops = 0;
        for (MapPlots.Plot plot : chosen) {
            if (plot.kind() == MapPlots.Kind.PLANNED) planned++;
            if (plot.kind() != MapPlots.Kind.FIELD) continue;
            MapPart.Section.Field field = land.field(plot.index());
            if (field == null) continue;
            fields++;
            mature += field.mature();
            growing += field.growing();
            weighted += (long) field.growth() * field.planted();
            if (FieldKind.byId(field.kind()) == FieldKind.PLAIN) plain++;
            crop = crop == CropKeys.NONE || crop == field.crop() ? field.crop() : CropKeys.MIXED;
            int grows = field.crop() == FarmState.NO_CROP ? field.main() : field.crop();
            if (field.planted() == 0 || grows == FarmState.NO_CROP) {
                bare = true;
            } else {
                growingCrops |= 1 << grows;
                if (field.planted() < cost()) openCrops |= 1 << grows;
            }
        }
        int planted = mature + growing;
        return new FieldTotals(fields, planned, planted, fields * cost(), mature, growing,
                planted == 0 ? 0 : (int) (weighted / planted), plain, fields - plain,
                crop == CropKeys.NONE ? CropKeys.MIXED : crop, bare, growingCrops, openCrops);
    }

    /** 所选田里有没有能选 {@code crop} 的（空田，或正种着它）。 */
    boolean canPick(int crop) { return fields > 0 && (bare || (growingCrops & 1 << crop) != 0); }

    /** 所选田里有没有能补种 {@code crop} 的空格。 */
    boolean canSow(int crop) { return crop >= 0 && fields > 0 && (bare || (openCrops & 1 << crop) != 0); }

    long mudNeed() { return (long) plain * cost(); }

    int empty() { return Math.max(0, cells - planted); }
}
