package net.alex.guzhenren.gameplay.blissful;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.alex.guzhenren.core.ModSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 一处福地[Blissful Land]的经营状态，存在主世界存储的 {@code guzhenren_<福地 id>.dat}（不随福地维度存，没装 Gu World 时
 * 存档也不丢，GameTest 也能直接构造）。
 * <ul>
 *   <li>主人与白名单：{@link #owner} 为空即无主；白名单记 UUID 与名字（名字只供离线显示），同一份名单既免荡魂山震荡，也
 *       免「驱逐外人」。失主由 {@link BlissfulLandService#lose} 清白名单、关托管，库藏、疆域、标准田、每种保持数留给下
 *       一任。</li>
 *   <li>托管：种植一个开关；养殖按种类开关（实体类型 ID 集合），另有每种保持数（实体类型 ID → 只数，0 = 不管）。保持数
 *       与托管开关分开存，关掉托管不丢保持数，一键宰杀也照保持数算。</li>
 *   <li>冷却按游戏刻记「何时可再用」，读档后仍有效；{@link #spirit} 记当前地灵 UUID，供补生成判断。</li>
 *   <li>标准田记水坑方块坐标（即耕地那一层的田心），顺序即开垦顺序。</li>
 *   <li>⚠ 所有改动走本类的 setter 或 {@link #changed}，保证置脏；集合只读暴露。</li>
 * </ul>
 */

public final class BlissfulLandData extends ModSavedData {

    private final BlissfulLand land;
    private @Nullable UUID owner;
    private String ownerName = "";
    private final Map<UUID, String> whitelist = new LinkedHashMap<>();
    private final BlissfulStorage storage = new BlissfulStorage(this::changed);
    private final Set<ResourceLocation> herdHosted = new LinkedHashSet<>();
    private boolean farmHosting;
    private final Map<ResourceLocation, Integer> herdKeep = new LinkedHashMap<>();
    private Territory territory;
    private long moveReadyAt;
    private long reclaimReadyAt;
    private @Nullable UUID spirit;
    private final LongList fields = new LongArrayList();

    private BlissfulLandData(BlissfulLand land) {
        this.land = land;
        this.territory = land.wholeTerritory();
    }

    public static @NotNull BlissfulLandData create(@NotNull BlissfulLand land) { return new BlissfulLandData(land); }

    public static @NotNull BlissfulLandData get(@NotNull MinecraftServer server, @NotNull BlissfulLand land) {
        return ModSavedData.get(server.overworld(), "guzhenren_" + land.id(), () -> new BlissfulLandData(land),
                (tag, stacks) -> load(land, tag, stacks));
    }

    public @NotNull BlissfulLand land() { return land; }

    public void changed() { setDirty(); }

    //region Owner [主人]
    public @Nullable UUID owner() { return owner; }

    public @NotNull String ownerName() { return ownerName; }

    public boolean hasOwner() { return owner != null; }

    public boolean isOwner(@NotNull UUID player) { return player.equals(owner); }

    public void setOwner(@Nullable UUID owner, @NotNull String name) {
        this.owner = owner;
        this.ownerName = owner == null ? "" : name;
        setDirty();
    }

    public @NotNull Map<UUID, String> whitelist() { return Collections.unmodifiableMap(whitelist); }

    public boolean isWhitelisted(@NotNull UUID player) { return whitelist.containsKey(player); }

    public void addWhitelist(@NotNull UUID player, @NotNull String name) {
        if (name.equals(whitelist.put(player, name))) return;
        setDirty();
    }

    public void removeWhitelist(@NotNull UUID player) {
        if (whitelist.remove(player) != null) setDirty();
    }

    public void clearWhitelist() {
        if (whitelist.isEmpty()) return;
        whitelist.clear();
        setDirty();
    }
    //endregion

    //region Hosting [托管]
    public @NotNull BlissfulStorage storage() { return storage; }

    public boolean herdHosting() { return !herdHosted.isEmpty(); }

    public boolean isHosted(@NotNull ResourceLocation type) { return herdHosted.contains(type); }

    public @NotNull Set<ResourceLocation> herdHosted() { return Collections.unmodifiableSet(herdHosted); }

    public boolean farmHosting() { return farmHosting; }

    public void setHosted(@NotNull ResourceLocation type, boolean on) {
        if (on ? herdHosted.add(type) : herdHosted.remove(type)) setDirty();
    }

    public void clearHerdHosting() {
        if (herdHosted.isEmpty()) return;
        herdHosted.clear();
        setDirty();
    }

    public void setFarmHosting(boolean on) {
        if (farmHosting == on) return;
        farmHosting = on;
        setDirty();
    }

    public int keep(@NotNull ResourceLocation type) { return herdKeep.getOrDefault(type, 0); }

    public @NotNull Map<ResourceLocation, Integer> herdKeep() { return Collections.unmodifiableMap(herdKeep); }

    public void setKeep(@NotNull ResourceLocation type, int count) {
        int clamped = Math.max(0, count);
        Integer before = clamped == 0 ? herdKeep.remove(type) : herdKeep.put(type, clamped);
        if (before == null || before != clamped) setDirty();
    }
    //endregion

    //region Land [疆域与田]
    public @NotNull Territory territory() { return territory; }

    public void setTerritory(@NotNull Territory territory) {
        this.territory = territory;
        setDirty();
    }

    public long moveReadyAt() { return moveReadyAt; }

    public void setMoveReadyAt(long gameTime) {
        moveReadyAt = gameTime;
        setDirty();
    }

    public long reclaimReadyAt() { return reclaimReadyAt; }

    public void setReclaimReadyAt(long gameTime) {
        reclaimReadyAt = gameTime;
        setDirty();
    }

    public @Nullable UUID spirit() { return spirit; }

    public void setSpirit(@Nullable UUID spirit) {
        this.spirit = spirit;
        setDirty();
    }

    public @NotNull LongList fields() { return LongList.of(fields.toLongArray()); }

    public boolean hasField(@NotNull BlockPos center) { return fields.contains(center.asLong()); }

    public void addField(@NotNull BlockPos center) {
        fields.add(center.asLong());
        setDirty();
    }
    //endregion

    @Override
    protected void write(@NotNull CompoundTag tag, @NotNull StackIo stacks) {
        if (owner != null) {
            tag.putUUID("owner", owner);
            tag.putString("owner_name", ownerName);
        }
        ListTag names = new ListTag();
        for (Map.Entry<UUID, String> entry : whitelist.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("id", entry.getKey());
            item.putString("name", entry.getValue());
            names.add(item);
        }
        tag.put("whitelist", names);
        storage.save(tag, "storage", stacks);
        ListTag hosted = new ListTag();
        for (ResourceLocation type : herdHosted) hosted.add(StringTag.valueOf(type.toString()));
        tag.put("herd_hosted", hosted);
        tag.putBoolean("farm_hosting", farmHosting);
        CompoundTag keep = new CompoundTag();
        herdKeep.forEach((type, count) -> keep.putInt(type.toString(), count));
        tag.put("herd_keep", keep);
        CompoundTag land = new CompoundTag();
        territory.save(land);
        tag.put("territory", land);
        tag.putLong("move_ready_at", moveReadyAt);
        tag.putLong("reclaim_ready_at", reclaimReadyAt);
        if (spirit != null) tag.putUUID("spirit", spirit);
        tag.put("fields", new LongArrayTag(fields.toLongArray()));
    }

    private static BlissfulLandData load(BlissfulLand land, CompoundTag tag, StackIo stacks) {
        BlissfulLandData data = new BlissfulLandData(land);
        if (tag.hasUUID("owner")) {
            data.owner = tag.getUUID("owner");
            data.ownerName = tag.getString("owner_name");
        }
        for (Tag element : tag.getList("whitelist", Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) element;
            data.whitelist.put(item.getUUID("id"), item.getString("name"));
        }
        data.storage.load(tag, "storage", stacks);
        for (Tag element : tag.getList("herd_hosted", Tag.TAG_STRING)) {
            ResourceLocation type = ResourceLocation.tryParse(element.getAsString());
            if (type != null) data.herdHosted.add(type);
        }
        data.farmHosting = tag.getBoolean("farm_hosting");
        CompoundTag keep = tag.getCompound("herd_keep");
        for (String key : keep.getAllKeys()) {
            ResourceLocation type = ResourceLocation.tryParse(key);
            if (type != null && keep.getInt(key) > 0) data.herdKeep.put(type, keep.getInt(key));
        }
        data.territory = Territory.load(tag.getCompound("territory"), land.half(), land.core());
        data.moveReadyAt = tag.getLong("move_ready_at");
        data.reclaimReadyAt = tag.getLong("reclaim_ready_at");
        if (tag.hasUUID("spirit")) data.spirit = tag.getUUID("spirit");
        for (long field : tag.getLongArray("fields")) data.fields.add(field);
        data.setDirty(false);
        return data;
    }
}
