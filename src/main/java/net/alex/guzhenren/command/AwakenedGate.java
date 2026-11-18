package net.alex.guzhenren.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Predicate;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * 依赖空窍的子命令共用的开窍[Awakening]前置校验门禁。
 * <ul>
 *   <li>分设两层校验：{@link #isSourceAwakened} 用于 Brigadier 节点的 {@code requires()} 以向未开窍的命令源隐藏命令子
 *       树，而 {@link #AWAKENED} 与 {@link #applyOnAwakened} 在执行期逐个校验目标玩家是否已开窍以保护底层空窍数
 *       据。</li>
 *   <li>⚠ 任何改变玩家开窍状态（即改变 {@link #isSourceAwakened} 返回值）的操作完成后，必须调用
 *       {@link ModCommandSupport#refreshCommands} 向客户端重发命令树，否则客户端补全仍停留在旧命令树。</li>
 * </ul>
 */

public final class AwakenedGate {

    private AwakenedGate() {}

    public static final Predicate<ServerPlayer> AWAKENED = ApertureService::isAwakened;

    public static boolean isSourceAwakened(CommandSourceStack source) {
        return !(source.getEntity() instanceof ServerPlayer player) || ApertureService.isAwakened(player);
    }

    public static int applyOnAwakened(CommandContext<CommandSourceStack> context,
                                      ModCommandSupport.PlayerOperation operation) throws CommandSyntaxException {
        return ModCommandSupport.applyIf(context, AWAKENED, ModCommandSupport.FAILED_UNAWAKENED, operation);
    }
}
