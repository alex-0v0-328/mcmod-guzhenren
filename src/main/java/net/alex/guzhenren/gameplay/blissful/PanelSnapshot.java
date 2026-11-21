package net.alex.guzhenren.gameplay.blissful;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板一次刷新的全部显示数据，服务端算好后随开面板与每秒刷新下发；客户端只显示，操作仍以服务端复判为准。
 * <ul>
 *   <li>行列表的顺序就是界面行序；{@link Stored} 的下标是库藏取物操作的参数，只在本份快照内有效。</li>
 *   <li>冷却按秒（向上取整），0 = 可用；{@link #notice} 是上一次操作的结果提示，空组件 = 无。</li>
 *   <li>⚠ 改字段或编解码顺序时同步递增 {@code ModPayloads.VERSION}。</li>
 * </ul>
 */

public record PanelSnapshot(@NotNull String landNameKey, long acres, int half, int core, int north, int east,
                            int south, int west, int gutsStones, int gutsCap, int animals, int cropsMature,
                            int cropsGrowing, @NotNull List<Person> players, @NotNull List<Person> whitelist,
                            @NotNull List<Herd> herds, @NotNull List<Crop> crops,
                            boolean farmHosting, int moveCooldown, int reclaimCooldown, int fields,
                            int fieldCapacity, @NotNull List<Stored> storage, @NotNull Component notice) {

    public record Person(@NotNull UUID id, @NotNull String name, boolean whitelisted, boolean owner) {}

    public record Herd(@NotNull ResourceLocation type, int count, int keep, boolean hosted) {}

    public record Crop(@NotNull ResourceLocation block, int mature, int growing) {}

    public record Stored(@NotNull ItemStack key, long count) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, PanelSnapshot> STREAM_CODEC =
            StreamCodec.of(PanelSnapshot::encode, PanelSnapshot::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, PanelSnapshot snapshot) {
        buffer.writeUtf(snapshot.landNameKey);
        buffer.writeVarLong(snapshot.acres);
        for (int value : new int[] {snapshot.half, snapshot.core, snapshot.north, snapshot.east, snapshot.south,
                snapshot.west, snapshot.gutsStones, snapshot.gutsCap, snapshot.animals, snapshot.cropsMature,
                snapshot.cropsGrowing, snapshot.moveCooldown, snapshot.reclaimCooldown, snapshot.fields,
                snapshot.fieldCapacity}) {
            buffer.writeVarInt(value);
        }
        buffer.writeBoolean(snapshot.farmHosting);
        writeList(buffer, snapshot.players, PanelSnapshot::writePerson);
        writeList(buffer, snapshot.whitelist, PanelSnapshot::writePerson);
        writeList(buffer, snapshot.herds, (out, herd) -> {
            out.writeResourceLocation(herd.type);
            out.writeVarInt(herd.count);
            out.writeVarInt(herd.keep);
            out.writeBoolean(herd.hosted);
        });
        writeList(buffer, snapshot.crops, (out, crop) -> {
            out.writeResourceLocation(crop.block);
            out.writeVarInt(crop.mature);
            out.writeVarInt(crop.growing);
        });
        writeList(buffer, snapshot.storage, (out, stored) -> {
            ItemStack.STREAM_CODEC.encode(out, stored.key);
            out.writeVarLong(stored.count);
        });
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, snapshot.notice);
    }

    private static PanelSnapshot decode(RegistryFriendlyByteBuf buffer) {
        String landNameKey = buffer.readUtf();
        long acres = buffer.readVarLong();
        int[] values = new int[15];
        for (int i = 0; i < values.length; i++) values[i] = buffer.readVarInt();
        boolean farmHosting = buffer.readBoolean();
        List<Person> players = readList(buffer, PanelSnapshot::readPerson);
        List<Person> whitelist = readList(buffer, PanelSnapshot::readPerson);
        List<Herd> herds = readList(buffer, in -> new Herd(in.readResourceLocation(), in.readVarInt(),
                in.readVarInt(), in.readBoolean()));
        List<Crop> crops = readList(buffer, in -> new Crop(in.readResourceLocation(), in.readVarInt(),
                in.readVarInt()));
        List<Stored> storage = readList(buffer, in -> new Stored(ItemStack.STREAM_CODEC.decode(in),
                in.readVarLong()));
        Component notice = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer);
        return new PanelSnapshot(landNameKey, acres, values[0], values[1], values[2], values[3], values[4], values[5],
                values[6], values[7], values[8], values[9], values[10], players, whitelist, herds, crops,
                farmHosting, values[11], values[12], values[13], values[14], storage, notice);
    }

    private static void writePerson(RegistryFriendlyByteBuf buffer, Person person) {
        UUIDUtil.STREAM_CODEC.encode(buffer, person.id);
        buffer.writeUtf(person.name);
        buffer.writeBoolean(person.whitelisted);
        buffer.writeBoolean(person.owner);
    }

    private static Person readPerson(RegistryFriendlyByteBuf buffer) {
        return new Person(UUIDUtil.STREAM_CODEC.decode(buffer), buffer.readUtf(), buffer.readBoolean(),
                buffer.readBoolean());
    }

    private static <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> list,
                                      BiConsumer<RegistryFriendlyByteBuf, T> writer) {
        buffer.writeVarInt(list.size());
        for (T item : list) writer.accept(buffer, item);
    }

    private static <T> List<T> readList(RegistryFriendlyByteBuf buffer, Function<RegistryFriendlyByteBuf, T> reader) {
        int size = buffer.readVarInt();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(reader.apply(buffer));
        return list;
    }
}
