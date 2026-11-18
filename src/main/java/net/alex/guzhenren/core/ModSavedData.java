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
 * 模组维度级持久化存档数据（{@link SavedData}）的抽象基类。
 * <ul>
 *   <li>子类只需实现 {@link #write} 并通过 {@link #get} 按需创建或读取实例；高频访问的热路径可通过预构建的
 *       {@link SavedData.Factory} 调用重载版 {@link #get(ServerLevel, String, SavedData.Factory)} 以避免重复分配工厂对
 *       象。</li>
 *   <li>通过 {@link StackIo} 将当前 {@link HolderLookup.Provider} 注册表上下文绑定到单个非空 {@link ItemStack} 的 NBT
 *       读写中，屏蔽底层序列化样板代码。</li>
 * </ul>
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
