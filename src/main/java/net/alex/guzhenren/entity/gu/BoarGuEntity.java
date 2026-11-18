package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 野生豕蛊实体：在世界中自由飞行与降落歇息，不追逐玩家。
 * <ul>
 *   <li>AI 目标、飞行阶段与动画控制器均继承自 {@link RestingFlyingGuEntity}；白豕蛊[White Boar Gu]、
 *       黑豕蛊[Black Boar Gu]与花豕蛊[Flower Boar Gu]共用本类，仅捕获所得物品与贴图不同。</li>
 *   <li>瓢虫模型动画约定：歇息循环 {@code animation.idle}；起飞播放 0.8 秒 {@code animation.lift} 后转入循环
 *       {@code animation.fly}；着地播放 0.8 秒 {@code animation.land} 后转入循环 {@code animation.idle}。</li>
 * </ul>
 */

public class BoarGuEntity extends RestingFlyingGuEntity {

    public BoarGuEntity(EntityType<? extends BoarGuEntity> type, Level level, Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }
}
