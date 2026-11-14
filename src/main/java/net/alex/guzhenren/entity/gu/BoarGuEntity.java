package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 野生豕蛊：自由游荡，不追玩家。
 * <ul>
 *   <li>移动目标、飞行阶段、动画控制器继承 {@link RestingFlyingGuEntity}；白豕蛊[White Boar Gu]、黑豕
 *       蛊[Black Boar Gu]、花豕蛊[Flower Boar Gu]只差捕获物与贴图。</li>
 *   <li>瓢虫模型动画：歇息 {@code animation.idle}；{@code animation.lift} 0.8 秒后循环 {@code animation.fly}；
 *       {@code animation.land} 0.8 秒后循环 {@code animation.idle}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class BoarGuEntity extends RestingFlyingGuEntity {

    public BoarGuEntity(EntityType<? extends BoarGuEntity> type, Level level, Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }
}
