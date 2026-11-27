package net.alex.guzhenren.client.screen.immortalaperture.map;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelPage;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapCamera;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapPick;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapSurface;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelAction;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapView;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * 「地图」页：全屏，顶栏（{@value EarthSpiritScreen#BAR_H} 像素，由 {@link EarthSpiritScreen} 画，投影压在地图上）以下整片
 * 是地图视口；地形贴图由 {@link MapSurface} 后台光栅，地图层由 {@link MapScene} 画；叠在上面的是左上资源条与左下按键提示
 * {@link MapHud}（含一键收获）、右上视角簇 {@link MapControls}、右缘高度刻度 {@link MapHeightRail}、底部正中工具栏
 * {@link MapTools}（含割弃；选中田时换成操作栏 {@link MapActionBar}）、右下图例 {@link MapLegend}、左下选区卡
 * {@link MapSelectionCard}、割弃弹窗 {@link CedePanel}。
 * <ul>
 *   <li>每帧：按窗口与 GUI 缩放定视口（1 地图像素 = 2 真实像素）、把视口区块矩形交给
 *       {@link LandMapClient#requestView}、让贴图判是否重画；底色、贴图与标记按维度时间乘 {@code MapTint}。</li>
 *   <li>⚠ 离开本页调 {@link LandMapClient#stopView}（服务端不看页，不退订会一直扫和发），关面板调
 *       {@link LandMapClient#clear} 并关掉贴图线程。</li>
 *   <li>操作（Alex 定）：右键或中键拖动、按住 WASD 平移（每秒 {@value #PAN_SPEED} GUI 像素）；滚轮以光标为锚缩放；Q 逆时针
 *       转、E 顺时针转（R 备用：顺时针，Shift+R 逆时针）；V 切等高线；H 回山心。本页吃掉这些键，E 不再关面板。按下与松开
 *       相距不到 {@value #CLICK_SLOP} 像素算点击：拿着工具即执行工具；拿着工具时右键或 Esc 退回选择模式。</li>
 *   <li>选择模式（不拿工具，Alex 定）：点占地只选它，Shift 点田加减，点空地清空；左键拖动框选矩形里的田与规划田（Shift
 *       拖为加选）；{@value #DOUBLE_CLICK_MILLIS} 毫秒内双击一块田选中所有同作物的田；Ctrl+A 全选田；Esc 依次关传送菜单、
 *       退工具、清选区，都没有才交给面板关闭。</li>
 *   <li>相机对准山心（参照平面 = 山底）；秘境还没解出山心时对准玩家脚下；「回山心」键同。</li>
 *   <li>⚠ 绘制都在 {@link #fills}（贴图、头像与罗盘指针是立即绘制，见 {@link MapScene}），叠加部件的精灵在同一步另起
 *       {@link SpriteBatch}；精灵批开画前先冲掉已排的填充，所以刻度线、图例色块、工具栏竖线排在叠加底之后、卡片之前。</li>
 * </ul>
 */

public class MapPage extends PanelPage {

    private static final int TOP = EarthSpiritScreen.BAR_H - 1;
    private static final int NO_BUTTON = -1;
    private static final int CLICK_SLOP = 3;
    private static final int PAN_SPEED = 240;
    private static final int RAIL_GAP = 8;
    private static final int CARD_GAP = 6;
    private static final long DOUBLE_CLICK_MILLIS = 300L;
    private final MapCamera camera = new MapCamera(0.0, 64.0, 0.0);
    private final MapSurface surface = new MapSurface();
    private final MapControls controls = new MapControls();
    private final MapHud hud = new MapHud();
    private final MapHeightRail rail = new MapHeightRail();
    private final MapLegend legend = new MapLegend();
    private final CedePanel cede = new CedePanel(this);
    private final MapTools tools = new MapTools();
    private final MapSelection selection = new MapSelection();
    private final MapSelectionCard card = new MapSelectionCard();
    private final MapActionBar actions = new MapActionBar();
    private final MapTeleportMenu teleport = new MapTeleportMenu();
    private final MapToolLand land = new MapToolLand(this);
    private boolean aimed;
    private int dragging = NO_BUTTON;
    private double pressX;
    private double pressY;
    private boolean moved;
    private @Nullable MapPick.Hit hover;
    private MapTools.@Nullable Target target;
    private boolean areaing;
    private boolean boxing;
    private boolean boxAdds;
    private int boxX;
    private int boxZ;
    private int @Nullable [] box;
    private long lastTap;
    private long lastTapChunk = Long.MIN_VALUE;
    private List<MapPlots.Plot> chosen = List.of();
    private FieldTotals totals = FieldTotals.NONE;
    private MapPlots.@Nullable Plot pasture;
    private int pendingCrop = CropKeys.NONE;
    private int areaX;
    private int areaZ;
    private long areaEnd = Long.MIN_VALUE;
    private List<MapTools.Target> area = List.of();
    private long lastFrame;

    public MapPage(EarthSpiritScreen screen) { super(screen); }


    @Override
    protected Component name() { return Component.translatable(EarthSpiritScreen.UI + "tab.map"); }

    /** 信息页点作物行：下一帧选中种这种作物的田（作物设为这种，或设为自动而田里这种最多；一块没有就不动选区）。 */
    public void selectCrop(int crop) { pendingCrop = crop; }

    //region Shared with parts [供部件用]
    MapPart.Section data() { return section(MapPart.INSTANCE, MapPart.Section.EMPTY); }

    MapTools.Land land() { return land; }

    void sendAction(PanelAction action) { send(action); }

    boolean tipAt(GuiGraphics graphics, int left, int top, int width, int height, int mouseX, int mouseY,
                  Component text) {
        return tip(graphics, left, top, width, height, mouseX, mouseY, text);
    }
    //endregion

    //region Lifecycle [生命周期]
    @Override
    protected void resized(int width, int height) {
        controls.layout(width);
        tools.layout(width, height);
        actions.layout(width, height);
        legend.layout(width, height);
        hud.layout(height);
        card.layout(hud.keysTop() - CARD_GAP);
        rail.layout(width, controls.bottom() + RAIL_GAP, legend.top() - RAIL_GAP);
        cede.layout(width, height);
    }

    @Override
    protected void hidden() {
        dragging = NO_BUTTON;
        endArea();
        endBox();
        lastFrame = 0L;
        cede.closeAll();
        tools.cancel();
        actions.cancel();
        selection.clear();
        teleport.close();
        LandMapClient.stopView();
    }

    @Override
    protected void closed() {
        LandMapClient.clear();
        surface.close();
    }

    private void aim() {
        if (!aimed) aimed = MapScene.aim(camera, data().mountain());
    }

    private static double guiScale() { return Minecraft.getInstance().getWindow().getGuiScale(); }

    private double scale() { return MapCamera.guiPerMapPixel(guiScale()); }

    private void sync() {
        aim();
        double perGui = guiScale() / MapCamera.REAL_PER_MAP_PIXEL;
        camera.setViewport((int) Math.ceil(width() * perGui), (int) Math.ceil((height() - TOP) * perGui));
        MapView view = camera.viewChunks();
        LandMapClient.requestView(view.minChunkX(), view.minChunkZ(), view.maxChunkX(), view.maxChunkZ());
        surface.update(camera, LandMapClient::tile, LandMapClient.revision());
    }
    //endregion

    //region Drawing [绘制]
    @Override
    protected void fills(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        panByKeys();
        sync();
        hover = pick(mouseX, mouseY);
        List<MapPlots.Plot> plots = land.plots();
        if (pendingCrop != CropKeys.NONE && !plots.isEmpty()) {
            selectCropFields(pendingCrop);
            pendingCrop = CropKeys.NONE;
        }
        selection.refresh(plots);
        chosen = selection.plots(plots);
        pasture = selection.pasture(plots);
        if (pasture != null) chosen = List.of(pasture);
        totals = FieldTotals.of(chosen, land);
        card.place(selection, totals, pasture);
        target = tools.target(land, hover);
        updateArea();
        updateBox();
        if (!area.isEmpty()) target = null;
        MapPlots.Plot under = tools.active() != null || hover == null || boxing && moved ? null
                : MapPlots.at(plots, hover.x() >> 4, hover.z() >> 4);
        MapPlots.Plot hovered = under != null && !selection.has(under) ? under : null;
        MapScene.draw(graphics, camera, surface, TOP, width(), height(), guiScale(), new MapScene.Layers(plots,
                chosen, hovered, box, target, target == null ? 0 : tools.colour(target), area, hover));
        boolean acting = selection.hasFields();
        SpriteBatch.draw(graphics, sprites -> {
            hud.sprites(sprites, font(), land, mouseX, mouseY);
            controls.sprites(sprites, camera, mouseX, mouseY);
            rail.sprites(sprites, font(), surface.lowest(), surface.highest(), hover);
            legend.sprites(sprites);
            if (acting) {
                actions.sprites(sprites, totals, land, selection, font(), mouseX, mouseY);
            } else {
                tools.sprites(sprites, mouseX, mouseY);
                tools.hint(sprites, null, font());
                tools.pickerSprites(sprites, font(), land, mouseX, mouseY);
            }
        });
        rail.fills(graphics, surface.lowest(), surface.highest(), hover);
        legend.fills(graphics);
        if (acting) actions.fills(graphics);
        else tools.fills(graphics);
        SpriteBatch.draw(graphics, sprites -> {
            card.sprites(sprites, selection, totals, pasture, land, mouseX, mouseY);
            teleport.sprites(sprites, mouseX, mouseY);
        });
        controls.needle(graphics, camera);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        MapScene.labels(graphics, font(), camera, surface, TOP, guiScale());
        hud.texts(graphics, font(), land);
        controls.texts(graphics, font());
        rail.texts(graphics, font(), surface.lowest(), surface.highest(), hover);
        if (selection.hasFields()) {
            actions.texts(graphics, font(), totals, land);
        } else {
            tools.texts(graphics, font());
            tools.hint(null, graphics, font());
            tools.pickerTexts(graphics, font(), land);
        }
        card.texts(graphics, font(), selection, totals, pasture, land);
        teleport.texts(graphics, font());
        legend.texts(graphics, font(), camera, surface.lowest(), surface.highest(), hover);
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        boolean acting = selection.hasFields();
        if (controls.tip(this, graphics, mouseX, mouseY) || hud.tip(this, graphics, land, mouseX, mouseY)
                || acting && actions.tip(this, graphics, totals, land, mouseX, mouseY)
                || !acting && (tools.tip(this, graphics, mouseX, mouseY)
                        || tools.pickerTip(this, graphics, land, mouseX, mouseY))
                || card.tip(this, graphics, selection, mouseX, mouseY)) {
            return;
        }
        if (target != null && !target.verdict().fit() && !target.verdict().reason().getString().isEmpty()) {
            graphics.renderTooltip(font(), target.verdict().reason(), mouseX, mouseY);
        }
    }

    private @Nullable MapPick.Hit pick(double mouseX, double mouseY) {
        return !isOnMap(mouseX, mouseY) || cede.isOpen() ? null : MapScene.pick(camera, mouseX, mouseY - TOP, scale());
    }

    private boolean isOnMap(double mouseX, double mouseY) {
        return mouseY >= EarthSpiritScreen.BAR_H && mouseY < height() && mouseX >= 0 && mouseX < width()
                && !controls.contains(mouseX, mouseY) && !bottomBar(mouseX, mouseY)
                && !tools.isOverPicker(mouseX, mouseY) && !hud.contains(mouseX, mouseY)
                && !legend.contains(mouseX, mouseY) && !card.contains(mouseX, mouseY)
                && !teleport.contains(mouseX, mouseY);
    }

    private boolean bottomBar(double mouseX, double mouseY) {
        return selection.hasFields() ? actions.contains(mouseX, mouseY) : tools.contains(mouseX, mouseY);
    }
    //endregion

    //region Input [输入]
    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        boolean left = button == InputConstants.MOUSE_BUTTON_LEFT;
        if (teleport.contains(mouseX, mouseY)) {
            if (left) teleport.click(land, mouseX, mouseY);
            return true;
        }
        teleport.close();
        if (card.contains(mouseX, mouseY)) {
            if (left) card.click(land, tools, selection, pasture, mouseX, mouseY);
            return true;
        }
        MapControls.Control control = controls.at(mouseX, mouseY);
        if (control != null) {
            if (left) press(control);
            return true;
        }
        if (hud.isOverHarvest(mouseX, mouseY)) {
            if (left) hud.clickHarvest(land);
            return true;
        }
        if (selection.hasFields() && actions.contains(mouseX, mouseY)) {
            if (left) actions.click(land, selection, totals, mouseX, mouseY);
            return true;
        }
        actions.cancel();
        if (!selection.hasFields() && clickTools(left, mouseX, mouseY)) return true;
        if (hud.contains(mouseX, mouseY) || legend.contains(mouseX, mouseY)) return true;
        if (!isOnMap(mouseX, mouseY)) return false;
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && tools.active() != null) {
            tools.cancel();
            return true;
        }
        dragging = button;
        pressX = mouseX;
        pressY = mouseY;
        moved = false;
        if (!left) return true;
        MapPick.Hit start = pick(mouseX, mouseY);
        if (start == null) return true;
        if (tools.active() == MapTools.Tool.FARM) {
            areaing = true;
            areaX = start.x() >> 4;
            areaZ = start.z() >> 4;
        } else if (tools.active() == null) {
            boxing = true;
            boxAdds = shift;
            boxX = start.x() >> 4;
            boxZ = start.z() >> 4;
        }
        return true;
    }

    /** 工具栏与新田作物条上的点击；点中返回真。 */
    private boolean clickTools(boolean left, double mouseX, double mouseY) {
        if (tools.isOverPicker(mouseX, mouseY)) {
            int crop = tools.pickerCrop(mouseX, mouseY);
            if (left && crop != CropKeys.NONE && crop != land.newCrop()) land.setNewCrop(crop);
            return true;
        }
        if (tools.isOverSelect(mouseX, mouseY)) {
            if (left) tools.cancel();
            return true;
        }
        MapTools.Tool tool = tools.at(mouseX, mouseY);
        if (tool != null) {
            if (left) {
                tools.press(tool);
                selection.clear();
            }
            return true;
        }
        if (tools.isOverCede(mouseX, mouseY)) {
            if (left) cede.open();
            return true;
        }
        return tools.contains(mouseX, mouseY);
    }

    private void updateArea() {
        if (!areaing || !moved || hover == null) {
            area = List.of();
            areaEnd = Long.MIN_VALUE;
            return;
        }
        long end = ChunkPos.asLong(hover.x() >> 4, hover.z() >> 4);
        if (end == areaEnd && !area.isEmpty()) return;
        areaEnd = end;
        area = tools.area(land, areaX, areaZ, hover.x() >> 4, hover.z() >> 4);
    }

    private void endArea() {
        areaing = false;
        area = List.of();
        areaEnd = Long.MIN_VALUE;
    }

    private void updateBox() {
        if (!boxing || !moved || hover == null) {
            if (!boxing) box = null;
            return;
        }
        int toX = hover.x() >> 4;
        int toZ = hover.z() >> 4;
        box = new int[] {Math.min(boxX, toX), Math.min(boxZ, toZ), Math.max(boxX, toX), Math.max(boxZ, toZ)};
    }

    private void endBox() {
        boxing = false;
        box = null;
    }

    private void press(MapControls.Control control) {
        switch (control) {
            case CONTOUR -> camera.setPlan(!camera.plan());
            case HOME -> {
                aimed = false;
                aim();
            }
            case COMPASS -> camera.rotate(1);
        }
    }

    private void panByKeys() {
        long now = Util.getMillis();
        double seconds = lastFrame == 0L ? 0.0 : Math.min(now - lastFrame, 100L) / 1000.0;
        lastFrame = now;
        if (seconds == 0.0 || cede.isOpen() || Screen.hasControlDown()) return;

        long window = Minecraft.getInstance().getWindow().getWindow();
        int dx = (held(window, InputConstants.KEY_A) ? 1 : 0) - (held(window, InputConstants.KEY_D) ? 1 : 0);
        int dy = (held(window, InputConstants.KEY_W) ? 1 : 0) - (held(window, InputConstants.KEY_S) ? 1 : 0);
        if (dx == 0 && dy == 0) return;
        double step = PAN_SPEED * seconds / scale();
        camera.pan(dx * step, dy * step);
    }

    private static boolean held(long window, int key) { return InputConstants.isKeyDown(window, key); }

    private void tap(int mouseX, int mouseY) {
        MapPick.Hit hit = pick(mouseX, mouseY);
        if (tools.active() != null) {
            MapTools.Target chosenTarget = tools.target(land, hit);
            if (chosenTarget != null) tools.apply(land, chosenTarget);
            return;
        }
        MapPlots.Plot plot = hit == null ? null : MapPlots.at(land.plots(), hit.x() >> 4, hit.z() >> 4);
        boolean shift = Screen.hasShiftDown();
        if (plot == null) {
            if (!shift) selection.clear();
            return;
        }
        long chunk = ChunkPos.asLong(plot.chunkX(), plot.chunkZ());
        long now = Util.getMillis();
        boolean twice = chunk == lastTapChunk && now - lastTap <= DOUBLE_CLICK_MILLIS;
        lastTap = twice ? 0L : now;
        lastTapChunk = chunk;
        if (twice && plot.kind() == MapPlots.Kind.FIELD) {
            selectSameCrop(plot);
        } else if (shift) {
            selection.toggle(plot);
        } else {
            selection.only(plot);
        }
    }

    private void selectSameCrop(MapPlots.Plot plot) {
        MapPart.Section.Field field = land.field(plot.index());
        if (field != null) selectCropFields(field.crop());
    }

    private void selectCropFields(int crop) {
        List<MapPlots.Plot> same = new ArrayList<>();
        for (MapPlots.Plot each : land.plots()) {
            MapPart.Section.Field other = each.kind() == MapPlots.Kind.FIELD ? land.field(each.index()) : null;
            boolean grows = other != null
                    && (other.crop() == crop || other.crop() == FarmState.AUTO_CROP && other.main() == crop);
            if (grows) same.add(each);
        }
        if (same.isEmpty()) return;
        tools.cancel();
        selection.clear();
        selection.addAll(same);
    }

    private void selectAllFields() {
        tools.cancel();
        selection.clear();
        selection.addAll(land.plots().stream().filter(plot -> plot.kind() == MapPlots.Kind.FIELD).toList());
    }

    @Override
    protected boolean drag(int x, int y, double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != dragging) return false;
        if (Math.abs(mouseX - pressX) >= CLICK_SLOP || Math.abs(mouseY - pressY) >= CLICK_SLOP) moved = true;
        if (moved && !areaing && !boxing) camera.pan(dragX / scale(), dragY / scale());
        return true;
    }

    @Override
    protected boolean release(int x, int y, double mouseX, double mouseY, int button) {
        if (button != dragging) return false;
        dragging = NO_BUTTON;
        if (areaing && moved) {
            MapTools.planArea(land, area);
        } else if (boxing && moved && box != null) {
            List<MapPlots.Plot> inside = MapSelection.inBox(land.plots(), box[0], box[1], box[2], box[3]);
            if (!boxAdds) selection.clear();
            selection.addAll(inside);
        } else if (!moved && button == InputConstants.MOUSE_BUTTON_LEFT) {
            tap((int) mouseX, (int) mouseY);
        } else if (!moved && button == InputConstants.MOUSE_BUTTON_RIGHT && tools.active() == null) {
            MapPick.Hit hit = pick(mouseX, mouseY);
            if (hit != null && hit.known()) teleport.open(hit, (int) mouseX, (int) mouseY, width(), height());
        }
        endArea();
        endBox();
        return true;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (card.scroll(land, selection, mouseX, mouseY, delta)) return true;
        if (cede.isOpen() || !isOnMap(mouseX, mouseY) || delta == 0.0) return false;
        camera.zoom(delta > 0.0 ? 1 : -1, mouseX / scale(), (mouseY - TOP) / scale());
        return true;
    }

    @Override
    protected boolean key(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (teleport.isOpen()) teleport.close();
            else if (tools.active() != null) tools.cancel();
            else if (!selection.isEmpty()) selection.clear();
            else return false;
            return true;
        }
        if (cede.isOpen()) return false;
        if (keyCode == InputConstants.KEY_A && Screen.hasControlDown()) {
            selectAllFields();
            return true;
        }
        switch (keyCode) {
            case InputConstants.KEY_Q -> camera.rotate(-1);
            case InputConstants.KEY_E -> camera.rotate(1);
            case InputConstants.KEY_R -> camera.rotate(Screen.hasShiftDown() ? -1 : 1);
            case InputConstants.KEY_V -> press(MapControls.Control.CONTOUR);
            case InputConstants.KEY_H -> press(MapControls.Control.HOME);
            case InputConstants.KEY_W, InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D -> {}
            default -> {
                return false;
            }
        }
        return true;
    }
    //endregion

    //region Cede popup [割弃弹窗]
    @Override
    protected boolean hasOverlay() { return cede.isOpen(); }

    @Override
    protected void overlay(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        cede.draw(graphics, font(), camera, mouseX, mouseY);
        cede.tip(graphics, mouseX, mouseY);
    }

    @Override
    protected void overlayClick(int x, int y, double mouseX, double mouseY) {
        cede.click(camera, mouseX, mouseY, Screen.hasShiftDown());
    }

    @Override
    protected void closeOverlay() { cede.close(); }
    //endregion
}
