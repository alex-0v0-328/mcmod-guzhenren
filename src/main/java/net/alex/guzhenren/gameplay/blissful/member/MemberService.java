package net.alex.guzhenren.gameplay.blissful.member;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.blissful.farm.FarmWork;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地[Blissful Land]的认主、失主、白名单与驱逐。
 * <ul>
 *   <li>认主：无主时，第一个站上山巅的非旁观玩家成为主人（{@link #isOnSummit}：离山心水平
 *       {@value BlissfulLand#SUMMIT_RADIUS} 格内、脚下方块正是最高一层的荡魂山石）；白名单重置为只有主人，田区工单重
 *       挂；全服广播「【福地易主】福地名 · 玩家名」（标签金色，福地名与玩家名白色），认主者本人也只收这一条（Alex 定）。
 *   </li>
 *   <li>失主（{@link #lose}）：清主人与白名单、关两种托管、撤田区工单；库藏、疆域、标准田、每种保持数留给下一任。何时失
 *       主由 {@link BlissfulLands} 判定（死亡且不开「死亡不掉落」、角色重置）。</li>
 *   <li>白名单与免震荡、「驱逐外人」共用一份；主人可以把自己移出（面板是否允许由界面决定）。按名字只加在线玩家：原版
 *       玩家档案缓存查不到名字时会在主线程联网（离线服则凭空造一个 UUID），所以不用它找离线玩家。</li>
 *   <li>驱逐 = 走锚定旅行的 {@code exit}，回各自进福地前的返回点；主人不可被驱逐。</li>
 * </ul>
 */

public final class MemberService {

    private MemberService() {}

    public static final String MSG_CLAIMED = "guzhenren.blissful_land.claimed";
    public static final String MSG_LOST = "guzhenren.blissful_land.lost";
    public static final String MSG_EXPELLED = "guzhenren.blissful_land.expelled";

    //region Ownership [认主与失主]
    public static boolean isOnSummit(@NotNull ServerPlayer player, @NotNull RealmSite site, int summitY) {
        if (!site.isWithin(player.getX(), player.getZ(), BlissfulLand.SUMMIT_RADIUS)) return false;

        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.2D, player.getZ());
        return below.getY() == summitY && player.level().getBlockState(below).is(ModBlocks.SOULSUNDER_STONE.get());
    }

    public static boolean tryClaim(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                   @NotNull RealmSite site, int summitY, @NotNull ServerPlayer player) {
        if (data.hasOwner() || player.isSpectator() || !player.isAlive()) return false;
        if (!isOnSummit(player, site, summitY)) return false;

        claim(level, data, player);
        return true;
    }

    public static void claim(@Nullable ServerLevel level, @NotNull BlissfulLandData data,
                             @NotNull ServerPlayer player) {
        String name = player.getGameProfile().getName();
        data.setOwner(player.getUUID(), name);
        data.clearWhitelist();
        data.addWhitelist(player.getUUID(), name);
        if (level != null) FarmWork.retain(level, data);
        player.server.getPlayerList().broadcastSystemMessage(claimedMessage(data, player), false);
    }

    public static @NotNull Component claimedMessage(@NotNull BlissfulLandData data, @NotNull ServerPlayer player) {
        return Component.translatable(MSG_CLAIMED,
                Component.translatable(data.land().nameKey()).withStyle(ChatFormatting.WHITE),
                Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GOLD);
    }

    public static void lose(@NotNull MinecraftServer server, @Nullable ServerLevel level,
                            @NotNull BlissfulLandData data) {
        UUID owner = data.owner();
        if (owner == null) return;

        data.setOwner(null, "");
        data.clearWhitelist();
        data.herd().clearHosting();
        data.farm().setHosting(false);
        if (level != null) FarmWork.release(level, data);
        ServerPlayer former = server.getPlayerList().getPlayer(owner);
        if (former != null) {
            former.sendSystemMessage(Component.translatable(MSG_LOST, Component.translatable(data.land().nameKey())));
        }
    }
    //endregion

    //region Whitelist [白名单]
    public static boolean addWhitelist(@NotNull MinecraftServer server, @NotNull BlissfulLandData data,
                                       @NotNull String name) {
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (player.getGameProfile().getName().equalsIgnoreCase(name)) {
                    data.addWhitelist(player.getUUID(), player.getGameProfile().getName());
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean setWhitelisted(@NotNull MinecraftServer server, @NotNull BlissfulLandData data,
                                         @NotNull UUID player, boolean listed) {
        if (!listed) {
            data.removeWhitelist(player);
            return true;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getPlayerByUUID(player) instanceof ServerPlayer online) {
                data.addWhitelist(player, online.getGameProfile().getName());
                return true;
            }
        }
        return false;
    }

    public static boolean isExempt(@NotNull BlissfulLandData data, @NotNull ServerPlayer player) {
        return data.hasOwner() && data.isWhitelisted(player.getUUID());
    }
    //endregion

    //region Expel [驱逐]
    public static boolean expel(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @NotNull UUID target) {
        if (data.isOwner(target) || !(level.getPlayerByUUID(target) instanceof ServerPlayer player)) return false;

        send(player, data);
        return true;
    }

    public static int expelOutsiders(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        return expelOutsiders(level.players(), data);
    }

    public static int expelOutsiders(@NotNull Iterable<ServerPlayer> present, @NotNull BlissfulLandData data) {
        List<ServerPlayer> outsiders = new ArrayList<>();
        for (ServerPlayer player : present) {
            if (!data.isOwner(player.getUUID()) && !data.isWhitelisted(player.getUUID())) outsiders.add(player);
        }
        for (ServerPlayer player : outsiders) send(player, data);
        return outsiders.size();
    }

    private static void send(ServerPlayer player, BlissfulLandData data) {
        DimensionTravelService.exit(player);
        player.sendSystemMessage(Component.translatable(MSG_EXPELLED, Component.translatable(data.land().nameKey())));
    }
    //endregion
}
