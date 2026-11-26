package net.alex.guzhenren.gameplay.immortalaperture.state;

import java.util.function.Function;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * 福地存档的一段[Land Part]：一个经营功能自己的状态，按 {@link #key} 挂进 {@link BlissfulLandData}，存档里是同名子项。
 * <ul>
 *   <li>功能包各自声明一个 {@code PART} 常量，经 {@link BlissfulLandData#part} 取状态；根存档不认识任何功能，加功能不改
 *       根存档。</li>
 *   <li>{@link #create} 收存档的置脏回调，状态的任何改动都要调它；{@link State#save} 与 {@link State#load} 只读写
 *       {@code key} 这一项，格式由各功能自定（列表、复合标签都行）。</li>
 *   <li>⚠ {@code key} 即存档里的子项名，改名等于丢档；同一份存档里两段不许同名（{@link BlissfulLandData#part} 会拒）。</li>
 * </ul>
 */

public record LandSavePart<S extends LandSavePart.State>(@NotNull String key, @NotNull Function<Runnable, S> create) {

    public interface State {

        void save(@NotNull CompoundTag tag, @NotNull String key, @NotNull StackIo stacks);

        void load(@NotNull CompoundTag tag, @NotNull String key, @NotNull StackIo stacks);
    }
}
