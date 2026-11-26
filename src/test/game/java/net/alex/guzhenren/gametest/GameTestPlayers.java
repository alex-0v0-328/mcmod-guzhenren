package net.alex.guzhenren.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GameTest 套件共用的模拟生存玩家与场景地面。
 * <ul>
 *   <li>NeoForge {@code makeMockServerPlayerInLevel()} 登录中途被 Epic Fight 数据包检查拒绝，故自建
 *       {@link ServerPlayer}，连接吞包：确定、惰性、无客户端。</li>
 *   <li>{@link #ground} 以石铺满 9x9 场景 y=0 层；{@link #place} 把玩家钉在场景相对位置，静止无重力。</li>
 *   <li>{@link Mock#creative}、{@link Mock#spectator} 每次重读，可事后换模式；{@link Mock#inbox} 收系统消息，否则丢弃；
 *       {@link Mock#unconnected} 无连接、不入世界。</li>
 *   <li>{@link Mock#pastSpawnInvulnerability} 跳过登录 60 tick 无敌，因无网络连接在等待攻击时驱动 tick。</li>
 *   <li>{@link #withPvp} 在一次同步调用内开关服务器 PvP（GameTest 服务器默认关，玩家互伤与友伤测试须先开）。</li>
 *   <li>⚠ 套件并排运行，模拟玩家会渗入相邻结构，6 格内野生蛊会逃；目标时序断言先疑此抖动，或避开该目标。</li>
 * </ul>
 */

final class GameTestPlayers {

    /** 场景中央、站在 {@link #ground} 地面上的位置。 */
    static final Vec3 CENTER = new Vec3(4.5D, 1.0D, 4.5D);
    /** {@link #CENTER} 上方 1 格悬空的位置，用于无地面的场景。 */
    static final Vec3 STAND = new Vec3(4.5D, 2.0D, 4.5D);
    /** {@link #CENTER} 处的方块，用于生成和方块检查。 */
    static final BlockPos CENTER_POS = new BlockPos(4, 1, 4);
    /** 新生存模式玩家的生命值。 */
    static final float PLAYER_MAX_HEALTH = 20.0F;

    private GameTestPlayers() {}

    /** 以玩家身份、带管理员权限运行 {@code command}。 */
    static void command(ServerPlayer player, String command) {
        player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4),
                command);
    }

    /** {@link Mock#inbox} 中翻译键为 {@code key} 的消息。 */
    static List<Component> messages(List<Component> inbox, String key) {
        return inbox.stream().filter(message -> message.getContents() instanceof TranslatableContents content
                && content.getKey().equals(key)).toList();
    }

    /** 收件箱是否收到蛊力竭而死的消息。 */
    static boolean exhausted(List<Component> inbox) {
        return !messages(inbox, "guzhenren.item.gu.forced_dead").isEmpty();
    }

    /**
     * 仿 {@code ServerLevel.tickNonPassenger} 与连接为玩家重放 {@code ticks} 个真实服务端 tick：维度加 tick 计数并调
     * {@code tick()}（出生无敌在此倒数）；{@code doTick()} 跑 {@code Player.tick}，触发 {@code PlayerTickEvent}（心
     * 跳）。测试体在一 tick 内跑完，故在此重放。
     */
    static void serverTicks(ServerPlayer player, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            player.tickCount++;
            player.tick();
            player.doTick();
        }
    }

    /** 在 PvP 为 {@code allowed} 的服务器上同步跑 {@code body}，事后还原；GameTest 服务器默认关 PvP，玩家互伤须先开。 */
    static void withPvp(GameTestHelper helper, boolean allowed, Runnable body) {
        MinecraftServer server = helper.getLevel().getServer();
        boolean before = server.isPvpAllowed();
        server.setPvpAllowed(allowed);
        try {
            body.run();
        } finally {
            server.setPvpAllowed(before);
        }
    }

    static Mock survival(GameTestHelper helper, String profileName) {
        return new Mock(helper, profileName);
    }

    static void ground(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
    }

    static void place(ServerPlayer player, GameTestHelper helper, Vec3 relative) {
        Vec3 absolute = helper.absoluteVec(relative);
        player.moveTo(absolute.x, absolute.y, absolute.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
    }

    static final class Mock {

        private final GameTestHelper helper;
        private final String profileName;
        private BooleanSupplier creative = () -> false;
        private BooleanSupplier spectator = () -> false;
        private @Nullable List<Component> inbox;
        private boolean connect = true;
        private boolean pastSpawnInvulnerability;

        private Mock(GameTestHelper helper, String profileName) {
            this.helper = helper;
            this.profileName = profileName;
        }

        Mock creative(BooleanSupplier creative) {
            this.creative = creative;
            return this;
        }

        Mock spectator(BooleanSupplier spectator) {
            this.spectator = spectator;
            return this;
        }

        Mock inbox(@Nullable List<Component> inbox) {
            this.inbox = inbox;
            return this;
        }

        Mock unconnected() {
            this.connect = false;
            return this;
        }

        Mock pastSpawnInvulnerability() {
            this.pastSpawnInvulnerability = true;
            return this;
        }

        /** 铺好 {@link #ground}，在 {@link #CENTER} 构建站在其上的玩家。 */
        ServerPlayer onGround() {
            ground(helper);
            return at(CENTER);
        }

        /** 构建玩家并用 {@link #place} 放到场景相对位置。 */
        ServerPlayer at(Vec3 relative) {
            ServerPlayer player = build();
            place(player, helper, relative);
            return player;
        }

        ServerPlayer build() {
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                    new GameProfile(UUID.randomUUID(), profileName), false);
            ServerPlayer player = player(cookie);
            if (connect) {
                Connection connection = new Connection(PacketFlow.SERVERBOUND);
                new EmbeddedChannel(connection);
                new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
                    @Override
                    public void send(@NotNull Packet<?> packet) {}

                    @Override
                    public void send(@NotNull Packet<?> packet, @Nullable PacketSendListener listener) {}
                };
                helper.getLevel().addNewPlayer(player);
            }
            player.getAbilities().instabuild = false;
            if (pastSpawnInvulnerability) {
                for (int tick = 0; tick < 61; tick++) player.tick();
            }
            return player;
        }

        private ServerPlayer player(CommonListenerCookie cookie) {
            BooleanSupplier creative = this.creative;
            BooleanSupplier spectator = this.spectator;
            List<Component> inbox = this.inbox;
            return new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    cookie.gameProfile(), cookie.clientInformation()) {
                @Override
                public boolean isSpectator() { return spectator.getAsBoolean(); }

                @Override
                public boolean isCreative() { return creative.getAsBoolean(); }

                @Override
                public void sendSystemMessage(@NotNull Component message) { if (inbox != null) inbox.add(message); }
            };
        }
    }
}
