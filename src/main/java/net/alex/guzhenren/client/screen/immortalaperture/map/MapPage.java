package net.alex.guzhenren.client.screen.immortalaperture.map;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelPage;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapCamera;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapPick;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapSurface;
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
 * {@link MapHud}、右上视角簇 {@link MapControls}、右缘高度刻度 {@link MapHeightRail}、底部正中工具栏 {@link MapTools}（含
 * 割弃）、右下图例 {@link MapLegend}、对象卡片 {@link MapCard}、割弃弹窗 {@link CedePanel}。
 * <ul>
 *   <li>每帧：按窗口与 GUI 缩放定视口（1 地图像素 = 2 真实像素）、把视口区块矩形交给
 *       {@link LandMapClient#requestView}、让贴图判是否重画；底色、贴图与标记按维度时间乘 {@code MapTint}。</li>
 *   <li>⚠ 离开本页调 {@link LandMapClient#stopView}（服务端不看页，不退订会一直扫和发），关面板调
 *       {@link LandMapClient#clear} 并关掉贴图线程。</li>
 *   <li>操作（Alex 定）：左键或右键拖动、按住 WASD 平移（每秒 {@value #PAN_SPEED} GUI 像素）；滚轮以光标为锚缩放；Q 逆时针
 *       转、E 顺时针转（R 备用：顺时针，Shift+R 逆时针）；V 切等高线；H 回山心。本页吃掉这些键，E 不再关面板。按下与松开
 *       相距不到 {@value #CLICK_SLOP} 像素算点击：拿着工具即执行工具，否则点占地开卡片；拿着工具时右键或 Esc 退出工具，Esc
 *       也关卡片。</li>
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
    private final MapCamera camera = new MapCamera(0.0, 64.0, 0.0);
    private final MapSurface surface = new MapSurface();
    private final MapControls controls = new MapControls();
    private final MapHud hud = new MapHud();
    private final MapHeightRail rail = new MapHeightRail();
    private final MapLegend legend = new MapLegend();
    private final CedePanel cede = new CedePanel(this);
    private final MapTools tools = new MapTools();
    private final MapCard card = new MapCard();
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
    private int areaX;
    private int areaZ;
    private long areaEnd = Long.MIN_VALUE;
    private List<MapTools.Target> area = List.of();
    private long lastFrame;

    public MapPage(EarthSpiritScreen screen) { super(screen); }


    @Override
    protected Component name() { return Component.translatable(EarthSpiritScreen.UI + "tab.map"); }

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
        legend.layout(width, height);
        hud.layout(height);
        rail.layout(width, controls.bottom() + RAIL_GAP, legend.top() - RAIL_GAP);
        cede.layout(width, height);
    }

    @Override
    protected void hidden() {
        dragging = NO_BUTTON;
        endArea();
        lastFrame = 0L;
        cede.closeAll();
        tools.cancel();
        card.close();
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
        card.refresh(plots);
        target = tools.target(land, hover);
        updateArea();
        if (!area.isEmpty()) target = null;
        MapPlots.Plot highlighted = card.plot() != null ? card.plot() : tools.active() != null || hover == null
                ? null : MapPlots.at(plots, hover.x() >> 4, hover.z() >> 4);
        MapScene.draw(graphics, camera, surface, TOP, width(), height(), guiScale(), new MapScene.Layers(plots,
                highlighted, target, target == null ? 0 : tools.colour(target), area, hover));
        SpriteBatch.draw(graphics, sprites -> {
            hud.sprites(sprites, font(), land);
            controls.sprites(sprites, camera, mouseX, mouseY);
            rail.sprites(sprites, font(), surface.lowest(), surface.highest(), hover);
            legend.sprites(sprites);
            tools.sprites(sprites, mouseX, mouseY);
            tools.hint(sprites, null, font());
        });
        rail.fills(graphics, surface.lowest(), surface.highest(), hover);
        legend.fills(graphics);
        tools.fills(graphics);
        SpriteBatch.draw(graphics, sprites -> {
            card.sprites(sprites, land, mouseX, mouseY);
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
        tools.texts(graphics, font());
        tools.hint(null, graphics, font());
        card.texts(graphics, font(), land);
        teleport.texts(graphics, font());
        legend.texts(graphics, font(), camera, surface.lowest(), surface.highest(), hover);
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (controls.tip(this, graphics, mouseX, mouseY) || tools.tip(this, graphics, mouseX, mouseY)
                || hud.tip(this, graphics, land, mouseX, mouseY)
                || card.isOpen() && card.tip(this, graphics, land, mouseX, mouseY)) {
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
                && !controls.contains(mouseX, mouseY) && !tools.contains(mouseX, mouseY)
                && !hud.contains(mouseX, mouseY) && !legend.contains(mouseX, mouseY) && !card.contains(mouseX, mouseY)
                && !teleport.contains(mouseX, mouseY);
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
            if (left) card.click(land, tools, mouseX, mouseY);
            return true;
        }
        card.close();
        MapControls.Control control = controls.at(mouseX, mouseY);
        if (control != null) {
            if (left) press(control);
            return true;
        }
        MapTools.Tool tool = tools.at(mouseX, mouseY);
        if (tool != null) {
            if (left) tools.press(tool);
            return true;
        }
        if (tools.isOverCede(mouseX, mouseY)) {
            if (left) cede.open();
            return true;
        }
        if (controls.contains(mouseX, mouseY) || tools.contains(mouseX, mouseY) || hud.contains(mouseX, mouseY)
                || legend.contains(mouseX, mouseY)) {
            return true;
        }
        if (!isOnMap(mouseX, mouseY)) return false;
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && tools.active() != null) {
            tools.cancel();
            return true;
        }
        dragging = button;
        pressX = mouseX;
        pressY = mouseY;
        moved = false;
        if (left && tools.active() == MapTools.Tool.FARM) {
            MapPick.Hit start = pick(mouseX, mouseY);
            areaing = start != null;
            if (start != null) {
                areaX = start.x() >> 4;
                areaZ = start.z() >> 4;
            }
        }
        return true;
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
        if (seconds == 0.0 || cede.isOpen()) return;

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
            MapTools.Target chosen = tools.target(land, hit);
            if (chosen != null) tools.apply(land, chosen);
            return;
        }
        if (hit == null) return;
        MapPlots.Plot plot = MapPlots.at(land.plots(), hit.x() >> 4, hit.z() >> 4);
        if (plot != null) card.open(plot, mouseX, mouseY, width(), height());
    }

    @Override
    protected boolean drag(int x, int y, double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != dragging) return false;
        if (Math.abs(mouseX - pressX) >= CLICK_SLOP || Math.abs(mouseY - pressY) >= CLICK_SLOP) moved = true;
        if (moved && !areaing) camera.pan(dragX / scale(), dragY / scale());
        return true;
    }

    @Override
    protected boolean release(int x, int y, double mouseX, double mouseY, int button) {
        if (button != dragging) return false;
        dragging = NO_BUTTON;
        if (areaing && moved) MapTools.planArea(land, area);
        else if (!moved && button == InputConstants.MOUSE_BUTTON_LEFT) tap((int) mouseX, (int) mouseY);
        else if (!moved && button == InputConstants.MOUSE_BUTTON_RIGHT && tools.active() == null) {
            MapPick.Hit hit = pick(mouseX, mouseY);
            if (hit != null && hit.known()) teleport.open(hit, (int) mouseX, (int) mouseY, width(), height());
        }
        endArea();
        return true;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (card.contains(mouseX, mouseY)) {
            card.scroll(land, mouseX, mouseY, delta);
            return true;
        }
        if (cede.isOpen() || !isOnMap(mouseX, mouseY) || delta == 0.0) return false;
        camera.zoom(delta > 0.0 ? 1 : -1, mouseX / scale(), (mouseY - TOP) / scale());
        return true;
    }

    @Override
    protected boolean key(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE && (tools.active() != null || card.isOpen() || teleport.isOpen())) {
            teleport.close();
            tools.cancel();
            card.close();
            return true;
        }
        if (cede.isOpen()) return false;
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
