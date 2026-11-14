package net.alex.guzhenren.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

/**
 * 模组事件发布入口与地形改动门槛。
 * <ul>
 *   <li>{@link #post} 发到游戏事件总线，下层借此告知上层变化（空窍[Aperture]、体质、力道、本命蛊丢失）。</li>
 *   <li>{@link #canGrief}：唯一地形门槛，管空窍压力爆炸弹坑、山如故[Mountain As Before Gu]、
 *       江如故[River As Before Gu]；经加载器破坏事件跟随原版 {@code mobGriefing}，服务器可用
 *       {@code /gamerule mobGriefing false} 关闭，保护模组可逐实体裁定；伤害不问它。</li>
 *   <li>⚠ 服务只经 {@link #post} 发布、经 {@link #canGrief} 询问，地形改动才都过同一门槛。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModEvents {

    private ModEvents() {}

    public static void post(@NotNull Event event) { NeoForge.EVENT_BUS.post(event); }

    public static boolean canGrief(@NotNull Level level, @NotNull Entity entity) {
        return EventHooks.canEntityGrief(level, entity);
    }
}
