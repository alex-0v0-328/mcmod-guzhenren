package net.alex.guzhenren.core;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

/**
 * 模组维度存档数据的基类。
 * <ul>
 *   <li>子类只覆写 {@link #write}，经 {@link #get} 取用并交出读回函数；热路径改传只建一次的
 *       {@link SavedData.Factory}。</li>
 *   <li>物品堆叠经两侧共享的 {@link StackIo} 在存档标签内读写单个非空堆叠，带注册表上下文。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public abstract class ModSavedData extends SavedData {

    public interface StackIo {

        @NotNull Tag save(@NotNull ItemStack stack);

        @NotNull ItemStack load(@NotNull CompoundTag tag);
    }

    protected abstract void write(@NotNull CompoundTag tag, @NotNull StackIo stacks);

    @Override
    public final @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        write(tag, stackIo(registries));
        return tag;
    }

    public static <T extends ModSavedData> @NotNull T get(@NotNull ServerLevel level, @NotNull String name,
            @NotNull Supplier<T> create, @NotNull BiFunction<CompoundTag, StackIo, T> read) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(create, (tag, registries) -> read.apply(tag, stackIo(registries))), name);
    }

    public static <T extends ModSavedData> @NotNull T get(@NotNull ServerLevel level, @NotNull String name,
            @NotNull SavedData.Factory<T> factory) {
        return level.getDataStorage().computeIfAbsent(factory, name);
    }

    private static StackIo stackIo(HolderLookup.Provider registries) {
        return new StackIo() {
            @Override
            public @NotNull Tag save(@NotNull ItemStack stack) { return stack.save(registries); }

            @Override
            public @NotNull ItemStack load(@NotNull CompoundTag tag) {
                return ItemStack.parseOptional(registries, tag);
            }
        };
    }
}
