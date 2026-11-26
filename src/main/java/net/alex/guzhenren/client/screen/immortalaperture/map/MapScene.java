package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapMark;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页的地图层绘制：底色 → 地形贴图 → 规划田轮廓、悬停或选中的占地、工具目标 → 活物点 → 玩家头像，全部按维度时间乘
 * {@link MapTint}（轮廓与头像除外）。
 * <ul>
 *   <li>⚠ 贴图与头像是立即绘制，排队中的填充必须先 {@code flush}，否则会盖到后画的东西上；所以底色后、头像前各
 *       flush 一次。</li>
 *   <li>占地轮廓画在占地中心那一列的真实地面高度上（取不到地图片时用相机参照高度）。</li>
 *   <li>没拿工具、光标下也没有占地时，描光标下那一列。</li>
 * </ul>
 */

final class MapScene {

    private MapScene() {}

    /** 一帧要叠加的东西：{@code highlighted} 是悬停或卡片选中的占地，{@code target} 是工具目标。 */
    record Layers(List<MapPlots.Plot> plots, MapPlots.@Nullable Plot highlighted, MapTools.@Nullable Target target,
                  int targetColour, MapPick.@Nullable Hit hover) {}

    static void draw(GuiGraphics graphics, MapCamera camera, MapSurface surface, int top, int width, int height,
                     double guiScale, Layers layers) {
        long time = Minecraft.getInstance().level == null ? 6000L : Minecraft.getInstance().level.getDayTime();
        MapTint tint = MapTint.at(time);
        graphics.fill(0, top, width, height, tint.apply(MapPalette.BACKDROP));
        graphics.flush();
        surface.draw(graphics, camera, 0, top, guiScale, tint);
        MapOverlay.draw(graphics, camera, 0, top, guiScale, overlay -> {
            for (MapPlots.Plot plot : layers.plots()) {
                if (plot.kind() == MapPlots.Kind.PLANNED) outline(overlay, camera, plot, MapOverlay.PLANNED);
            }
            if (layers.highlighted() != null) outline(overlay, camera, layers.highlighted(), MapOverlay.SELECTED);
            MapTools.Target target = layers.target();
            if (target != null) {
                MapTools.outline(overlay, target.chunkX(), target.chunkZ(), target.size(),
                        centreGround(camera, target.chunkX(), target.chunkZ(), target.size()), layers.targetColour());
            } else if (layers.highlighted() == null && layers.hover() != null) {
                overlay.select(layers.hover().x(), layers.hover().z(), layers.hover().top());
            }
            for (MapMark mark : LandMapClient.marks()) {
                if (mark.kind() == MapMark.PLAYER && mark.player().isPresent()) continue;
                overlay.mark(mark.x() + 0.5, mark.y(), mark.z() + 0.5, tint.apply(MapLegend.colour(mark.kind())));
            }
            graphics.flush();
            for (MapMark mark : LandMapClient.marks()) {
                if (mark.kind() != MapMark.PLAYER || mark.player().isEmpty()) continue;
                overlay.head(mark.player().get(), mark.x() + 0.5, mark.y(), mark.z() + 0.5);
            }
        });
    }

    /** 相机对准山心（参照平面 = 山底）；山心未解出（{@code BlockPos.ZERO}）时对准玩家脚下；都不行返回假。 */
    static boolean aim(MapCamera camera, BlockPos mountain) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!mountain.equals(BlockPos.ZERO)) {
            camera.setCenter(mountain.getX() + 0.5, mountain.getY(), mountain.getZ() + 0.5);
        } else if (player != null) {
            camera.setCenter(player.getX(), Math.floor(player.getY()) - 1.0, player.getZ());
        } else {
            return false;
        }
        return true;
    }

    static void labels(GuiGraphics graphics, Font font, MapCamera camera, MapSurface surface, int top,
                       double guiScale) {
        if (!camera.plan()) return;
        MapOverlay.draw(graphics, camera, 0, top, guiScale, overlay -> {
            for (MapRaster.Label label : surface.labels(camera)) {
                overlay.label(font, Integer.toString(label.level()), label.x(), label.y(), 0xFFFFFFFF);
            }
        });
    }

    /** 视口内 GUI 坐标（相对视口左上角）→ 光标下的列；{@code scale} 为每地图像素的 GUI 像素数。 */
    static MapPick.@Nullable Hit pick(MapCamera camera, double viewX, double viewY, double scale) {
        int pixelX = camera.originX() + (int) Math.floor(viewX / scale);
        int pixelY = camera.originY() + (int) Math.floor(viewY / scale);
        return MapPick.pick(camera.projection(), LandMapClient::tile, camera.fogPlane(), pixelX, pixelY);
    }

    private static void outline(MapOverlay overlay, MapCamera camera, MapPlots.Plot plot, int colour) {
        MapTools.outline(overlay, plot.chunkX(), plot.chunkZ(), plot.size(),
                centreGround(camera, plot.chunkX(), plot.chunkZ(), plot.size()), colour);
    }

    private static int centreGround(MapCamera camera, int chunkX, int chunkZ, int size) {
        int x = chunkX * 16 + size * 8;
        int z = chunkZ * 16 + size * 8;
        MapTile tile = LandMapClient.tile(x >> 4, z >> 4);
        return tile == null ? (int) Math.floor(camera.y()) : tile.groundAt(x, z);
    }
}
