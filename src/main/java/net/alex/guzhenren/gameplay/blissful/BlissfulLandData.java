package net.alex.guzhenren.gameplay.blissful;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.core.ModSavedData;
import net.alex.guzhenren.gameplay.blissful.farm.FarmState;
import net.alex.guzhenren.gameplay.blissful.gu.GuState;
import net.alex.guzhenren.gameplay.blissful.herd.HerdState;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainState;
import net.alex.guzhenren.gameplay.blissful.storage.BlissfulStorage;
import net.alex.guzhenren.gameplay.blissful.territory.Territory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 一处福地[Blissful Land]的经营存档，存在主世界存储的 {@code guzhenren_<福地 id>.dat}（不随福地维度存，没装 Gu World 时
 * 存档也不丢，GameTest 也能直接构造）。
 * <ul>
 *   <li>本类只管福地共用的部分：主人、白名单、疆域、当前地灵 UUID；各经营模块的状态各占一段（{@link #storage}、
 *       {@link #herd}、{@link #farm}、{@link #mountain}、{@link #gu}），存档里也各是一个子标签。</li>
 *   <li>主人与白名单：{@link #owner} 为空即无主；白名单记 UUID 与名字（名字只供离线显示），同一份名单既免荡魂山震荡，也
 *       免「驱逐外人」。失主由 {@code MemberService#lose} 清白名单、关托管，库藏、疆域、标准田、每种保持数留给下一任。
 *       </li>
 *   <li>⚠ 所有改动走 setter 或 {@link #changed}，各模块状态经构造时传入的回调置脏；集合只读暴露。</li>
 * </ul>
 */

public final class BlissfulLandData extends ModSavedData {

    private final BlissfulLand land;
    private @Nullable UUID owner;
    private String ownerName = "";
    private final Map<UUID, String> whitelist = new LinkedHashMap<>();
    private final BlissfulStorage storage = new BlissfulStorage(this::changed);
    private final HerdState herd = new HerdState(this::changed);
    private final GuState gu = new GuState(this::changed);
    private final FarmState farm = new FarmState(this::changed);
    private final MountainState mountain = new MountainState(this::changed);
    private Territory territory;
    private @Nullable UUID spirit;

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

    public @NotNull BlissfulStorage storage() { return storage; }

    public @NotNull HerdState herd() { return herd; }

    public @NotNull GuState gu() { return gu; }

    public @NotNull FarmState farm() { return farm; }

    public @NotNull MountainState mountain() { return mountain; }

    //region Owner [主人与白名单]
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

    //region Land [疆域与地灵]
    public @NotNull Territory territory() { return territory; }

    public void setTerritory(@NotNull Territory territory) {
        this.territory = territory;
        setDirty();
    }

    public @Nullable UUID spirit() { return spirit; }

    public void setSpirit(@Nullable UUID spirit) {
        this.spirit = spirit;
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
        tag.put("herd", herd.save());
        tag.put("gu", gu.save());
        tag.put("farm", farm.save());
        tag.put("mountain", mountain.save());
        CompoundTag bounds = new CompoundTag();
        territory.save(bounds);
        tag.put("territory", bounds);
        if (spirit != null) tag.putUUID("spirit", spirit);
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
        data.herd.load(tag.getCompound("herd"));
        data.gu.load(tag.getCompound("gu"));
        data.farm.load(tag.getCompound("farm"));
        data.mountain.load(tag.getCompound("mountain"));
        data.territory = Territory.load(tag.getCompound("territory"), land.half(), land.core());
        if (tag.hasUUID("spirit")) data.spirit = tag.getUUID("spirit");
        data.setDirty(false);
        return data;
    }
}
