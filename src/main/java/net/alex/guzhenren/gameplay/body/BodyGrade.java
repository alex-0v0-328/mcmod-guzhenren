package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.core.NamedEnum;

/**
 * 筋骨皮肉一格的一档：原生，或金道、土道炼体蛊换上的档。
 * <ul>
 *   <li>每格一个闭合枚举，以自身为 {@code G} 实现本接口，占 {@code BodyData} 一格；每档带授予蛊转数，原生 0。</li>
 *   <li>同族各转连续，{@code /gzr body} 借此按族与转指名一档。</li>
 *   <li>{@link #canReplace} 是炼体蛊共问的唯一门槛：当前档是否让位于此档；各格自答，物品不自比档。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Skin
 * @see Tendon
 * @see Bone
 * @see Flesh
 * @since 1.0.0
 */

public interface BodyGrade<G extends BodyGrade<G>> extends NamedEnum {

    boolean isNative();

    int getRank();

    boolean canReplace(G worn);
}
