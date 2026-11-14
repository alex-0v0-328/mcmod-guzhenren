package net.alex.guzhenren.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /gzr awaken}：绕过通常负责开窍[Awakening]的蛊，直接开出空窍[Aperture]。
 *
 * <ul>
 *   <li>挂在根部，{@code /gzr aperture} 才能只带一个 {@code requires()} 门槛；{@link ApertureService#awaken} 后调
 *       {@link net.alex.guzhenren.command.ModCommandSupport#refreshCommands}，客户端才见新分支。</li>
 *   <li>⚠ 服务不拒绝已开窍者，会再追加一个空窍；门槛只能放在调用方，即这里。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.CmdReset
 * @since 1.0.0
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
