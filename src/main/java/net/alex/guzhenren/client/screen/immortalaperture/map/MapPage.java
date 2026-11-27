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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 「地图」页：全屏，顶栏（{@value EarthSpiritScreen#BAR_H} 像素，由 {@link EarthSpiritScreen} 画）以下整片是地图视口；
 * 地形贴图由 {@link MapSurface} 后台光栅，地图层由 {@link MapScene} 画，左下视角按钮条 {@link MapControls} 与其上的选址
 * 工具条 {@link MapTools}、右下图例 {@link MapLegend}、对象卡片 {@link MapCard}、割弃弹窗 {@link CedePanel}。
 * <ul>
 *   <li>每帧：按窗口与 GUI 缩放定视口（1 地图像素 = 2 真实像素）、把视口区块矩形交给
 *       {@link LandMapClient#requestView}、让贴图判是否重画；底色、贴图与标记按维度时间乘 {@code MapTint}。</li>
 *   <li>⚠ 离开本页调 {@link LandMapClient#stopView}（服务端不看页，不退订会一直扫和发），关面板调
 *       {@link LandMapClient#clear} 并关掉贴图线程。</li>
 *   <li>操作：左键或右键拖动平移；滚轮以光标为锚缩放；R 顺时针转、Shift+R 逆时针转；V 切地形 / 等高线；= 与 -
 *       缩放。按下与松开相距不到 {@value #CLICK_SLOP} 像素算点击：拿着工具即执行工具，否则点占地开卡片；拿着工具时右键
 *       或 Esc 退出工具，Esc 也关卡片。</li>
 *   <li>本页显示时 {@link #hidesWorld} 为真，{@code GameRendererMixin} 整帧跳过世界渲染；启动参数
 *       {@code -D}{@value #KEEP_WORLD_PROPERTY}{@code =true} 退回照常渲染，供光影或性能模组冲突时排查。</li>
 *   <li>相机对准山心（参照平面 = 山底）；秘境还没解出山心时对准玩家脚下；「山心」按钮同。</li>
 *   <li>⚠ 绘制都在 {@link #fills}（贴图与头像是立即绘制，见 {@link MapScene}），按钮与卡片精灵在同一步另起
 *       {@link SpriteBatch}。</li>
 * </ul>
 */

public class MapPage extends PanelPage {

    public static final String KEEP_WORLD_PROPERTY = "guzhenren.mapKeepsWorld";
    private static final boolean KEEPS_WORLD = Boolean.getBoolean(KEEP_WORLD_PROPERTY);
    private static final int TOP = EarthSpiritScreen.BAR_H + 1;
    private static final int NO_BUTTON = -1;
    private static final int CLICK_SLOP = 3;
    private static volatile boolean showing;
    private final MapCamera camera = new MapCamera(0.0, 64.0, 0.0);
    private final MapSurface surface = new MapSurface();
    private final MapControls controls = new MapControls();
    private final MapLegend legend = new MapLegend();
    private final CedePanel cede = new CedePanel(this);
    private final MapTools tools = new MapTools();
    private final MapCard card = new MapCard();
    private final MapToolLand land = new MapToolLand(this);
    private boolean aimed;
    private int dragging = NO_BUTTON;
    private double pressX;
    private double pressY;
    private boolean moved;
    private @Nullable MapPick.Hit hover;
    private MapTools.@Nullable Target target;

    public MapPage(EarthSpiritScreen screen) { super(screen); }

    public static boolean hidesWorld() {
        return !KEEPS_WORLD && showing && Minecraft.getInstance().screen instanceof EarthSpiritScreen;
    }

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
        controls.layout(height);
        tools.layout(controls.top());
        legend.layout(width, height);
        cede.layout(width, height);
    }

    @Override
    protected void hidden() {
        showing = false;
        dragging = NO_BUTTON;
        cede.closeAll();
        tools.cancel();
        card.close();
        LandMapClient.stopView();
    }

    @Override
    protected void closed() {
        showing = false;
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
        showing = true;
        sync();
        hover = pick(mouseX, mouseY);
        List<MapPlots.Plot> plots = land.plots();
        card.refresh(plots);
        target = tools.target(land, hover);
        MapPlots.Plot highlighted = card.plot() != null ? card.plot() : tools.active() != null || hover == null
                ? null : MapPlots.at(plots, hover.x() >> 4, hover.z() >> 4);
        MapScene.draw(graphics, camera, surface, TOP, width(), height(), guiScale(), new MapScene.Layers(plots,
                highlighted, target, target == null ? 0 : tools.colour(target), hover));
        legend.fills(graphics, surface.lowest(), surface.highest(), hover);
        SpriteBatch.draw(graphics, sprites -> {
            controls.sprites(sprites, mouseX, mouseY);
            tools.sprites(sprites, mouseX, mouseY);
            card.sprites(sprites, land, mouseX, mouseY);
        });
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        MapScene.labels(graphics, font(), camera, surface, TOP, guiScale());
        controls.texts(graphics, font(), camera);
        tools.texts(graphics, font(), land);
        card.texts(graphics, font(), land);
        legend.texts(graphics, font(), camera, surface.lowest(), surface.highest(), hover);
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (controls.tip(this, graphics, mouseX, mouseY) || tools.tip(this, graphics, mouseX, mouseY)
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
        return mouseY >= TOP && mouseY < height() && mouseX >= 0 && mouseX < width()
                && controls.at(mouseX, mouseY) == null && tools.at(mouseX, mouseY) == null
                && !legend.contains(mouseX, mouseY) && !card.contains(mouseX, mouseY);
    }
    //endregion

    //region Input [输入]
    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        boolean left = button == InputConstants.MOUSE_BUTTON_LEFT;
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
        if (legend.contains(mouseX, mouseY)) return true;
        if (!isOnMap(mouseX, mouseY)) return false;
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && tools.active() != null) {
            tools.cancel();
            return true;
        }
        dragging = button;
        pressX = mouseX;
        pressY = mouseY;
        moved = false;
        return true;
    }

    private void press(MapControls.Control control) {
        if (control == MapControls.Control.CEDE) {
            cede.open();
        } else if (control == MapControls.Control.HOME) {
            aimed = false;
            aim();
        } else {
            MapControls.turn(control, camera);
        }
    }

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
        if (moved) camera.pan(dragX / scale(), dragY / scale());
        return true;
    }

    @Override
    protected boolean release(int x, int y, double mouseX, double mouseY, int button) {
        if (button != dragging) return false;
        dragging = NO_BUTTON;
        if (!moved && button == InputConstants.MOUSE_BUTTON_LEFT) tap((int) mouseX, (int) mouseY);
        return true;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (cede.isOpen() || !isOnMap(mouseX, mouseY) || delta == 0.0) return false;
        camera.zoom(delta > 0.0 ? 1 : -1, mouseX / scale(), (mouseY - TOP) / scale());
        return true;
    }

    @Override
    protected boolean key(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE && (tools.active() != null || card.isOpen())) {
            tools.cancel();
            card.close();
            return true;
        }
        switch (keyCode) {
            case InputConstants.KEY_R -> camera.rotate(Screen.hasShiftDown() ? -1 : 1);
            case InputConstants.KEY_V -> camera.setPlan(!camera.plan());
            case InputConstants.KEY_EQUALS, InputConstants.KEY_ADD -> press(MapControls.Control.ZOOM_IN);
            case InputConstants.KEY_MINUS -> press(MapControls.Control.ZOOM_OUT);
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
