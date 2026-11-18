package net.alex.guzhenren.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

/**
 * 模组内部事件发布入口与实体破坏地形的统一权限门禁。
 * <ul>
 *   <li>{@link #post} 将模组自定义事件派发至 {@link NeoForge#EVENT_BUS}，供底层玩法服务向高层的进度、联动或属性模块通知
 *       状态变更（如空窍、体质、力道变化或本命蛊损毁）。</li>
 *   <li>{@link #canGrief} 封装 {@link EventHooks#canEntityGrief}，作为空窍自爆弹坑、山如故[Mountain As Before Gu]
 *       与江如故[River As Before Gu]等地形改动的唯一门禁，自动遵循原版 {@code mobGriefing} 游戏规则与领地保护模组的实体
 *       级裁定（实体伤害结算不受此门禁限制）。</li>
 *   <li>⚠ 所有玩法服务必须统一通过 {@link #post} 发布事件、通过 {@link #canGrief} 检查地形修改权限，禁止绕过本类直接访
 *       问底层总线或直接破坏方块。</li>
 * </ul>
 */

public final class ModEvents {

    private ModEvents() {}

    public static void post(@NotNull Event event) { NeoForge.EVENT_BUS.post(event); }

    public static boolean canGrief(@NotNull Level level, @NotNull Entity entity) {
        return EventHooks.canEntityGrief(level, entity);
    }
}
