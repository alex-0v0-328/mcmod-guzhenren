package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.gameplay.body.Anatomy;
import net.alex.guzhenren.gameplay.body.BodyPart;
import net.alex.guzhenren.gameplay.body.Injury;
import net.alex.guzhenren.gameplay.body.Organ;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * B 面板肉身页签中的方块人伤势图示：六大肢体各自按伤势程度着色，外层再叠绘一张固定颜色的轮廓线。
 * <ul>
 *   <li>精灵图位于 {@code textures/gui/sprites/body/}，同画布同原点叠放并由 Pixel Art 会话分层导出：六张纯白平涂的
 *       肢体图层分别乘以各自伤势色，最上层的 {@code outline} 轮廓图保持原色不染色。</li>
 *   <li>不绘制七窍以及筋骨皮肉改造状态（由 Alex 选定）：七窍伤势仅在鼠标悬停头部时的提示中列出，肉身改造仅在右侧文字
 *       行中展示。</li>
 *   <li>悬停提示按部位只列出有伤条目（由 Alex 选定）：某肢体有伤时显示对应伤势（头部额外列出受伤的窍），「无伤」一
 *       律不展示；当所有条目均无伤时返回空列表，使 {@code renderComponentTooltip} 自动不弹出提示框，悬停无伤肢体
 *       即无反馈。</li>
 *   <li>采用整数倍率绘制：取可用宽与可用高均能容纳的最大整数倍并受 {@link #MAX_SCALE} 限制，连 1 倍都放不下时跳过绘
 *       制。</li>
 *   <li>⚠ {@link #BOXES} 硬编码画布上各肢体的碰撞包围盒，悬停命中检测完全依赖它；若后续更换体形贴图，必须同步核对这
 *       组包围盒。</li>
 * </ul>
 */

final class BodyFigure {

    private BodyFigure() {}

    static final int WIDTH = 36;
    static final int HEIGHT = 68;
    private static final int MAX_SCALE = 3;
    private static final ResourceLocation OUTLINE = sprite("outline");
    private static final Map<BodyPart, ResourceLocation> LIMBS = new EnumMap<>(BodyPart.class);
    private static final Map<BodyPart, Box> BOXES = new EnumMap<>(BodyPart.class);
    private static final Map<Injury, Integer> TINTS = new EnumMap<>(Map.of(
            Injury.NONE, 0xFFE0DCD4,
            Injury.LIGHT, 0xFFE8C547,
            Injury.MODERATE, 0xFFE8873A,
            Injury.SEVERE, 0xFFD0453A,
            Injury.CRIPPLED, 0xFF8A3A3A));

    static {
        for (BodyPart part : BodyPart.values()) LIMBS.put(part, sprite("limb_" + part.getSerializedName()));
        BOXES.put(BodyPart.HEAD, new Box(10, 1, 16, 16));
        BOXES.put(BodyPart.TORSO, new Box(10, 18, 16, 24));
        BOXES.put(BodyPart.RIGHT_ARM, new Box(1, 18, 8, 24));
        BOXES.put(BodyPart.LEFT_ARM, new Box(27, 18, 8, 24));
        BOXES.put(BodyPart.RIGHT_LEG, new Box(10, 43, 8, 24));
        BOXES.put(BodyPart.LEFT_LEG, new Box(19, 43, 8, 24));
    }

    private static ResourceLocation sprite(String name) { return Guzhenren.id("body/" + name); }

    static int getScale(int width, int height) {
        return Math.min(MAX_SCALE, Math.min(width / WIDTH, height / HEIGHT));
    }

    static void draw(SpriteBatch sprites, Anatomy anatomy, int x, int y, int scale) {
        int width = WIDTH * scale;
        int height = HEIGHT * scale;
        LIMBS.forEach((part, limb) -> sprites.blit(limb, x, y, width, height, getTint(anatomy.getInjury(part))));
        sprites.blit(OUTLINE, x, y, width, height);
    }

    static @Nullable BodyPart getHovered(double mouseX, double mouseY, int x, int y, int scale) {
        for (Map.Entry<BodyPart, Box> entry : BOXES.entrySet()) {
            Box box = entry.getValue();
            if (Widgets.isOver(mouseX, mouseY, x + box.x() * scale, y + box.y() * scale, box.width() * scale,
                    box.height() * scale)) {
                return entry.getKey();
            }
        }
        return null;
    }

    static List<Component> getTooltip(Anatomy anatomy, BodyPart part) {
        List<Component> lines = new ArrayList<>();
        addIfInjured(lines, part.getTranslationKey(), anatomy.getInjury(part));
        if (part == BodyPart.HEAD) {
            for (Organ organ : Organ.values()) addIfInjured(lines, organ.getTranslationKey(), anatomy.getInjury(organ));
        }
        return lines;
    }

    private static void addIfInjured(List<Component> lines, String nameKey, Injury injury) {
        if (!injury.isInjured()) return;

        MutableComponent value = Component.translatable(injury.getTranslationKey()).withColor(getTint(injury));
        lines.add(Component.translatable("guzhenren.screen.figure.entry", Component.translatable(nameKey), value));
    }

    private static int getTint(Injury injury) { return TINTS.get(injury); }

    private record Box(int x, int y, int width, int height) {}
}
