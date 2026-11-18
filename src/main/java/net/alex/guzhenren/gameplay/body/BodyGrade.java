package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.core.NamedEnum;

/**
 * 筋、骨、皮、肉单部位炼体档位的公共接口：涵盖原生状态或金道、土道炼体蛊改造后的品阶。
 * <ul>
 *   <li>每个部位对应一个实现 {@code BodyGrade<G>} 的闭合枚举（{@link Skin}、{@link Tendon}、{@link Bone}、
 *       {@link Flesh}），在 {@link BodyData} 中各占一槽；每档携带对应蛊虫转数（原生为 {@code 0}），同族各转按序排列供
 *       {@code /gzr body} 指令按族与转数寻址。</li>
 *   <li>{@link #canReplace} 是炼体蛊起手校验的唯一替换门槛：由各部位枚举自行决定当前档位能否被新档覆盖，蛊虫物品侧不直
 *       接比较档位高低。</li>
 * </ul>
 */

public interface BodyGrade<G extends BodyGrade<G>> extends NamedEnum {

    boolean isNative();

    int getRank();

    boolean canReplace(G worn);
}
