package net.alex.guzhenren.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Predicate;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * 需要空窍[Aperture]的命令共用的开窍[Awakening]门槛。
 *
 * <ul>
 *   <li>{@link #AWAKENED} 逐玩家拒绝目标，保护数据；{@link #isSourceAwakened} 在 {@code requires()} 隐藏子树，
 *       只管显示；{@link #applyOnAwakened} 只对已开窍目标执行。</li>
 *   <li>放在空窍一侧而非命令框架，框架因此低于所有功能。</li>
 *   <li>⚠ 改变 {@code sourceAwakened} 结果须调 {@code ModCommandSupport.refreshCommands}，否则客户端留着旧命令树。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ModCommandSupport
 * @since 1.0.0
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
