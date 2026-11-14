package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
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
 * B 面板肉身页的方块人：六肢体按伤势给填充上色，外加一层固定色轮廓。
 * <ul>
 *   <li>精灵图在 {@code textures/gui/sprites/body/}，同画布同原点叠放，由 Pixel Art 会话分层导出：六张纯白平涂的肢体
 *       乘伤势色，最上面一张 {@code outline} 不染色。</li>
 *   <li>七窍不画、筋骨皮肉改造不画（Alex 定）：七窍伤势只在悬停头的提示里列，改造只在文字行显示。</li>
 *   <li>按整数倍绘制：倍数取可用宽高都放得下的最大值，上限 {@link #MAX_SCALE}；放不下一倍时不画。</li>
 *   <li>⚠ {@link #BOXES} 是画布上的肢体包围盒，悬停命中靠它；换图后须按新图核对。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @since 1.0.0
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
        lines.add(formatEntry(part.getTranslationKey(), anatomy.getInjury(part)));
        if (part == BodyPart.HEAD) {
            for (Organ organ : Organ.values()) {
                lines.add(formatEntry(organ.getTranslationKey(), anatomy.getInjury(organ)));
            }
        }
        return lines;
    }

    private static Component formatEntry(String nameKey, Injury injury) {
        MutableComponent value = Component.translatable(injury.getTranslationKey())
                .withColor(injury.isInjured() ? getTint(injury) : ModPalette.TEXT);
        return Component.translatable("guzhenren.screen.figure.entry", Component.translatable(nameKey), value);
    }

    private static int getTint(Injury injury) { return TINTS.get(injury); }

    private record Box(int x, int y, int width, int height) {}
}
