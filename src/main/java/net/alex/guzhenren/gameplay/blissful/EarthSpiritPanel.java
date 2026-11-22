package net.alex.guzhenren.gameplay.blissful;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.secretrealm.GutsStones;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵经营面板的服务端：打开、算快照、执行 {@link PanelAction}、每秒刷新。
 * <ul>
 *   <li>{@link #open}：主人打开面板；无主时别人只收到认主条件，有主时只收到主人名字。</li>
 *   <li>{@link #handle} 先复判：玩家开着的是地灵面板、面板仍有效（地灵活着、距离、仍是主人），再分派给各页的
 *       {@code *Work}；结果写进下一份快照的提示，立即下发一次。</li>
 *   <li>{@link #snapshot} 每次现算：清点动物、作物要扫已加载实体与标准田，只在有人开着面板时每秒算一次。</li>
 *   <li>⚠ 库藏下标来自客户端快照，可能已过时：取物同时带物品 ID，下标处不是这种物品就改取第一条同 ID 的条目，没有就不
 *       取。取进背包前先算背包还放得下多少（{@code room}），只取这么多：创造模式背包满时 {@code Inventory#add} 会把数量
 *       吞成0 并报成功。</li>
 * </ul>
 */

public final class EarthSpiritPanel {

    private EarthSpiritPanel() {}

    public static final String MSG_UNOWNED = "guzhenren.blissful_land.spirit.unowned";
    public static final String MSG_OWNED = "guzhenren.blissful_land.spirit.owned";
    private static final String PANEL = "guzhenren.blissful_land.panel.";

    //region Open [打开]
    public static void open(@NotNull ServerPlayer player, @NotNull EarthSpiritEntity spirit) {
        BlissfulLandData data = BlissfulLandData.get(player.server, spirit.getLand());
        if (!data.hasOwner()) {
            player.sendSystemMessage(Component.translatable(MSG_UNOWNED, BlissfulLand.SUMMIT_RADIUS));
            return;
        }
        if (!data.isOwner(player.getUUID())) {
            player.sendSystemMessage(Component.translatable(MSG_OWNED, data.ownerName()));
            return;
        }
        PanelSnapshot snapshot = snapshot((ServerLevel) spirit.level(), data, Component.empty());
        player.openMenu(new SimpleMenuProvider((id, inventory, who) -> new EarthSpiritMenu(id, inventory, spirit,
                        snapshot), spirit.getDisplayName()),
                buffer -> PanelSnapshot.STREAM_CODEC.encode(buffer, snapshot));
    }

    public static void refresh(@NotNull ServerPlayer player, @NotNull Component notice) {
        if (!(player.containerMenu instanceof EarthSpiritMenu menu) || menu.getSpirit() == null) return;

        EarthSpiritEntity spirit = menu.getSpirit();
        BlissfulLandData data = BlissfulLandData.get(player.server, spirit.getLand());
        ModEvents.post(new EarthSpiritPanelEvent(player, snapshot((ServerLevel) spirit.level(), data, notice)));
    }
    //endregion

    //region Snapshot [快照]
    public static @NotNull PanelSnapshot snapshot(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                                  @NotNull Component notice) {
        Territory territory = data.territory();
        List<PanelSnapshot.Person> players = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            players.add(new PanelSnapshot.Person(id, player.getGameProfile().getName(), data.isWhitelisted(id),
                    data.isOwner(id)));
        }
        List<PanelSnapshot.Person> whitelist = new ArrayList<>();
        data.whitelist().forEach((id, name) -> whitelist.add(new PanelSnapshot.Person(id, name, true,
                data.isOwner(id))));

        Map<ResourceLocation, List<Animal>> herds = HerdWork.census(level);
        Map<ResourceLocation, int[]> rows = new TreeMap<>(Comparator.comparing(ResourceLocation::toString));
        int animals = 0;
        for (Map.Entry<ResourceLocation, List<Animal>> herd : herds.entrySet()) {
            rows.put(herd.getKey(), new int[] {herd.getValue().size(), data.keep(herd.getKey())});
            animals += herd.getValue().size();
        }
        data.herdKeep().forEach((type, keep) -> rows.putIfAbsent(type, new int[] {0, keep}));
        data.herdHosted().forEach(type -> rows.putIfAbsent(type, new int[] {0, data.keep(type)}));
        List<PanelSnapshot.Herd> herdRows = new ArrayList<>();
        rows.forEach((type, row) -> herdRows.add(new PanelSnapshot.Herd(type, row[0], row[1], data.isHosted(type))));

        List<PanelSnapshot.Crop> crops = new ArrayList<>();
        int mature = 0;
        int growing = 0;
        for (Map.Entry<Block, FarmWork.Count> crop : FarmWork.census(level, data).entrySet()) {
            crops.add(new PanelSnapshot.Crop(BuiltInRegistries.BLOCK.getKey(crop.getKey()), crop.getValue().mature(),
                    crop.getValue().growing()));
            mature += crop.getValue().mature();
            growing += crop.getValue().growing();
        }
        List<PanelSnapshot.Stored> storage = new ArrayList<>();
        for (BlissfulStorage.Entry entry : data.storage().entries()) {
            storage.add(new PanelSnapshot.Stored(entry.key().copy(), entry.count()));
        }
        return new PanelSnapshot(data.land().nameKey(), territory.acres(), territory.half(), territory.core(),
                territory.north(), territory.east(), territory.south(), territory.west(),
                GutsStones.get(level).total(), SoulsunderMountain.GUTS_STONE_CAP, animals, mature, growing, players,
                whitelist, herdRows, crops, data.farmHosting(),
                seconds(MountainWork.moveCooldown(level, data)), seconds(FarmWork.reclaimCooldown(level, data)),
                data.fields().size(), FarmWork.capacity(data.land()), storage, notice);
    }

    private static int seconds(long ticks) { return (int) ((ticks + Ticks.SECOND - 1) / Ticks.SECOND); }
    //endregion

    //region Actions [操作]
    public static void handle(@NotNull ServerPlayer player, @NotNull PanelAction action, int value,
                              @NotNull String text) {
        if (!(player.containerMenu instanceof EarthSpiritMenu menu) || menu.getSpirit() == null
                || !menu.stillValid(player)) {
            return;
        }
        EarthSpiritEntity spirit = menu.getSpirit();
        ServerLevel level = (ServerLevel) spirit.level();
        BlissfulLandData data = BlissfulLandData.get(player.server, spirit.getLand());
        Component notice = perform(player, menu, level, data, action, value, text);
        refresh(player, notice);
    }

    private static Component perform(ServerPlayer player, EarthSpiritMenu menu, ServerLevel level,
                                     BlissfulLandData data, PanelAction action, int value, String text) {
        return switch (action) {
            case EXPEL_OUTSIDERS -> notice("expelled_outsiders", BlissfulLandService.expelOutsiders(level, data));
            case EXPEL -> {
                UUID target = parseUuid(text);
                yield target != null && BlissfulLandService.expel(level, data, target)
                        ? notice("expelled") : notice("expel_failed");
            }
            case WHITELIST_ADD -> BlissfulLandService.addWhitelist(player.server, data, text)
                    ? notice("whitelist_added", text) : notice("player_not_found", text);
            case WHITELIST_SET -> {
                UUID target = parseUuid(text);
                yield target != null && BlissfulLandService.setWhitelisted(player.server, data, target, value != 0)
                        ? Component.empty() : notice("player_not_found", text);
            }
            case MOVE_MOBS -> moveMobs(level, data);
            case DRIVE_GUTS_GU -> driveGutsGu(player, level, data);
            case BREED -> {
                HerdWork.Breed bred = HerdWork.breed(HerdWork.census(level), data.storage(), HerdWork.LIMIT, null);
                yield notice("bred", bred.animals(), bred.food());
            }
            case SLAUGHTER -> notice("slaughtered", HerdWork.slaughter(level, HerdWork.census(level),
                    data.herdKeep(), data.storage(), player, HerdWork.LIMIT));
            case HERD_HOSTING -> {
                ResourceLocation type = ResourceLocation.tryParse(text);
                if (type != null && BuiltInRegistries.ENTITY_TYPE.containsKey(type)) data.setHosted(type, value != 0);
                yield Component.empty();
            }
            case HERD_KEEP -> {
                ResourceLocation type = ResourceLocation.tryParse(text);
                if (type != null && BuiltInRegistries.ENTITY_TYPE.containsKey(type)) data.setKeep(type, value);
                yield Component.empty();
            }
            case HARVEST -> notice("harvested", FarmWork.harvest(level, data, Integer.MAX_VALUE));
            case FARM_HOSTING -> {
                data.setFarmHosting(value != 0);
                yield Component.empty();
            }
            case RECLAIM -> reclaim(player, level, data);
            case CEDE -> cede(level, data, text, value);
            case TAKE_STACK -> take(player, menu, data, value, text, false);
            case TAKE_ALL -> take(player, menu, data, value, text, true);
            case DEPOSIT_CARRIED -> {
                ItemStack carried = menu.getCarried();
                if (data.storage().insert(carried)) menu.setCarried(ItemStack.EMPTY);
                yield Component.empty();
            }
        };
    }

    private static Component moveMobs(ServerLevel level, BlissfulLandData data) {
        long cooldown = MountainWork.moveCooldown(level, data);
        if (cooldown > 0) return notice("cooldown", seconds(cooldown));

        RealmSite site = SecretRealms.siteOf(level.getServer(), data.land().realm());
        if (site == null) return notice("no_mountain");
        return notice("moved", MountainWork.moveMobsUp(level, site, data, level.getRandom()));
    }

    private static Component driveGutsGu(ServerPlayer player, ServerLevel level, BlissfulLandData data) {
        RealmSite site = SecretRealms.siteOf(level.getServer(), data.land().realm());
        if (site == null) return notice("no_mountain");

        return MountainWork.driveGutsGu(level, site, player) == null ? notice("outside_mountain") : Component.empty();
    }

    private static Component reclaim(ServerPlayer player, ServerLevel level, BlissfulLandData data) {
        long cooldown = FarmWork.reclaimCooldown(level, data);
        if (cooldown > 0) return notice("cooldown", seconds(cooldown));

        if (data.fields().size() >= FarmWork.capacity(data.land())) return notice("farm_full");
        BlockPos slot = FarmWork.nextSlot(level, data.land(), data);
        if (slot == null) return notice("farm_no_dry");
        return FarmWork.reclaim(level, data, slot, player) == null
                ? notice("grief_denied") : notice("reclaimed", data.fields().size());
    }

    private static Component cede(ServerLevel level, BlissfulLandData data, String text, int width) {
        Territory.Side side = parseSide(text);
        if (side == null || !TerritoryWork.cede(level, data, side, width)) return notice("cede_refused");
        return notice("ceded", Component.translatable("guzhenren.blissful_land.side." + side.name().toLowerCase()),
                width, data.territory().acres());
    }

    private static Component take(ServerPlayer player, EarthSpiritMenu menu, BlissfulLandData data, int index,
                                  String itemId, boolean all) {
        int at = resolve(data.storage(), index, itemId);
        if (at < 0) return Component.empty();

        ItemStack key = data.storage().entries().get(at).key().copy();
        if (!all && menu.getCarried().isEmpty()) {
            menu.setCarried(data.storage().extract(at, key.getMaxStackSize()));
            return Component.empty();
        }
        int room = room(player.getInventory(), key);
        int wanted = all ? room : Math.min(room, key.getMaxStackSize());
        while (wanted > 0) {
            int next = indexOf(data.storage(), key);
            if (next < 0) break;
            ItemStack stack = data.storage().extract(next, Math.min(wanted, key.getMaxStackSize()));
            wanted -= stack.getCount();
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                data.storage().insert(stack);
                break;
            }
        }
        return Component.empty();
    }

    private static int resolve(BlissfulStorage storage, int index, String itemId) {
        List<BlissfulStorage.Entry> entries = storage.entries();
        if (index >= 0 && index < entries.size() && idOf(entries.get(index).key()).equals(itemId)) return index;
        for (int i = 0; i < entries.size(); i++) {
            if (idOf(entries.get(i).key()).equals(itemId)) return i;
        }
        return -1;
    }

    public static @NotNull String idOf(@NotNull ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static int room(Inventory inventory, ItemStack key) {
        int room = 0;
        for (ItemStack slot : inventory.items) {
            if (slot.isEmpty()) {
                room += key.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(slot, key)) {
                room += Math.max(0, slot.getMaxStackSize() - slot.getCount());
            }
        }
        return room;
    }

    private static int indexOf(BlissfulStorage storage, ItemStack key) {
        List<BlissfulStorage.Entry> entries = storage.entries();
        for (int i = 0; i < entries.size(); i++) {
            if (ItemStack.isSameItemSameComponents(entries.get(i).key(), key)) return i;
        }
        return -1;
    }
    //endregion

    private static Component notice(String key, Object... args) { return Component.translatable(PANEL + key, args); }

    private static @Nullable UUID parseUuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Territory.@Nullable Side parseSide(String text) {
        for (Territory.Side side : Territory.Side.values()) {
            if (side.name().equals(text)) return side;
        }
        return null;
    }
}
