package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /gzr awaken} 子命令：跳过希望蛊[Hope Gu]开窍[Awakening]仪式，直接为未开窍玩家开辟第一空窍。
 * <ul>
 *   <li>直接挂载在根命令下（使 {@code /gzr aperture} 可统一挂载已开窍可见性门禁）；调用 {@link ApertureService#awaken}
 *       后会立即调用 {@link ModCommandSupport#refreshCommands} 向目标客户端重发命令树以解锁空窍子命令。</li>
 *   <li>⚠ {@link ApertureService#awaken} 自身不拦截已开窍玩家（重复调用会继续追加新空窍），因此必须在本命令入口通过
 *       {@code AwakenedGate.AWAKENED.negate()} 拒绝已开窍目标。</li>
 * </ul>
 */

public final class CmdAwaken {

    private CmdAwaken() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return ModCommandSupport.withTargets(Commands.literal("awaken"),
                context -> ModCommandSupport.applyIf(context, AwakenedGate.AWAKENED.negate(),
                        ModCommandSupport.FAILED_AWAKENED, CmdAwaken::awaken));
    }

    private static void awaken(ServerPlayer player) {
        ApertureStorageMenu.closeFor(player);
        ApertureService.awaken(player);
        ModCommandSupport.refreshCommands(player);
    }
}
